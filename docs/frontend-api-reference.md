# Silo - Frontend Build Reference

This document is written to be handed to a frontend developer (or an AI building the frontend)
with zero additional back-and-forth with the backend team. It covers every API endpoint, every
request/response shape, the auth flow, and the business rules that shape how the UI should behave
- not just what the API accepts, but *why*, so the frontend can make 
  sane decisions in the gaps.

## What Silo is

A cooperative savings & loan platform (a "thrift"/SACCO-style system) for a members' cooperative.
Members contribute money into a shared fund, can request loans against it (backed by a guarantor,
not collateral), repay over time, and the system tracks a real double-entry ledger underneath all
of it. Built as a Java/Spring Boot modular monolith (not microservices) 
- that's an internal
architecture detail that doesn't affect the API surface, mentioned only because it explains why
the domain is cleanly separated into: Member, Contribution, Loan, Repayment, Accounting/Ledger,
Reporting, Notification, Payment Gateway (Paystack), and Auth.

## Base URL, format, auth transport

- No context path - endpoints are mounted at the root, e.g. 
  `http://localhost:8080/api/members`.
- All request/response bodies are JSON.
- Auth: `Authorization: Bearer <accessToken>` header on every request except the ones listed as
  Public below.
- Access tokens expire in **15 minutes**. Refresh tokens last **7 days** and rotate on every use
  (see Auth flow below).

## Response envelope

Every endpoint **except** the Paystack webhook and the SSE dashboard stream returns:

```jsonc
{
  "success": true,
  "message": "Contribution recorded successfully",  // present on write operations, null on most reads
  "data": { /* the actual payload, shape documented per-endpoint below */ },
  "timestamp": "2026-08-15T22:04:07.123Z"
}
```

On error, `success` is irrelevant - the body shape changes entirely to:

```jsonc
{
  "timestamp": "2026-08-15T22:04:07.123Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Repayment amount exceeds the loan's outstanding balance",
  "path": "/api/repayments",
  "errors": []   // populated with { "field": "...", "message": "..." } entries only on validation failures (400s)
}
```

**Status code map** - build your HTTP client's error handling around 
this, it's exhaustive:

| Status | When                                                                                                                                           | Body                                                             |
|---|------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| 400 | Request body failed `@Valid` (missing/malformed fields), bad query param type, malformed JSON                                                  | `errors[]` populated for field-level validation; empty otherwise |
| 401 | No/invalid/expired JWT, or invalid Paystack webhook signature                                                                                  | -                                                                |
| 403 | JWT valid but caller lacks the role/ownership the endpoint requires                                                                            | -                                                                |
| 404 | Referenced id doesn't exist (member, loan, loan request, liability, etc.)                                                                      | -                                                                |
| 409 | Duplicate resource (e.g. registering credentials twice for one member)                                                                         | -                                                                |
| 422 | Business rule violation - the request is well-formed but violates a domain rule (insufficient credibility, loan not ACTIVE, overpayment, etc.) | -                                                                |
| 500 | Unhandled server error                                                                                                                         | generic message, don't surface raw internals to the user         |

Every 4xx `message` is written to be **directly displayable to the 
user** - the backend already
phrases these as human-readable sentences (e.g. "Borrower already has an active loan and cannot
be approved for another"). Don't re-word them, just show them.

## Roles

Two roles: `MEMBER` and `OFFICER`. Every member has exactly one role, returned as a plain string
(`"MEMBER"` or `"OFFICER"`) in the login response - it's not a list, no 
multi-role support. An
officer is cooperative staff; they approve/reject loans, record manual contributions, and manage
member status/KYC. Everything else a member does for themselves (request a loan, guarantee
someone, repay).

**An officer IS a member, not a separate account type.** There's no separate "Officer" table —
`Credential` (the login row) has a `memberId` pointing to a normal `Member` row plus a `role`
field. An officer has their own member profile and can do everything a plain member can: submit a
loan request, invite/accept/decline as a guarantor, make repayments. The only things gated to
`OFFICER` specifically are: approving/rejecting loan requests, recording manual contributions, and
changing another member's status/KYC. **There is currently no API endpoint to create an
officer** — every new credential is hardcoded to `MEMBER` at registration; promoting someone to
officer today happens by editing the database directly. If you need an "officer management" screen,
that backend endpoint doesn't exist yet — flag it back.

**An officer cannot act on themselves for any OFFICER-gated action.** Approving/rejecting their own
loan request, recording their own contribution, or changing their own status/KYC are all rejected
with a 422 (`"An officer cannot approve their own loan request"`, etc.) even though they hold the
OFFICER role. A different officer has to do it. This is enforced server-side, but build the UI to
match: don't show an officer their own pending loan request in an "approve/reject" queue, don't let
them pick themselves in a "record contribution for member X" search, and don't show themselves in
a "change member status" admin list.

Two authorization patterns show up across endpoints:
- **Role-gated**: `@PreAuthorize("hasRole('OFFICER')")` - only officers,
  full stop.
- **Self-or-officer**: officers can act on any member, a regular member only on themselves,
  enforced either by comparing the JWT's subject (member id) against a path variable, or
  implicitly by services using the JWT's member id rather than trusting a body field (e.g.
  `payerMemberId` on a repayment always comes from the token, never 
  from the request body - the
  frontend does not send who is paying, it's implicit from who's logged in).

## Auth flow

Registration is two steps: a member profile first, then credentials.

1. **`POST /api/members`** (public) - creates a member profile with 
   `kycStatus: PENDING`. No
   password yet, no login possible. Body: `{ fullName, email, phoneNumber }` (all required,
   `email` must be valid format).
2. **`POST /api/auth/register`** (public) - sets a password for an 
   *existing* member id. Body:
   `{ memberId, password }` (password min 8 chars). This is a separate step because in practice
   an officer/admin process creates the member record (e.g. after in-person KYC), and the member
   sets their own password afterward - plan the UI as two distinct 
   screens/steps, not one combined
   signup form, unless you want to chain them client-side.
3. **`POST /api/auth/login`** (public) - body `{ email, password }` → 
   returns
   `{ accessToken, refreshToken, memberId, role }`. Store both tokens; use `role` to decide which
   UI (member vs officer) to render.
4. **`POST /api/auth/refresh`** (public) - body `{ refreshToken }` → 
   returns a **new**
   `{ accessToken, refreshToken, memberId, role }` pair. The old refresh token is revoked the
   moment you use it (rotation) - always overwrite your stored refresh 
   token with the new one, or
   the *next* refresh attempt will fail. Call this proactively before the 15-minute access token
   expires, or reactively on a 401, whichever pattern you prefer - 
   there's no separate
   "is this token about to expire" endpoint.

There is no logout endpoint - logging out is purely a client-side token 
discard. There is no
"forgot password" flow yet - out of scope for this version.

## Business domains and endpoint reference

### Members - `/api/members`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/members` | Public | Register a member profile |
| GET | `/api/members/{id}` | Any authenticated | View a member's profile |
| PUT | `/api/members/{id}` | Any authenticated | Update contact info + KYC doc refs (see note) |
| PATCH | `/api/members/{id}/status` | OFFICER | Change member status |
| PATCH | `/api/members/{id}/kyc` | OFFICER | Change KYC status |

> **Note on PUT `/api/members/{id}`**: the backend does not currently restrict this to "self or
> officer" - any authenticated member can technically edit any other 
> member's profile via this
> endpoint. Build the UI as if it's self-only (don't expose an "edit any member" screen to regular
> members), but be aware this is a real gap if you're pen-testing or if this matters for your
> deadline - flag it back if you want it tightened.

> **Note on PATCH `/status` and `/kyc`**: an officer cannot use these on their own member id, even
> though they hold the OFFICER role - both 422 with a message like "An officer cannot change their
> own status." Exclude the logged-in officer from any member list/search you build for these admin
> screens.

**MemberRequest** (POST body): `{ fullName: string, email: string, 
phoneNumber: string }` - all
required, `email` validated as an email format.

**MemberProfileUpdateRequest** (PUT body): `{ fullName: string, phoneNumber: string, idType?: string, idNumber?: string, idDocumentRef?: string }`
- `fullName`/`phoneNumber` required, the three ID fields are free-form 
  strings with no format
validation (put your own client-side rules on them if you want, e.g. idType as a select of
"National ID" / "Passport" / "Driver's License").

**MemberStatusUpdateRequest**: `{ status: MemberStatus }`. **MemberKycUpdateRequest**:
`{ kycStatus: KYCStatus }`.

**MemberResponse** (all GET/PUT/PATCH here return this):
```jsonc
{
  "id": "uuid", "fullName": "string", "email": "string", "phoneNumber": "string",
  "kycStatus": "PENDING | VERIFIED | REJECTED",
  "idType": "string | null", "idNumber": "string | null", "idDocumentRef": "string | null",
  "status": "ACTIVE | INACTIVE | SUSPENDED",
  "joinedDate": "2026-08-15T22:04:07"
}
```

**Business rule to build around:** a member must be `status: ACTIVE` **and** `kycStatus: VERIFIED`
before they can make a contribution or request a loan. If they're not, those endpoints return 422
with a message like "Member is not active and KYC verified" - surface 
that clearly rather than
letting them fumble through a form that will always fail. Consider disabling the "Request a Loan" /
"Contribute" actions in the UI entirely until the member's own profile shows both conditions met.

### Auth - `/api/auth`

Covered above. Endpoints: `POST /register`, `POST /login`, `POST /refresh`, all public.

### Contributions - `/api/contributions`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/contributions` | OFFICER | Record a manual (cash/bank) contribution for a member |
| GET | `/api/contributions/member/{memberId}` | Any authenticated | Contribution history |
| GET | `/api/contributions/member/{memberId}/summary` | Any authenticated | Running total + count |

**ContributionRequest**: `{ memberId: uuid, amount: decimal (min 0.01), reference: string }`.
`recordedBy` is not sent - it's the logged-in officer, taken from the JWT. If `memberId` equals the
logged-in officer's own id, this 422s ("An officer cannot record their own contribution") - exclude
the logged-in officer from whatever member search/picker you build for this form.

**ContributionResponse**: `{ id, memberId, amount, reference, source: "MANUAL"|"PAYSTACK", recordedBy: uuid|null, contributionDate }`.
`recordedBy` is `null` for Paystack-sourced contributions (nobody "recorded" those, they came in
via webhook).

**ContributionSummaryResponse**: `{ memberId, totalAmount, contributionCount }`.

There is a second, automatic way contributions get created: a member pays via **Paystack**
(handled entirely server-side via webhook, see the Payment Gateway 
section) - those show up in the
same history/summary endpoints with `source: "PAYSTACK"`. If you're building a "contribute" flow
for a member (not an officer), that's a Paystack checkout integration on your end (initiate a
Paystack transaction client-side/via your own backend, Paystack calls Silo's webhook on success),
**not** a call to `POST /api/contributions` - that endpoint is 
officer-only for manual cash
entries.

### Loan requests - `/api/loan-requests`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/loan-requests` | Any authenticated | Submit a loan request (self) |
| POST | `/api/loan-requests/{id}/guarantors` | Requesting member only | Invite a guarantor |
| POST | `/api/loan-requests/{id}/approve` | OFFICER | Approve → creates the Loan |
| POST | `/api/loan-requests/{id}/reject` | OFFICER | Reject the request |

**LoanRequestSubmitRequest**: `{ amountRequested: decimal (min 0.01), purpose: string }`.
`memberId` comes from the JWT.

**AddGuarantorRequest**: `{ guarantorMemberId: uuid }`.

**LoanApprovalRequest**: `{ interestRate: decimal (min 0.0), 
durationMonths: integer (min 1) }` -
these are officer-set at approval time, not chosen by the borrower at request time. Your loan
request form should **not** ask for interest rate or duration; your officer approval screen should.

**LoanRequestResponse**: `{ id, memberId, amountRequested, purpose, status: "PENDING"|"APPROVED"|"REJECTED", submittedAt }`.

**LoanGuarantorResponse** (from the invite endpoint): `{ id, loanRequestId, memberId, status: "PENDING"|"ACCEPTED"|"DECLINED", invitedAt }`.

**LoanResponse** (from approve): `{ id, loanRequestId, memberId, principalAmount, interestRate, durationMonths, disbursedDate, status: "ACTIVE"|"CLOSED"|"DEFAULTED", outstandingBalance }`.

**The full loan lifecycle, so the UI can model state correctly:**

1. Member submits a `LoanRequest` (`PENDING`). Requires the member to be `ACTIVE` + `VERIFIED`.
2. Member invites one or more guarantors. Each invite is its own `LoanGuarantor` row, starting
   `PENDING`.
3. Each invited guarantor independently accepts or declines (see Guarantors section below).
4. Once at least one guarantor has `ACCEPTED`, an officer can approve the request. Approval also
   checks: the borrower has no active default, and **every accepted guarantor** individually meets
   a minimum credibility score (see Credit Profiles section) - if any 
   accepted guarantor fails
   that check, approval is rejected with a 422 naming that guarantor.
5. Approval also fails if the borrower **already has another `ACTIVE` 
   loan** - the system only
   allows one active loan per member at a time. Surface this clearly; don't let a member queue up
   a second loan request while one is still running without warning them it'll be blocked at
   approval time.
5a. Approval/rejection also fails with a 422 if the logged-in officer *is* the borrower on that
   request ("An officer cannot approve/reject their own loan request") - since an officer is also
   a member and can submit loan requests like anyone else. Filter an officer's own pending
   requests out of whatever "requests awaiting approval" queue you build for them, so they aren't
   shown a doomed approve/reject action on their own request.
6. On approval, a `Loan` is created (`ACTIVE`), an installment schedule is generated, and the loan
   request flips to `APPROVED`. Rejection just flips the request to 
   `REJECTED` - no Loan is ever
   created.
7. From here the Loan is either fully repaid over time and becomes `CLOSED`, or the borrower falls
   behind (3 consecutive late installments, checked by a scheduled backend job, not client-driven)
   and it becomes `DEFAULTED`. A `DEFAULTED` loan can *still* later become `CLOSED` if the debt
   gets fully recovered through guarantor liability payments (see 
   Repayments section) - don't treat
   `DEFAULTED` as a permanent dead-end state in your UI; a loan can move `DEFAULTED → CLOSED`.

### Loans (read) - `/api/loans`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/loans/{id}` | Any authenticated | Full loan detail + installment schedule |

**LoanDetailResponse**: `{ id, loanRequestId, memberId, principalAmount, interestRate, durationMonths, disbursedDate, status, outstandingBalance, installments: LoanInstallmentResponse[] }`.

**LoanInstallmentResponse**: `{ id, installmentNumber, dueDate, expectedAmount, status: "PENDING"|"PAID"|"LATE"|"DEFAULTED", paidDate }`.

This is your loan detail / repayment schedule screen - render the 
installment list as a table,
one row per `installmentNumber`, with `status` driving a badge color.

### Guarantors - `/api/guarantors` and `/api/members` (discovery)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/guarantors/{id}/accept` | Invited guarantor only | Accept a guarantee invite |
| POST | `/api/guarantors/{id}/decline` | Invited guarantor only | Decline |
| GET | `/api/members/available-guarantors` | Any authenticated | Discover members you could ask (excludes yourself) |
| GET | `/api/members/{memberId}/guarantor-invites` | Any authenticated | View pending invites sent to a member |

Note the discovery endpoints live under `/api/members`, not 
`/api/guarantors` - a backend naming
quirk, not a typo on your end.

**AvailableGuarantorResponse**: `{ memberId, email, credibilityScore: 
int }` - use this to build
the "pick a guarantor" search/select UI; showing the credibility score up front lets a borrower
avoid inviting someone who'll fail the minimum-credibility check at approval time.

**GuarantorInviteResponse**: `{ id, loanRequestId, borrowerMemberId, 
amountRequested, purpose, borrowerRiskTier: "LOW"|"MEDIUM"|"HIGH", invitedAt }` - this is what a prospective guarantor sees
before accepting/declining: who's asking, how much, why, and the borrower's own risk tier so they
can make an informed call. Surface `borrowerRiskTier` prominently - 
it's the single most useful
signal for "should I guarantee this person."

### Credit profiles - `/api/credit-profiles`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/credit-profiles/borrower-risk/{memberId}` | Self or OFFICER | Borrower's risk score |
| GET | `/api/credit-profiles/guarantor-credibility/{memberId}` | Self or OFFICER | Guarantor's credibility score |

**BorrowerRiskProfileResponse**: `{ memberId, totalLoans: int, defaultedLoans: int, lateLoanPayments: int, currentRiskTier: "LOW"|"MEDIUM"|"HIGH" }`.

**GuarantorCredibilityProfileResponse**: `{ memberId, timesGuaranteed: int, loansWentBad: int, successfulGuarantees: int, credibilityScore: int (0-100) }`.

These are read-only, computed server-side. Credibility starts at 100, drops 30 points every time a
loan a member guaranteed defaults, and gains 5 points (capped at 100) every time a loan they
guaranteed closes successfully without ever defaulting. A score below 50 blocks that person from
being an eligible guarantor on new loan approvals. Good candidates for a "your standing" profile
page/widget - a member should be able to see their own score and 
roughly why it is what it is
(you have the raw counts to build a simple explanation like "2 of your guarantees went bad").

### Repayments - `/api/repayments`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/repayments` | Loan's own borrower | Repay against your own active loan |
| POST | `/api/repayments/liability` | Assigned guarantor only | Pay off a guarantor liability |
| GET | `/api/repayments/loan/{loanId}` | Any authenticated | Repayment history for a loan |

**RepaymentRequest**: `{ loanId: uuid, amount: decimal (min 0.01), reference: string }`.
**LiabilityRepaymentRequest**: `{ liabilityId: uuid, amount: decimal (min 0.01), reference: string }`.
`payerMemberId` on both is implicit from the JWT - never send it.

**RepaymentResponse**: `{ id, loanId, payerMemberId, liabilityId: uuid|null, amount, reference, paymentDate }`.
`liabilityId` is `null` for ordinary borrower repayments, populated for liability repayments.

**Rules that directly affect form UX:**

- **Overpayment is rejected outright**, not capped or silently absorbed. If a borrower tries to
  pay more than the loan's current `outstandingBalance` (or a guarantor more than the liability's
  remaining balance), the request 422s. **Pre-fill or cap the amount input at the current
  outstanding balance** (you already have it from `GET /api/loans/{id}`) to avoid a frustrating
  round-trip failure - don't just let them type any number.
- A repayment against a loan that isn't `ACTIVE` is rejected (a `CLOSED` or not-yet-`DEFAULTED`
  loan can't take ordinary repayments - once defaulted, only liability 
  repayments from guarantors
  continue reducing the balance).
- Each repayment is allocated against the **oldest unpaid installment first**; when a repayment
  brings `outstandingBalance` to exactly zero, the loan closes automatically (`status → CLOSED`) in
  the same request - no separate "close my loan" action exists or is 
  needed. Reflect this by
  re-fetching the loan detail after a repayment and updating status/installments in the UI rather
  than assuming it stays `ACTIVE`.
- A guarantor liability repayment reduces the *loan's* balance too (the guarantor is paying down
  the borrower's debt on their behalf) and can, on its own, close a `DEFAULTED` loan the same way.

### Ledger - `/api/ledger`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/ledger/accounts/{id}/balance` | Any authenticated | One account's current balance |
| GET | `/api/ledger/trial-balance` | Any authenticated | Full trial balance across all accounts |

**LedgerAccountBalanceResponse**: `{ accountId, code: string, name: string, balance: decimal }`.

**TrialBalanceResponse**: `{ accounts: TrialBalanceEntryResponse[], totalDebits: decimal, totalCredits: decimal }`.
**TrialBalanceEntryResponse**: `{ accountId, code, name, totalDebits: decimal, totalCredits: decimal }`.

This is bookkeeping-facing, not member-facing - you'd wire this up for 
an officer/admin
"accounting" screen, not a regular member's dashboard. The chart of accounts is fixed (seeded by
the backend, not creatable via API): `1000` Cash and Bank, `1100` Loans Receivable, `1200`
Guarantor Receivable, `1300` Penalty Receivable, `2000` Member Contributions Payable, `3000`
Cooperative Fund Equity, `4000` Interest Income, `4100` Penalty Income. You'll need to hardcode
these codes/names on the frontend if you want a labeled chart-of-accounts view, since there's no
"list all accounts" endpoint - only lookup-by-id and the trial balance 
(which does include every
account by way of listing all of them with their totals).

### Reporting - `/api/reports`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/reports/dashboard` | Any authenticated | Cooperative-wide summary stats |
| GET | `/api/reports/members/{id}/summary` | Any authenticated | One member's summary |
| GET | `/api/reports/top-contributors` | Any authenticated | Leaderboard |
| GET | `/api/reports/stream` | Any authenticated | Live-updating dashboard via SSE |

**DashboardResponse**: `{ activeLoans: long, totalContributions: decimal, outstandingBalance: decimal, defaultRate: decimal }`.

**MemberReportSummaryResponse**: `{ memberId, totalContributions, activeLoans: int, totalRepayments }`.

**TopContributorResponse**: `{ memberId, totalContributions }`.

`GET /api/reports/top-contributors?limit=10` - `limit` is an optional 
query param, defaults to 10,
no upper bound enforced server-side, so clamp it client-side if you don't want someone requesting
`limit=100000`.

`GET /api/reports/stream` is a **Server-Sent Events** endpoint (`Content-Type: text/event-stream`),
**not** wrapped in the standard `ApiResponse` envelope - connect with 
`EventSource` (browser-native)
or an SSE-capable HTTP client, not a normal fetch/axios call. It pushes live dashboard updates;
use it for a real-time admin dashboard instead of polling `/dashboard` on a timer. If your framework
doesn't have great native SSE support, polling `/api/reports/dashboard` every N seconds is a
perfectly reasonable fallback - this isn't a hard requirement, just an 
available upgrade.

### Notifications - `/api/notifications`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/notifications/member/{memberId}` | Any authenticated | A member's notification log |

**NotificationLogResponse**: `{ id, memberId, eventType: string, channel: "EMAIL", status: "SENT"|"FAILED", sentAt }`.

Notifications are currently **email-only** (`channel` will always be `"EMAIL"` today, but the enum
exists for future channels - don't hardcode a UI that assumes only 
email will ever appear). This
endpoint is a read-only log, not an inbox - there's no "mark as read," 
no in-app notification
delivery, no push notifications. If you want a notification bell/inbox UX, you're building it on
top of this log endpoint with your own read/unread tracking (not backend-supported yet).

### Payment gateway (Paystack) - `/api/webhooks/paystack`

This endpoint is called by **Paystack's servers**, not your frontend. It's documented here only so
you understand where Paystack-sourced contributions come from: your frontend integrates with
Paystack's own client-side SDK/checkout to collect payment, Paystack calls this backend webhook on
success, and the backend converts that into a `Contribution` with `source: "PAYSTACK"` that then
shows up in the normal contribution history endpoints. You don't call this endpoint directly, and
there's currently no backend endpoint to *initiate* a Paystack 
transaction - that flow needs to be
either built entirely client-side against Paystack's own APIs, or added as a new backend endpoint
if you want the initiation to go through Silo too. Flag this back if your Paystack integration plan
needs an initiation endpoint - it doesn't exist yet.

## Enum reference

| Enum | Values | Used on                                                                                                          |
|---|---|------------------------------------------------------------------------------------------------------------------|
| `MemberStatus` | `ACTIVE`, `INACTIVE`, `SUSPENDED` | Member                                                                                                           |
| `KYCStatus` | `PENDING`, `VERIFIED`, `REJECTED` | Member                                                                                                           |
| `ContributionSource` | `MANUAL`, `PAYSTACK` | Contribution                                                                                                     |
| `LoanStatus` | `ACTIVE`, `CLOSED`, `DEFAULTED` | Loan                                                                                                             |
| `LoanRequestStatus` | `PENDING`, `APPROVED`, `REJECTED` | LoanRequest                                                                                                      |
| `GuarantorStatus` | `PENDING`, `ACCEPTED`, `DECLINED` | LoanGuarantor                                                                                                    |
| `InstallmentStatus` | `PENDING`, `PAID`, `LATE`, `DEFAULTED` | LoanInstallment                                                                                                  |
| `LiabilityStatus` | `PENDING`, `PAID`, `WAIVED` | GuarantorLiability (not directly exposed on a DTO today, but drives repayment behavior - see Repayments section) |
| `RiskTier` | `LOW`, `MEDIUM`, `HIGH` | BorrowerRiskProfile, guarantor invite view                                                                       |
| `EntryType` | `DEBIT`, `CREDIT` | Ledger entries (internal)                                                                                        |
| `NotificationChannel` | `EMAIL` (only value today) | NotificationLog                                                                                                  |
| `NotificationStatus` | `SENT`, `FAILED` | NotificationLog                                                                                                  |

## Explicitly out of scope (don't build UI expecting these)

- No password reset / "forgot password" flow.
- No file upload for KYC documents - `idDocumentRef` is just a string 
  field (a reference/URL you'd
  populate from wherever the actual file lives; Silo doesn't host document storage).
- No in-app read/unread notification state, no push notifications.
- No endpoint to initiate a Paystack transaction - only the webhook 
  receiving the result exists.
- No partial/installment-level guarantor liability visibility beyond 
  the aggregate profile -
  guarantors don't see a schedule for their liability the way borrowers see a loan's installment
  schedule, just the liability's total/remaining via the repayment flow.
- No admin "list all accounts" or "list all members" endpoint - member 
  lookup is always by id
  (you'd need a search/list endpoint added if your officer UI needs a member directory; ask if you
  need this, it's a quick addition).
- No refund or reversal of a contribution/repayment once recorded.

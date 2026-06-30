# Silo: a Cooperative Savings & Loan Platform
---

A backend platform that lets cooperative members contribute to a shared
fund and apply for loans backed by guarantors - with every financial
movement recorded through real double-entry accounting. Built as a
Java/Spring Boot modular monolith with event-driven communication between
modules.

This is a bootcamp capstone project (Project 2: Cooperative Savings & Loan
Platform), built to be a portfolio-grade, full-stack implementation rather
than a minimal MVP.

## What it does

Members register, get KYC-verified, and contribute money to a common
cooperative fund. They can apply for loans, which require at least one
guarantor (also a member) and officer approval. Approved loans get a real
repayment schedule; missed installments accrue toward default, which
splits a financial liability across the loan's guarantors and assesses
a penalty against the borrower. Every one of these events - a
contribution, a disbursement, a repayment, a default - is posted as a
balanced journal entry in a double-entry ledger, which is the single
source of truth for every balance in the system. A read-only reporting
layer listens to the same events and pushes a live dashboard over
Server-Sent Events.

## Architecture

**Style:** Modular monolith - one Spring Boot application, one PostgreSQL
database, each business capability isolated into its own package. Modules
talk to each other either through direct calls (synchronous validation,
e.g. "is this member active?") or through Spring Application Events (
decoupled side effects, e.g. "post a journal entry after a contribution is
recorded").

Three design principles run through the whole system:

- **Single source of truth for money.** Only the Accounting (Ledger) module
  writes financial balances. Contribution, Loan, and Repayment modules each
  save their own record and publish a domain event; Accounting listens and
  posts the corresponding double-entry journal entry. No other module ever
  updates a balance directly.
- **Reporting is read-only (CQRS-lite).** The Reporting module owns no
  business logic and never writes operational data. It listens to the same
  domain events as Accounting, maintains its own denormalised projection
  tables, and pushes updates live over SSE.
- **Guarantorship and risk are loan-scoped history, not standalone modules.
  ** A guarantor relationship is born when a loan request is submitted and
  ends when the loan closes, so it lives inside the Loan module - along
  with the borrower risk and guarantor credibility profiles derived from
  event history across every past loan.

Reliability is structural rather than bolted on: every
financially-meaningful event is written to a transactional outbox in the
same DB transaction as its originating write, then dispatched by a poller,
so a crash between commit and delivery can't silently lose a ledger
posting. Outbound calls (Paystack, email) are wrapped in retry with
exponential backoff and jitter.

Full diagrams: see `/docs` - core domain architecture, platform &
reliability architecture, and the entity relationship diagram.

## Modules

| Module              | Owns                                                                                                            |
|---------------------|-----------------------------------------------------------------------------------------------------------------|
| Member              | Identity, profile, KYC verification, membership lifecycle                                                       |
| Contribution        | Recording contributions (manual or Paystack), contribution history                                              |
| Loan                | Loan requests, guarantors, risk/credibility scoring, repayment schedule, default detection, guarantor liability |
| Repayment           | Borrower repayments and guarantor liability repayments                                                          |
| Accounting (Ledger) | Chart of accounts, double-entry journal entries, trial balance - sole writer of every balance                   |
| Reporting           | Read-only dashboards and projections, live SSE updates                                                          |
| Notification        | Email notifications on key events                                                                               |
| Payment Gateway     | Paystack webhook intake, signature verification, reconciliation                                                 |
| Auth                | Authentication and role-based authorization (Spring Security)                                                   |

See `docs/silo-technical-documentation.docx` for full module specs
(entities, business rules, events published/consumed, endpoints).

## Tech stack

- **Backend:** Java 21, Spring Boot 4.0.1
- **Database:** PostgreSQL
- **Data access:** Spring Data JPA
- **Security:** Spring Security
- **Build tool:** Maven
- **API docs:** OpenAPI / Swagger
- **Payments:** Paystack (webhooks)
- **Live updates:** Server-Sent Events
- **Architecture:** Modular Monolith + Event Driven (Spring Application
  Events, transactional outbox)

## Getting started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for PostgreSQL locally)

### Setup

```bash
git clone https://github.com/oluwatimilehinawoniyi/Silo.git
cd Silo

# start PostgreSQL
docker compose up -d

# run the app
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. API docs are at
`http://localhost:8080/swagger-ui.html`.

### Environment variables

| Variable                     | Description                | Default (local)                         |
|------------------------------|----------------------------|-----------------------------------------|
| `SPRING_DATASOURCE_URL`      | Postgres connection string | `jdbc:postgresql://localhost:5432/coop` |
| `SPRING_DATASOURCE_USERNAME` | DB username                | `coop`                                  |
| `SPRING_DATASOURCE_PASSWORD` | DB password                | `coop`                                  |

[//]: # (| `PAYSTACK_SECRET_KEY`        | Paystack secret key for webhook signature verification | -                                       |)
| `JWT_SECRET`                 | Secret used to sign auth tokens | - |

[//]: # (| `MAIL_*`                     | SMTP config for the Notification module                | -                                       |)

[//]: # (A `.env.example` is included - copy it to `.env` and fill in real values)

[//]: # (before running with Paystack/email enabled.)

## Running tests

```bash

mvn clean verify

```

CI runs this on every pull request against a Postgres service container -
see `.github/workflows/ci.yml`.

## Project structure

```
src/main/java/com/silo/
  member/
  contribution/
  loan/
  repayment/
  accounting/
  reporting/
  notification/
  paymentgateway/
  common/        # shared infra: exception handling, outbox, event base classes, scheduling
```

Each module package is self-contained: its own entities, repository,
service, controller, and event listeners. Cross-module communication
happens through the `common` event infrastructure, not direct package
imports of another module's internals.

## Contributing

This project is built by a team with module ownership split per

`docs/technical-documentation.docx` (Section 4: Task Allocation). Pull

1. Branch off `main`: `feature/T<task-id>-short-description` (e.g.
   `feature/T17-loan-request-api`)

2. Open a PR referencing the task ID - see the PR template for what to
   include

3. CI must pass and the module owner must approve before merging

## Documentation

- `docs/silo-technical-documentation.docx` - full module specs and task
  breakdown

- `docs/silo-architecture-core-domain.png` - Member, Contribution, Loan,
  Repayment, Accounting, Reporting

- `docs/silo-architecture-platform.png` - Notification, Payment Gateway,
  outbox, retry, scheduled jobs

[//]: # (- `docs/er-diagram.png` - entity relationship diagram)

## License

TBD by the team.
# Contributing

This is a team project, so the rules below exist to keep people
from stepping on each other, not to slow anyone down. If something here
gets in the way, raise it - these can change.

## Before you start a task

Find your task in `docs/silo-technical-documentation.docx` (Section 4: Task
Allocation). Each task has an ID (T1, T17, T45...) - you'll reference this
in your branch name and PR.

## Branching

Branch off the latest `main`:

```bash
git checkout main
git pull
git checkout -b feature/T17-loan-request-api
```

Naming: `feature/T<id>-short-description`. For a fix that isn't a listed
task, use `fix/short-description`.

Don't work directly on `main` - it's protected and you can't push to it
directly anyway.

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/) style:

```
feat: add loan request submission endpoint
fix: correct outstanding balance calc on partial repayment
test: add integration test for contribution → ledger flow
refactor: extract guarantor validation into its own method
docs: update README setup steps
chore: bump spring boot version
```

Keep commits reasonably small and focused - a commit per logical change is
easier to review than one giant commit at the end. Don't commit `.env`,
build artifacts, or IDE config (`.gitignore` already covers the common
ones; add to it if you hit something new).

## Before opening a PR

```bash
mvn clean verify
```

Make sure this passes locally. CI will run it again on your PR, but
catching it yourself first saves a review cycle.

If your task touches a module's public behavior (new/changed endpoint, new
event published or consumed), update the relevant section in the technical
doc or leave a note in the PR - the doc is the thing the rest of the team
reads to understand what exists.

## Opening the PR

- Target `main`.
- Use the PR template - fill in the task ID, what changed, and how you
  tested it.
- Keep the PR scoped to one task where possible. If a task naturally splits
  into multiple PRs (e.g. entity+migration first, endpoint second), that's
  fine - say so in the description.

## Review

- `CODEOWNERS` auto-requests the right reviewer(s) based on which module
  your PR touches - you don't need to manually pick someone.
- At least 1 approval is required before merge.
- CI must be green before merge.
- If review comments come back, push fixes to the same branch - don't open
  a new PR.

Be direct but kind in reviews. "This breaks the single-writer rule -
Contribution shouldn't update the fund balance directly, only Accounting
should" is a useful comment. Nitpicking variable names on someone's first
PR is not a good use of anyone's time.

## Merging

Squash and merge is the default, so `main` history stays one commit per
PR/task. The PR title becomes the commit message, so make sure it's a clean
Conventional Commit line (e.g.
`feat: add loan request submission endpoint (T17)`).

After merge, delete your branch.

## A few things that matter more than usual on this project

- **Never write a balance outside the Accounting module.** If you're
  tempted to set `outstandingBalance` or update a fund total directly from
  Contribution, Loan, or Repayment, stop - publish the event and let
  Accounting's listener do it. This is the one rule the whole architecture
  is built around.
- **Reporting endpoints are GET-only.** If a PR to the Reporting module
  adds a write endpoint, that's a sign the change belongs somewhere else.
- **Don't bypass the event bus to call another module's service directly
  for something that should be an event** (e.g. don't have Repayment call
  into Accounting's code to post a journal entry - publish
  `RepaymentMadeEvent` and let Accounting listen for it). Direct calls are
  fine for synchronous reads (e.g. Loan checking Member is active), not for
  triggering side effects in another module.

## Questions

If a task is unclear or blocked by someone else's incomplete work, say so
early rather than guessing - several tasks (T5 Domain Event infrastructure,
T7 Spring Security) are foundational and other tasks
depend on them landing first.
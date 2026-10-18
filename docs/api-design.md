# OpenShelves — API Design

Derived from `docs/poc/demo-site`. The source contract is `lib/api/*`, `types/index.ts`, `lib/store.tsx` (the actions the UI takes) and `lib/catalog.ts`.

## Conventions

- **Base path:** `/api/v1`. Auth is a JWT bearer token.
- **Who sees what:** patron data lives under `/me/...` and librarian tools under `/admin/...`. Catalog reads are shared.
- **Lists:** paged results come back as `{ items, page, size, total }`. Dates are ISO strings and enums are lowercase, as in `types/index.ts`.
- **No display fields from the backend.** The current types mix data with UI text. These should be built by the frontend from enums and raw values instead:
  - `initial`, `availabilityLabel`, `catalogCard`, `coverTone`, `arrivalBadge`
  - `ageLabel`, `group`, `icon`, `iconTone`, notification `actions`/`card`/`meta`
  - `updatedLabel`, `membersLabel`, `statsLabel`, `spines`, `ledgerTitle`/`compactTitle`
  - `countLabel`, `accrualLabel`, `pickers`, `collectionTone`, `accessTone`, `footerLink`
- **Computed by the server, not stored:** `availabilityStatus` and `holdsInQueue` (from copies and holds), `FineSummary`, `ReadingSummary`, and list item counts.
- **Errors:** 409 with a machine-readable `code` for business-rule failures (e.g. `MAX_RENEWALS`, `HOLD_QUEUED`, `DUPLICATE_ISBN`).

## Auth (sign-in screen)

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/login` | `{identifier, password, rememberMe, kiosk}`. The identifier is an email or a library card number. Returns an access token and sets a refresh cookie. Kiosk mode gets no refresh cookie. |
| POST | `/auth/refresh` | |
| POST | `/auth/logout` | |
| GET | `/auth/institutions` | Fills the SSO dropdown. **Defer:** SAML/OIDC is phase 2 (`CredentialType` is ready for it). |

## Me

| Method | Path | Notes |
|---|---|---|
| GET | `/me` | `User` plus `standing` and `borrowLimit` |
| GET | `/branches` | Pickup locations |

## Catalog

| Method | Path | Notes |
|---|---|---|
| GET | `/books` | Takes `q`, `access[]`, `type[]`, `subject[]`, `lang[]`, `yearFrom`, `yearTo`, `sort=relevance\|newest\|oldest\|title\|author`, `page`, `size`. Returns the page **plus facet counts**. The subject facets are client-side regexes today and must move to the server. `total` replaces `catalogStats.catalogMatches`. |
| GET | `/books/{id}` | Full record: contributors, table of contents, copies, availability. Includes `myState: {holdId, loanId, saved, listIds}` so detail and card buttons need no extra calls. |
| GET | `/books/{id}/related?limit=` | |
| GET | `/books/new-arrivals` | Takes `discipline`, `since`, page params. Sorted by accession date (`copies.acquired_on`). |
| GET | `/books/recommended` | **Defer.** The page has no design and stays inert. |

Citations stay client-side: `lib/citations.ts` already formats them, so no endpoint is needed.

## Circulation

| Method | Path | Notes |
|---|---|---|
| GET | `/me/loans` | |
| POST | `/me/loans/{id}/renewal` | Returns the updated loan, or 409 with `MAX_RENEWALS` / `HOLD_QUEUED` |
| POST | `/me/loans/renewals` | "Renew all eligible": returns a result per loan |
| GET | `/me/holds` | |
| POST | `/me/holds` | `{bookId, pickupBranchId}`. Returns 201 with the queue position. |
| DELETE | `/me/holds/{id}` | |
| GET | `/me/saved` | |
| PUT | `/me/saved/{bookId}` | Save (idempotent) |
| DELETE | `/me/saved/{bookId}` | Unsave (idempotent) |

Checkout and return happen at the desk, so they're out of scope for this demo.

## Reading lists

| Method | Path | Notes |
|---|---|---|
| GET | `/me/lists` | Summaries only: `itemCount`, a collaborator preview, no items |
| POST | `/me/lists` | `{title, bookId?}`. The optional `bookId` covers "create list and add this book". |
| GET | `/lists/{id}` | Items with book summaries embedded. Not under `/me` because cohort and public lists exist. |
| PATCH | `/lists/{id}` | Title, description, visibility, `settings` |
| DELETE | `/lists/{id}` | |
| POST | `/lists/{id}/items` | `{bookId, week?, note?}` |
| PATCH | `/lists/{id}/items/{bookId}` | Note and position (reorder) |
| DELETE | `/lists/{id}/items/{bookId}` | |
| GET | `/me/lists/activity?limit=` | The "Recent Bibliographic Ledger" |

## Fines

| Method | Path | Notes |
|---|---|---|
| GET | `/me/fines?status=` | |
| GET | `/me/fines/summary` | |
| GET | `/me/payments` | |
| POST | `/me/payments` | `{fineIds[], method: grant\|smartcard\|card}` with an `Idempotency-Key` header. The card method returns a gateway redirect. |

## History

| Method | Path | Notes |
|---|---|---|
| GET | `/me/history` | Filter by `year` and `format`; paged |
| GET | `/me/history/summary?year=` | |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/me/notifications` | Filter by `category` and `unread`. Each item returns `kind`, `category`, `createdAt`, `read`, `bookId`, `loanId`. The client decides the buttons; "Renew" just calls the renewal endpoint. |
| PATCH | `/me/notifications/{id}` | `{read: true}` |
| POST | `/me/notifications/read-all` | |

## Digital resources and journals

| Method | Path | Notes |
|---|---|---|
| GET | `/digital-resources` | Takes `type`, `q`, `subject[]`, `access`, `sort=recent\|title`, page params. Returns items plus a `counts` object per type, which replaces `DigitalResourceTab`. |
| GET | `/serials` | Takes `q`, `subject`, the flags `peerReviewed`/`openAccess`/`fullText`/`print`, and `sort=impact\|title\|holdings` |
| GET | `/serials/{id}` | Records the view server-side, so no separate tracking call is needed |
| GET | `/serials/lookup?issn=` | ISSN modal |
| GET | `/me/serials/recent` | |

## Admin: Add Resource

Maps directly onto the existing `metadata` module.

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/cataloging/providers` | Enabled provider toggles |
| POST | `/admin/cataloging/lookup` | `{idType: isbn\|title\|asin\|doi, value, format, providers[]}`. Returns `{matches:[{provider, confidence, coverUrl, record}], conflicts:{field:[{provider, value}]}}`. The UI's `pickers` text is built from `conflicts`. |
| POST | `/admin/books` | The final record. Returns 201, or 409 on a duplicate ISBN or OLID. |

## Home page

No endpoint of its own. It calls these in parallel:

- `/me/loans`
- `/me/holds`
- `/books/new-arrivals?size=5`
- `/me/lists`
- `/me/notifications`

## Deferred

- **Ask a Librarian:** the demo replies are canned. A real version needs conversations, messages and SSE.
- **SSO**, **recommendations**, **list collaborators**.
- **`ListsOverview`:** two vanity counters (saved citations, active scholars), not worth an endpoint yet.

## Schema gaps

`schema.sql` currently has books, authors, users, credentials and sessions only. The demo also needs:

- **New tables:** `branches`, `copies` (branch, status, call number, acquired date), `loans`, `holds`, `subjects`, `reading_lists` and `reading_list_items`, `saved_books`, `fines`, `payments`, `notifications`, `digital_resources`, `serials`.
- **New columns on `books`:** `resource_type`, `doi`, `edition`, `toc` (jsonb).

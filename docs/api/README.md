# OpenShelves API

API surface derived from the frontend demo site (`openshelves-demo-site`: `lib/api/*` for reads, `lib/store.tsx` for actions) and the current backend. Each group has its own file; endpoints get designed there.

## Groups

| # | Group | Reads | Writes | Backend today |
|---|---|---|---|---|
| 1 | [Auth & Sessions](./01-auth-sessions.md) | Current session | Password login, logout, SAML/OIDC start and callback, "keep me signed in" / kiosk mode | Login, session and logout done (`AuthController`, `/api/v1/auth`). SSO not started. |
| 2 | [Me / Account](./02-me-account.md) | Profile (card no., program, department, home library), account standing, borrow limit, sidebar badge counts | Probably none at first (profile is read-only) | `users` table only. |
| 3 | [Catalog & Discovery](./03-catalog-discovery.md) | Search with facets and pagination, book detail (copies, TOC, contributors), related books, new arrivals, recommendations, catalog stats, home highlights | None | `books`, `authors`, `book_authors` tables. No controllers yet. |
| 4 | [Circulation](./04-circulation.md) | Loans, holds | Renew a loan, renew all eligible, place a hold, cancel a hold | Nothing. |
| 5 | [Reading Lists (Folios)](./05-reading-lists.md) | Lists, list detail, overview stats, recent ledger | Create a list, add or remove an item, list settings (visibility) | Nothing. |
| 6 | [Saved Items](./06-saved-items.md) | Saved books, journals and digital resources | Save / unsave | Nothing. |
| 7 | [Fines & Payments](./07-fines-payments.md) | Fines, summary, payment history | Pay (smartcard or card) | Nothing. |
| 8 | [Reading History](./08-reading-history.md) | Paginated history, reading summary | None | Nothing. |
| 9 | [Notifications](./09-notifications.md) | Notification feed | Mark one read, mark all read. Inline actions reuse Circulation endpoints. | Nothing. |
| 10 | [Digital Resources](./10-digital-resources.md) | Resources by tab (e-books, databases, theses, datasets) | Access / launch the resource | Nothing. |
| 11 | [Journals & Serials](./11-journals-serials.md) | Serials list, recently viewed | ISSN lookup, save | Nothing. |
| 12 | [Admin: Cataloging](./12-admin-cataloging.md) | Draft and provider matches | Look up an identifier across providers, pick a match, edit metadata, save the record | Provider clients exist (Open Library, Google Books, Hardcover, ComicVine, RanobeDB). |

## Scope notes

- **Out of the API:** citations (APA/MLA/BibTeX) are generated on the client. Librarian Chat is mocked replies; deferred.
- **Merge or defer:**
  - Saved Items could fold into Reading Lists as a default "Saved" list (but that mixes books with journals and digital resources).
  - Journals and Digital Resources can start as filtered views of Catalog, and only get their own groups once they have their own data (ISSN, access links).
- **Build order:** 1 → 2 → 3 → 4 → 5; everything else depends on catalog and circulation.

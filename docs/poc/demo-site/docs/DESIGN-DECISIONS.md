# Design decisions: main points

The original chat is gone, so this file is the memory. The owner (Arpan) has approved every current screen.
**Don't restyle anything unasked.** The designs are in `design-reference` folder (Stitch export: `code.html` and
`screen.png` per screen).

## Scope
- Built 13 design screens plus `/profile`, which has no design. `scholar_workspace` was skipped (it duplicates Home).
- The Recommended page has no design, so it stays inert.
- Desktop only. Responsive layouts were deliberately deferred.

## Look and feel
- Only Literata (headings) and DM Sans (UI), bundled locally.
- No new `font-mono`. Use `tabular-nums` instead.
- The whole UI is scaled to 90% (`html { zoom: 0.9 }`) because it looked too big on the owner's Mac. Avoid `vh`.
- Every page uses the same width: `mx-auto max-w-[1600px] px-8`.
- Top-bar icons are plain icons with no border or box.

## Shell
- The sidebar is fixed, full height, with the logo pinned. Its bottom "Library Network" badge must always stay at the bottom.
- The top-bar search is centred on the **screen**, not on the header.
- Top-bar icons, left to right: Reading lists, Ask a Librarian, Help (inert), Notifications, then the profile.
- The design's "New List" button was removed.
- Holds is not in the top bar; it is in the sidebar only.

## Content
- The user is **Aditya Menon, Computer Science**, and the department is shown under the name.
- The institution is **Central University Library**. No Cambridge or Eleanor Vance.
- Today is fixed at 2024-10-24 (`lib/dates.ts`). Counts come from the mock data (for example, fines are $14.50).

## Per-feature decisions
- **Catalog:** filters are on the **right**, so the results stay centred.
- **Notifications:** the flyout always spans to the bottom edge. Its empty state has an illustration.
- **Ask a Librarian** (replaced the Holds icon in the top bar):
  - It's an anonymous help desk that any librarian can answer; each reply shows the replier's name.
  - The panel is bottom-right, with messages bottom-aligned.
  - Replies are canned in the demo.
- **Profile:** the avatar opens a menu (View profile, Settings, Sign out). `/profile` shows account details and sign-out only. Settings opens `/profile` for now.
- **Sign in:** the footer sits below the card. Kiosk mode visibly disables "keep me signed in" and shows a note.
- **Journals:** the search starts empty, not prefilled.
- **Add Resource** (librarian): it uses the normal shell and isn't linked from the sidebar.

## Structure
- Keep the current type-based folders (`components/`, `components/<screen>/`, `lib/api/`, `mock/`, `types/`).
- A feature-folder layout was tried and **rejected**.
- Route groups `(library)`, `(admin)` and `(auth)` decide whether a page gets the shell.

## Still open (ask the owner)
- Replace existing `font-mono` with DM Sans?
- Switch to Computer Science book data?
- New Holds and Reading-list icons?
- A real Settings page?
- Responsive layouts?

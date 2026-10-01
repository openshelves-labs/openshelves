# AGENTS.md: OpenShelves frontend

Read this before changing anything. It is the working agreement for this repo, for humans and AI agents alike.

Also read **[docs/DESIGN-DECISIONS.md](docs/DESIGN-DECISIONS.md)**, the key design decisions and open questions.
The original Stitch designs are in `../design-reference/` (each screen has `code.html` and `screen.png`; tokens are in `scholastic_paper/DESIGN.md`).

## What this is

OpenShelves is an academic library portal (student side plus one librarian screen), built from a Google Stitch
design export. It is a **frontend-only demo**: all data is typed mock data and every interaction is client state.
There is no backend yet. The Java backend will replace the mock layer later.

- **Stack:** Next.js 15 App Router, React 19, TypeScript (strict), Tailwind CSS 3.4.
- **Demo user:** Aditya Menon, MSc Computer Science, Central University Library (`mock/users.ts`).
- **Fixed "today":** `NOW = 2024-10-24` in `lib/dates.ts`. All "due in N days" labels are relative to it, so don't use `new Date()` for domain dates.

## Commands

```bash
npm install
npm run dev      # http://localhost:3000
npm run lint     # = tsc --noEmit (type-check; there is no ESLint yet)
npm run build
```

`NEXT_DIST_DIR=.next-foo npx next dev -p 3301` runs a second dev server with its own build dir (see `next.config.mjs`).

## Layout

```
app/
  layout.tsx            <html>, fonts, loads all initial data via lib/api -> <LibraryProvider>
  globals.css           tokens-level CSS, html { zoom: 0.9 }, icon font settings
  (library)/            student screens; layout.tsx wraps them in <AppShell>
    page.tsx            /           home
    catalog/            /catalog, /catalog/[id]
    loans/ lists/ fines/ new-arrivals/ history/ journals/ digital-resources/ profile/
  (admin)/admin/add-resource/   librarian "Catalog New Resource" (own layout, same shell for now)
  (auth)/sign-in/       full-bleed, no sidebar/top bar
components/
  AppShell.tsx          Sidebar + TopBar + LibrarianChat (used by group layouts)
  Sidebar, TopBar, ProfileMenu, NotificationsPanel, LibrarianChat
  Icon, StatusChip, Avatar, BookCover, BookCard, CiteButton, CiteModal, HoldButton,
  SaveButton, AddToListButton, LoanRow, NewListModal, Breadcrumbs, SectionHeader, Logo
  <screen>/             components used by one screen only (home, catalog, loans, lists,
                        fines, history, arrivals, journals, digital, admin, auth, profile)
lib/
  api.ts                data-layer barrel: re-exports lib/api/*.ts
  api/*.ts              async functions returning structuredClone'd mock data
  store.tsx             LibraryProvider + useLibrary(): client state (loans, holds, lists, saved, notifications, modals)
  chat.tsx              open/close state for the Ask-a-Librarian chat
  catalog.ts            catalog facets, filtering, sorting, paging
  citations.ts          formatCitation(book, "apa" | "mla" | "chicago" | "bibtex")
  dates.ts              NOW, daysUntil, dueLabel, formatDate…
mock/                   typed fixtures; mock/books.ts concatenates books-*.ts
types/index.ts          domain entities (User, Book, Loan, Hold, ReadingList, Fine, AppNotification…)
```

A **new page** goes in the matching route group. It gets the shell automatically; no path lists to update.

## Data rules

1. Server `page.tsx` files are `async` and fetch through `import * as api from "@/lib/api"`, then pass props to a `"use client"` view.
2. Components never import `@/mock`. Only `lib/api/*` (and other mock files) may. The one exception, a type-only import in `components/lists/ListsIndexView.tsx`, should move to `types/`.
3. Shared interactive state goes through `useLibrary()` (renew, place/cancel hold, save, list add/remove/create, notifications, `openCite`, `openNewList`). The sidebar badges read the store, so counts update everywhere.
4. Next 15: `params` and `searchParams` are Promises (`const { id } = await params`). `useSearchParams()` needs a `<Suspense>` boundary.
5. To go live, rewrite `lib/api/*.ts` as `fetch` calls with the same signatures. Nothing else should need to change.

## Visual rules (important: the owner has approved the current screens)

- **Don't restyle existing screens.** Unless asked, leave layout, spacing and copy alone on approved pages.
- **Tokens:** design tokens from the Stitch `DESIGN.md` live in `tailwind.config.ts`: colours (primary `#2F5D45`, canvas `#F3F1EC`, hairline `#E5E0D8`), type scale, spacing, radius. Use token classes such as `bg-surface-paper`, `border-border-warm`, `text-outline`, `font-metadata-caps` and `font-title-editorial`.
- **Fonts:** only two families. **Literata** for editorial and headings (`font-title-editorial`, `font-headline-*`, `font-display-*`) and **DM Sans** for everything else (`font-body-*`, `font-label-*`, `font-metadata-caps`). Both are bundled via `@fontsource-variable`, not Google Fonts. Don't add `font-mono` in new code: use `tabular-nums` for IDs and amounts. A few older spots still use `font-mono`, and the owner hasn't decided whether to change them.
- **Font classes:** `font-*` sets the family only, so set weight with `font-semibold` and similar.
- **Icons:** Material Symbols Outlined via `<Icon name="…" filled? />` (bundled `material-symbols` package). Top-bar icons are bare icons with no border or box.
- **90% zoom:** the whole UI is scaled by `html { zoom: 0.9 }`. `h-screen` and `min-h-screen` are redefined as `calc(100vh / 0.9)`. Avoid raw `vh`/`vw` in new code. For full-height overlays use `fixed` with `top-*`/`bottom-*` insets (as `NotificationsPanel` and `LibrarianChat` do).
- **Page gutters:** `<main className="mx-auto w-full max-w-[1600px] px-8 py-8">`, the same on every page.
- **Shell details the owner asked for:**
  - The sidebar is `fixed` and full height, with a spacer div, so its bottom "Central University Library Network" badge stays pinned.
  - The top-bar search is centred on the viewport, not the header (`-ml-32` compensates for the 16rem sidebar).
  - Catalog filters sit on the right.
  - The notification centre always spans from under the top bar to the bottom and has an illustrated empty state.
  - The Ask-a-Librarian chat is bottom-anchored with bottom-aligned messages. The librarian is anonymous ("Library Help Desk"), and each reply shows the replying librarian's name.
- **Desktop only:** responsive and tablet layouts are deliberately out of scope for now.
- **Cover images:** they hotlink `lh3.googleusercontent.com`. `BookCover` and `Avatar` fall back to generated art if an image fails.

## Behaviour notes

- **Demo-only interactions:** sign-in, sign-out and kiosk mode are demo-only. Sign-in routes to `/`, and sign-out links to `/sign-in`.
- **Librarian chat:** replies are canned keyword matches in `components/LibrarianChat.tsx`, with a fake rotating librarian name. Replace them with a real messaging API when a backend exists.
- **Inert controls (render but do nothing, by design):** Export, Share, Statement PDF, Launch Database, the Recommended sidebar item, Help, and "Notification Settings". The profile menu's "Settings" opens `/profile`.
- **Accepted differences from the designs:** counts come from the mock data, so they differ from the static numbers in the designs (for example, the fines balance is $14.50, not $0.00). The design's "Dr. Eleanor Vance / Cambridge" identity is replaced by Aditya / Central University everywhere.

## Working agreement

- Stay within the scope asked. Prefer the smallest change that does the job; the owner dislikes over-engineering.
- **Structure:** a feature-based restructure (`features/*`) was tried and reverted. Keep the current layout unless the owner asks otherwise.
- **After a change:** run `npm run lint`, then check the affected route in a browser at about 1440px wide.

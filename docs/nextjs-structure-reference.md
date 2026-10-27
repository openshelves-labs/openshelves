# Next.js Project Structure — Reference

Next.js frontend (App Router, TypeScript) acting as a client/BFF for a **separate Spring Boot backend**. Organized **by feature**, with a thin shared layer.

---

## 1. Folder tree (example: library app)

```
library-web/
├── .env.local                          # BACKEND_URL=http://localhost:8080 (no NEXT_PUBLIC_)
├── .env.example
├── next.config.ts
├── tsconfig.json                       # paths: "@/*" → "src/*"
├── package.json
│
└── src/
    ├── proxy.ts                        # Next 16+ (was middleware.ts) — MUST be inside src/
    │
    ├── app/                            # routes only, thin
    │   ├── layout.tsx                  # root layout (html/body)
    │   ├── page.tsx                    # dashboard
    │   ├── error.tsx
    │   ├── not-found.tsx
    │   ├── (auth)/
    │   │   └── login/
    │   │       └── page.tsx
    │   └── (main)/
    │       ├── layout.tsx              # shell: renders Navbar + Sidebar
    │       ├── books/
    │       │   ├── page.tsx            # list
    │       │   ├── loading.tsx
    │       │   ├── new/
    │       │   │   └── page.tsx
    │       │   └── [id]/
    │       │       ├── page.tsx        # detail
    │       │       └── edit/
    │       │           └── page.tsx
    │       ├── members/
    │       │   ├── page.tsx
    │       │   └── [id]/
    │       │       └── page.tsx
    │       └── loans/
    │           ├── page.tsx
    │           └── new/
    │               └── page.tsx
    │
    ├── features/                       # domain slices backed by Spring endpoints
    │   ├── auth/
    │   │   ├── auth.service.ts         # login, logout, getSession
    │   │   ├── auth.actions.ts         # "use server"
    │   │   ├── auth.types.ts           # LoginRequest, SessionUser
    │   │   └── components/
    │   │       └── LoginForm.tsx
    │   │
    │   ├── books/
    │   │   ├── book.service.ts         # getBooks, getBook, createBook, updateBook, deleteBook
    │   │   ├── book.actions.ts         # createBookAction, updateBookAction, deleteBookAction
    │   │   ├── book.types.ts           # BookDto, CreateBookRequest, UpdateBookRequest
    │   │   └── components/
    │   │       ├── BookTable.tsx
    │   │       ├── BookCard.tsx
    │   │       ├── BookForm.tsx        # "use client"
    │   │       └── BookSearchBar.tsx   # "use client"
    │   │
    │   ├── members/
    │   │   ├── member.service.ts
    │   │   ├── member.actions.ts
    │   │   ├── member.types.ts
    │   │   └── components/
    │   │       ├── MemberTable.tsx
    │   │       └── MemberForm.tsx
    │   │
    │   └── loans/
    │       ├── loan.service.ts         # issueLoan, returnLoan, getActiveLoans
    │       ├── loan.actions.ts
    │       ├── loan.types.ts
    │       └── components/
    │           ├── LoanTable.tsx
    │           └── IssueLoanForm.tsx   # imports @/features/books/..., @/features/members/... directly
    │
    ├── server/                         # shared, server-only
    │   ├── http-client.ts              # api<T>(): base URL, auth header, error mapping
    │   ├── http-errors.ts              # ApiError
    │   ├── session.ts                  # (later) session cookie name/options + get/set/clear; used by http-client, auth
    │   └── env.ts                      # (later) reads + checks server env vars (BACKEND_URL), fails fast if missing
    │
    ├── shared/
    │   ├── types/
    │   │   ├── paginated-response.types.ts # (later) PaginatedResponse<T>: backend paging response (PagedModel)
    │   │   └── error-response.types.ts # ErrorResponse: backend error body (ApiErrorResponse.java)
    │   ├── components/
    │   │   ├── ui/                     # generic, app-agnostic, props-driven
    │   │   │   ├── Button.tsx
    │   │   │   ├── Input.tsx
    │   │   │   ├── Table.tsx
    │   │   │   ├── Pagination.tsx
    │   │   │   └── Modal.tsx
    │   │   └── layout/                 # app-specific shell (knows routes/labels)
    │   │       ├── Navbar.tsx
    │   │       └── Sidebar.tsx
    │   └── lib/
    │       ├── format.ts
    │       └── cn.ts
    │
    └── styles/
        └── globals.css
```

---

## 2. Rules

1. **Feature-local first.** Code used by one feature lives in that feature's folder.
2. **Promote on second use.** Used by 2+ features → move to `shared/` or `server/`.
3. **No barrel files.** Import feature files directly (`@/features/books/book.types`). No `index.ts` re-exports — they leak `server-only` services into client bundles and add a file to keep in sync.
4. **`app/` stays thin.** Pages compose feature components and call feature services — no logic.
5. **Features = domain slices** (service + types + actions + components backed by backend endpoints). UI chrome is not a feature.
6. **Server-only code** (`server/*`, `*.service.ts`) starts with `import "server-only";`.
7. **Types are plain `type`/`interface`**, not classes — data crossing server→client is serialized.

---

## 3. Layers & call flow

| Concern | Location | Java analogy |
|---|---|---|
| Routes / pages | `app/` | Controller |
| Backend calls | `features/*/x.service.ts` | Feign client |
| Mutations from client | `features/*/x.actions.ts` | — |
| DTOs | `features/*/x.types.ts`, `shared/types/` | DTO |
| HTTP plumbing | `server/http-client.ts` | RestTemplate/WebClient config |

**Reads:** Server Component → feature service → `server/http-client.ts` → Spring
**Writes:** Client component → Server Action → feature service → Spring

---

## 4. Why types are separate from services

- Client components need DTOs, but services are `server-only`; a regular import would break the build.
- Generated types (OpenAPI) get overwritten — keep them isolated from hand-written code.
- Types are a leaf dependency → no circular imports.

(`import type` is erased at compile time, but one accidental value import breaks the build.)

---

## 5. `ui/` vs `layout/`

| | `ui/Button` | `layout/Navbar` |
|---|---|---|
| Knows about the app? | No | Yes (routes, labels, logo) |
| Reusable in another project? | Yes | No |
| Driven by | Props | Hardcoded app config |

**Test:** could you copy it unchanged into another project? → `ui/`. Otherwise → `layout/` or a feature.

Navbar stays dumb. Anything with backend logic (notifications, user menu from session) becomes a feature and is rendered inside it:

```tsx
// shared/components/layout/Navbar.tsx
import { NotificationBell } from "@/features/notifications/components/NotificationBell";

const NAV_ITEMS = [
  { href: "/books", label: "Books" },
  { href: "/members", label: "Members" },
  { href: "/loans", label: "Loans" },
];

export function Navbar() {
  return (
    <nav>
      {NAV_ITEMS.map(i => <Link key={i.href} href={i.href}>{i.label}</Link>)}
      <NotificationBell />
    </nav>
  );
}
```

Alternative: if a shell component is used by only one route group, colocate it in `app/(main)/_components/` (`_` = private, not a route).

---

## 6. `proxy.ts` (formerly `middleware.ts`)

- **Location:** `src/proxy.ts` when using `src/`; project root otherwise. Wrong location is silently ignored.
- **Next 16+:** file is `proxy.ts`, export `proxy`. `middleware.ts` still works but is deprecated.
- **Keep it cheap:** check cookie presence only. Don't call Spring here — real validation happens when `http-client.ts` forwards the token and Spring returns 401.

```ts
// src/proxy.ts
import { NextResponse, type NextRequest } from "next/server";

export function proxy(req: NextRequest) {
  if (!req.cookies.get("session")?.value) {
    return NextResponse.redirect(new URL("/login", req.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!login|_next/static|_next/image|favicon.ico).*)"],
};
```

---

## 7. Key snippets

**`server/http-client.ts`**

```ts
import "server-only";
import { cookies } from "next/headers";
import { ApiError } from "./http-errors";

const BASE = process.env.BACKEND_URL!; // no NEXT_PUBLIC_ → never reaches the browser

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const token = (await cookies()).get("session")?.value;
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => undefined); // non-JSON errors (e.g. proxy 502)
    throw new ApiError(res.status, body);
  }
  return res.json() as Promise<T>;
}
```

**`shared/types/paginated-response.types.ts`** — add once the backend paginates

```ts
// Mirrors Spring Data PagedModel (@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)).
// Don't return Page<T> from controllers — its JSON shape isn't stable.
export type PaginatedResponse<T> = {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
};
```

**`server/http-errors.ts`**

```ts
import "server-only";
import type { ErrorResponse } from "@/shared/types/error-response.types";

export class ApiError extends Error {
  constructor(public status: number, public body?: ErrorResponse) {
    super(body?.detail ?? body?.title ?? `HTTP ${status}`);
  }
}
```

**`shared/types/error-response.types.ts`**

```ts
// Mirrors backend ApiErrorResponse (RFC 9457 + extensions)
export type ErrorResponse = {
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code: string;            // machine-readable, e.g. "UNAUTHORIZED" — branch on this
  timestamp: string;       // ISO instant
  [key: string]: unknown;  // extra top-level properties
};
```

`ErrorResponse` vs `ApiError`:

| | `ErrorResponse` | `ApiError` |
|---|---|---|
| What | Type: JSON body the backend sends | `Error` class thrown by `http-client` |
| Where | `shared/types/` — server + client | `server/` — server only |
| Serializable to client | Yes | No (Next strips server errors) |

`ApiError` wraps `ErrorResponse`. Server code catches `ApiError`; a Server Action returns `error.body` when the client form needs it.

**`features/books/book.service.ts`**

```ts
import "server-only";
import { api } from "@/server/http-client";
import type { PaginatedResponse } from "@/shared/types/paginated-response.types";
import type { BookDto, CreateBookRequest } from "./book.types";

export const getBooks = (page = 0) => api<PaginatedResponse<BookDto>>(`/api/books?page=${page}`);
export const getBook = (id: string) => api<BookDto>(`/api/books/${id}`);
export const createBook = (b: CreateBookRequest) =>
  api<BookDto>("/api/books", { method: "POST", body: JSON.stringify(b) });
```

---

## 8. DTOs from Spring (optional)

Spring Boot: add `springdoc-openapi`. Then:

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/shared/types/api.generated.ts
```

Re-export per feature:

```ts
// features/books/book.types.ts
import type { components } from "@/shared/types/api.generated";
export type BookDto = components["schemas"]["BookDto"];
```

Skip Zod for backend responses (Spring already validates); use it only for form input if needed.

---

## 9. Feature vs layer — when to choose

- **Feature-based (default):** changes stay in one folder; easy to delete/extract; clear ownership.
- **Layer-based:** acceptable for small apps (< ~5 features) of thin CRUD pass-throughs.

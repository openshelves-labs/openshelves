# 🚦 Rate Limiting

*Even the quietest reading room has someone at the door.*

[← Docs](README.md)

What OpenShelves limits today, what it leaves to the reverse proxy, and what to add later as the platform grows.

**Contents**

1. [🧭 The short version](#-the-short-version)
2. [✅ Needed now](#-needed-now)
3. [🌐 Left to the reverse proxy](#-left-to-the-reverse-proxy)
4. [🔭 Future scope](#-future-scope)
5. [🚫 Not rate limiting](#-not-rate-limiting)

---

## 🧭 The short version

There is **no general rate-limiting framework** in the app for now. The only public endpoints are the auth ones, and a self-hosted install normally sits behind nginx, Caddy or Traefik, which can handle per-IP floods.

Two rules live inside the auth flow itself, because without them sign-in isn't safe on day one.

---

## ✅ Needed now

| | Rule | Where | Why |
|---|---|---|---|
| 🔑 | **Login backoff per account**: after 5 failed attempts, each response is delayed (1s → 2s → 4s …, max 30s). After 20 failures in an hour, password login locks for 15 minutes. A successful login resets the count. | `AuthService`, state in `user_passwords` | Student IDs are sequential and printed on ID cards, so they're easy to guess. The delay slows guessing. The lock comes late and is short, so nobody can lock a classmate out for long. |
| ✉️ | **Reset requests per email**: at most 3 per hour per address. The response is always the same `202`, whether or not the account exists. | Password reset flow (once emails are actually sent) | Protects the institution's mail relay quota. The identical response stops anyone checking which emails are registered. |

Ground rules for both:

- Failures count **per account, across all IPs**, so spreading attempts over many IPs doesn't help.
- A lock never blocks **SSO login** or **password reset**. The real owner can always get back in.
- When limited, return `429` with `Retry-After` and a plain message, e.g. *"Too many sign-in attempts. Please wait a few minutes or ask at the library desk."*
- The numbers are starting defaults. Make them configurable properties.

---

## 🌐 Left to the reverse proxy

Coarse per-IP limiting (floods, broken clients looping on an endpoint) belongs in front of the app, not in it. The deployment guide should suggest a basic per-IP limit on `/api/v1/auth/**`.

Keep it **generous**: a hostel, lab or campus Wi-Fi often reaches the server through a single public IP.

---

## 🔭 Future scope

Add these when the trigger happens, not before.

| | Trigger | What to add |
|---|---|---|
| 📚 | Catalogue becomes browsable without sign-in | Per-IP limits on anonymous search (the expensive one), record pages and exports. Cap pagination depth. Publish `robots.txt` and a sitemap of record pages so crawlers don't need search. |
| 📱 | OTP, magic links or SMS | Per-account send limits (e.g. 3 per 15 min, 10 per day). SMS costs money, so cap it more tightly than email. A code is invalid after 5 wrong tries. |
| 📝 | Self-registration (alumni, external members) | Per-email and per-IP limits, a CAPTCHA, email verification and librarian approval. |
| 🖥️ | Kiosks or an off-campus proxy (e.g. EZproxy) in real use | Admin-defined **trusted IP ranges** with a higher per-IP allowance. Per-account rules still apply. |
| 🔀 | Frontend calls the API server-side, or a proxy sits in front | Trust `X-Forwarded-For` only from known proxies. Group IPv6 by /64. |
| 👤 | Signed-in users scrape or a client loops | Per-user buckets: a general cap (around 120/min), a separate search cap, small caps on holds and exports. The limit applies per user, not per session. |
| 🛠️ | Cataloguing uses outside metadata APIs heavily | One **instance-wide** limit per provider (Open Library, Crossref…), queued rather than failed, so the whole install doesn't get banned. |
| 🔌 | Integrations (LMS, self-check, RFID) | An API key and admin-set quota per client. Alert an admin on repeated overruns. Never stop a patron at the gate. |
| 📈 | Real deployments show abuse | Log every `429`, plus an admin view of locked accounts and a button to clear a lock at the desk. |

Rules of thumb for all of these:

- **Tight per account, loose per IP.**
- **Slow attackers down rather than locking real users out.**
- **Size limits by abuse patterns, not average traffic.** Semester start, reading-list release and the night before due dates are legitimate 10× spikes.

---

## 🚫 Not rate limiting

These are borrowing rules and belong in the domain, never in a rate limiter:

- Max loans, holds and renewals per patron type
- Fines blocking borrowing
- Hold queue order and pickup windows
- E-resource licence seats

A rate limit says *"slow down"*, and waiting fixes it. A borrowing rule says *"not allowed"*, and waiting changes nothing.

---

<div align="center">

[← Back to the docs](README.md)

</div>

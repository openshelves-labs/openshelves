"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Icon from "@/components/Icon";
import BrandPanel from "./BrandPanel";

const INSTITUTIONS = [
  { id: "central", label: "Central University (Campus SSO)" },
  { id: "north", label: "Northern Institute of Technology (Federated ID)" },
  { id: "coastal", label: "Coastal State University (CoastKey)" },
  { id: "metro", label: "Metropolitan College Consortium (UniAuth)" },
  { id: "other", label: "Institutional Shibboleth / InCommon Federation..." },
];

const FOOTER_LINKS = ["Library Helpdesk", "Privacy & Telemetry", "Accessibility (WCAG 2.1 AAA)", "Usage Charter"];

type Pending = "saml" | "password" | null;

const FIELD =
  "h-11 w-full rounded-lg bg-surface-container-lowest font-body-md text-body-md text-on-surface shadow-sm transition-all focus:outline-none focus:ring-2 focus:ring-primary-container";

export default function SignInView() {
  const router = useRouter();
  const [institution, setInstitution] = useState(INSTITUTIONS[0].id);
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("scholar-2024");
  const [showPassword, setShowPassword] = useState(false);
  const [keepSignedIn, setKeepSignedIn] = useState(true);
  const [kiosk, setKiosk] = useState(false);
  const [pending, setPending] = useState<Pending>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);

  const start = (kind: Exclude<Pending, null>) => {
    if (pending) return;
    setPending(kind);
    timer.current = setTimeout(() => router.push("/"), 900);
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    start("password");
  };

  return (
    <main className="w-full px-4 py-6 md:px-8">
      <div className="mx-auto flex w-full max-w-6xl flex-col overflow-hidden rounded-xl bg-surface-container-lowest shadow-xl lg:flex-row">
        <BrandPanel />

        <div className="flex flex-col justify-between bg-surface-bright p-8 lg:w-7/12 lg:p-14">
          <div className="mx-auto w-full max-w-lg">
            <div className="mb-8 flex items-center justify-between">
              <div className="inline-flex items-center gap-2 rounded-full bg-surface-container px-3 py-1 font-label-md text-label-md text-on-surface-variant">
                <span className="h-2 w-2 rounded-full bg-secondary" />
                <span>All catalog services operational</span>
              </div>
              <span className="font-metadata-caps text-metadata-caps uppercase tabular-nums text-outline">Auth v4.8</span>
            </div>

            <div className="mb-8">
              <h2 className="mb-2 font-headline-md text-headline-md text-on-surface">Welcome back, scholar</h2>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Authenticate to access licensed institutional databases, reserve manuscripts, and manage your reading desk folios.
              </p>
            </div>

            <div className="mb-6 space-y-2.5">
              <label htmlFor="institution-select" className="block font-label-md text-label-md font-medium text-on-surface">
                Institutional Federated Sign-In
              </label>
              <div className="relative">
                <select
                  id="institution-select"
                  value={institution}
                  onChange={(e) => setInstitution(e.target.value)}
                  className={`${FIELD} cursor-pointer appearance-none pl-11 pr-10`}
                >
                  {INSTITUTIONS.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.label}
                    </option>
                  ))}
                </select>
                <Icon name="account_balance" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-primary" />
                <Icon name="unfold_more" className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-outline" />
              </div>
              <button
                type="button"
                onClick={() => start("saml")}
                disabled={pending !== null}
                className="mt-2 flex h-11 w-full items-center justify-center gap-2.5 rounded-lg bg-surface-container font-label-lg text-label-lg text-on-surface shadow-sm transition-colors hover:bg-surface-container-high disabled:opacity-80"
              >
                <Icon name={pending === "saml" ? "progress_activity" : "login"} className={`text-[20px] text-primary ${pending === "saml" ? "animate-spin" : ""}`} />
                <span>{pending === "saml" ? "Redirecting to campus SSO…" : "Authenticate via University Shibboleth / SAML"}</span>
              </button>
            </div>

            <div className="relative my-7 flex items-center justify-center">
              <div className="absolute inset-0 flex items-center">
                <div className="h-[1px] w-full bg-surface-container-highest" />
              </div>
              <span className="relative bg-surface-bright px-4 font-label-md text-label-md text-outline">or sign in with library credentials</span>
            </div>

            <form className="space-y-4" onSubmit={onSubmit}>
              <div>
                <div className="mb-1.5 flex items-center justify-between">
                  <label htmlFor="library-identifier" className="font-label-md text-label-md font-medium text-on-surface">
                    Library Card Number or Institutional Email
                  </label>
                  <span className="font-metadata-caps text-metadata-caps uppercase text-outline">Barcode / ID</span>
                </div>
                <div className="relative">
                  <input
                    id="library-identifier"
                    type="text"
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    placeholder="e.g. aditya.menon@student.centraluniversity.edu or CUL-2024-04172"
                    className={`${FIELD} truncate pl-11 pr-4 placeholder:text-outline`}
                  />
                  <Icon name="badge" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-outline" />
                </div>
              </div>

              <div>
                <div className="mb-1.5 flex items-center justify-between">
                  <label htmlFor="library-pass" className="font-label-md text-label-md font-medium text-on-surface">
                    Library PIN / Password
                  </label>
                  <button type="button" className="font-label-md text-label-md text-primary underline-offset-2 hover:underline">
                    Forgot PIN or credentials?
                  </button>
                </div>
                <div className="relative">
                  <input
                    id="library-pass"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className={`${FIELD} pl-11 pr-11 placeholder:text-outline`}
                  />
                  <Icon name="lock" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-outline" />
                  <button
                    type="button"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    aria-pressed={showPassword}
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3.5 top-1/2 flex -translate-y-1/2 items-center justify-center p-0.5 text-outline transition-colors hover:text-on-surface"
                  >
                    <Icon name={showPassword ? "visibility_off" : "visibility"} className="text-[20px]" />
                  </button>
                </div>
              </div>

              <div className="flex flex-col justify-between gap-2.5 pb-2 pt-1.5 sm:flex-row sm:items-center">
                <label className={`flex items-center gap-2.5 ${kiosk ? "cursor-not-allowed opacity-50" : "cursor-pointer"}`}>
                  <input
                    type="checkbox"
                    disabled={kiosk}
                    checked={keepSignedIn && !kiosk}
                    onChange={(e) => setKeepSignedIn(e.target.checked)}
                    className="h-4 w-4 rounded accent-primary focus:ring-primary"
                  />
                  <span className="font-body-sm text-body-sm text-on-surface">{kiosk ? "Keep me signed in (off on shared devices)" : "Keep me signed in on this private desk"}</span>
                </label>
                <button
                  type="button"
                  aria-pressed={kiosk}
                  title="For shared or public computers: auto sign-out and no saved login"
                  onClick={() => setKiosk((v) => !v)}
                  className={`flex items-center gap-1.5 self-start rounded-md px-2.5 py-1 font-label-md text-label-md transition-colors sm:self-auto ${
                    kiosk ? "bg-secondary-container text-on-secondary-container" : "bg-surface-container-low text-on-surface-variant hover:bg-surface-container"
                  }`}
                >
                  <Icon name={kiosk ? "check_circle" : "desktop_windows"} filled={kiosk} className={`text-[15px] ${kiosk ? "text-primary" : "text-outline"}`} />
                  <span>{kiosk ? "Kiosk mode on" : "Public kiosk mode"}</span>
                </button>
              </div>

              {kiosk && (
                <div role="status" className="flex items-start gap-2.5 rounded-lg border border-primary/20 bg-secondary-container/40 px-3.5 py-3 font-body-sm text-body-sm text-on-surface">
                  <Icon name="shield_lock" className="mt-0.5 text-[18px] text-primary" />
                  <p>
                    <span className="font-semibold">Shared-computer protection is on.</span> You&apos;ll be signed out after 5 minutes of inactivity, and this device won&apos;t remember your login or ID.
                  </p>
                </div>
              )}

              <button
                type="submit"
                disabled={pending !== null}
                className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary font-label-lg text-label-lg text-on-primary shadow-md transition-all hover:bg-primary-container active:scale-[0.99] disabled:opacity-80"
              >
                <span>{pending === "password" ? "Signing in…" : "Sign In to OpenShelves"}</span>
                <Icon name={pending === "password" ? "progress_activity" : "arrow_forward"} className={`text-[18px] ${pending === "password" ? "animate-spin" : ""}`} />
              </button>
            </form>

            <div className="mt-8 flex flex-col items-start justify-between gap-3 rounded-lg bg-surface-container-low/70 p-4 font-body-sm text-body-sm sm:flex-row sm:items-center">
              <div className="flex items-center gap-2 text-on-surface">
                <Icon name="how_to_reg" className="text-[20px] text-primary" />
                <span>Visiting fellow or new scholar?</span>
              </div>
              <div className="flex items-center gap-3">
                <button type="button" className="font-semibold text-primary underline-offset-2 hover:underline">
                  Activate card
                </button>
                <span className="text-surface-container-highest">|</span>
                <button type="button" className="text-on-surface-variant underline-offset-2 hover:text-on-surface hover:underline">
                  Proxy / VPN help
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="mx-auto mt-6 flex w-full max-w-6xl flex-col items-center justify-between gap-3 px-2 text-on-surface-variant md:flex-row">
        <div className="flex items-center gap-1.5 font-label-md text-label-md text-outline">
          <Icon name="menu_book" className="text-[15px]" />
          <span>OpenShelves Consortium © 2025</span>
        </div>
        <div className="flex flex-wrap items-center justify-center gap-4 font-label-md text-label-md">
          {FOOTER_LINKS.map((l) => (
            <button key={l} type="button" className="transition-colors hover:text-primary">
              {l}
            </button>
          ))}
          <button type="button" className="flex items-center gap-1 transition-colors hover:text-primary">
            <span className="h-1.5 w-1.5 rounded-full bg-secondary" />
            <span>Network Health</span>
          </button>
        </div>
      </div>
    </main>
  );
}

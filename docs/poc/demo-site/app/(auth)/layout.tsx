import type { ReactNode } from "react";

/** Full-bleed pages without the app shell (sign-in). */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return <div className="min-w-0 flex-1">{children}</div>;
}

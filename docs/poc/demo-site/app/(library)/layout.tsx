import type { ReactNode } from "react";
import AppShell from "@/components/AppShell";

/** Student-facing screens (home, catalog, loans, lists, fines…). */
export default function LibraryLayout({ children }: { children: ReactNode }) {
  return <AppShell>{children}</AppShell>;
}

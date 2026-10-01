import type { ReactNode } from "react";
import AppShell from "@/components/AppShell";

/** Librarian / staff screens. Shares the student shell for now; an admin sidebar or role guard can go here later. */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AppShell>{children}</AppShell>;
}

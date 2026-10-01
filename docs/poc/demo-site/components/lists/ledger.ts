import type { Book } from "@/types";
import { daysUntil, formatShortDate } from "@/lib/dates";

/** Short citation-ish title line: "Holling, C.S. (1973). Resilience and Stability…" */
export function ledgerTitle(b: Book): string {
  const authors = b.contributors.filter((x) => x.role === "author" || x.role === "co-author");
  const list = authors.length ? authors : b.contributors;
  const names = list.map((x) => `${x.family}, ${x.given}`);
  const who = names.length <= 1 ? names[0] ?? "" : `${names[0]} & ${list[1].given} ${list[1].family}`;
  const ed = b.edition ? ` (${b.edition.replace(/Edition/i, "Ed.")})` : "";
  return `${who} (${b.publicationYear}). ${b.title}${ed}.`;
}

/** Chip text for a ledger row: "Indexed" for online works, otherwise circulation state. */
export function ledgerStatus(b: Book): { label: string; indexed: boolean } {
  if (b.availabilityStatus === "online" || b.availabilityStatus === "open_access") return { label: "Indexed", indexed: true };
  const due = b.copies.find((c) => c.dueDate)?.dueDate;
  if (b.availabilityStatus === "checked_out" && due) return { label: `${b.availabilityLabel} (Due ${formatShortDate(due)})`, indexed: false };
  return { label: b.availabilityLabel, indexed: false };
}

/** "Due back in 3 days" style suffix for on-loan books. */
export function dueBackLabel(b: Book): string | null {
  const due = b.copies.find((c) => c.dueDate)?.dueDate;
  if (b.availabilityStatus !== "checked_out" || !due) return null;
  const n = daysUntil(due);
  return `Due back in ${n} day${n === 1 ? "" : "s"}`;
}

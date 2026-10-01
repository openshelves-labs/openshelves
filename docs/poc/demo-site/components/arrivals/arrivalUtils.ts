import type { AvailabilityStatus, Book, ResourceType } from "@/types";

export const TYPE_LABEL: Partial<Record<ResourceType, string>> = {
  monograph: "Monograph",
  "folio-edition": "Folio Edition",
  "digital-resource": "Digital Resource",
  ebook: "E-Book",
  "special-collection": "Special Collection",
  "manuscript-guide": "Manuscript Guide",
  archival: "Archival",
  journal: "Serial",
  dissertation: "Dissertation",
};

export type FormatKey = "print" | "ebook" | "archival" | "serial";

export const FORMAT_LABEL: Record<FormatKey, string> = {
  print: "Print Monograph",
  ebook: "E-Book (Full Access)",
  archival: "Archival Manuscript & Folio",
  serial: "Peer Serial / Volume",
};

export function formatKey(b: Book): FormatKey {
  switch (b.resourceType) {
    case "ebook":
    case "digital-resource":
      return "ebook";
    case "manuscript-guide":
    case "archival":
      return "archival";
    case "journal":
    case "journal-article":
      return "serial";
    default:
      return "print";
  }
}

/** Authors as the design shows them: "A", "A & B", "A et al." */
export function authorLine(b: Book): string {
  const names = b.contributors.filter((c) => c.role === "author" || c.role === "co-author").map((c) => c.name);
  if (names.length <= 2) return names.join(" & ");
  return `${names[0]} et al.`;
}

export const CHIP: Record<AvailabilityStatus, { cls: string; dot?: string; icon?: string }> = {
  available: { cls: "bg-secondary-container text-primary", dot: "bg-primary" },
  online: { cls: "bg-surface-container text-on-surface-variant", icon: "cloud_done" },
  open_access: { cls: "bg-surface-container text-on-surface-variant", icon: "cloud_done" },
  reserve: { cls: "bg-surface-container-highest text-on-surface-variant", dot: "bg-outline" },
  restricted: { cls: "bg-surface-container-highest text-tertiary", icon: "lock" },
  checked_out: { cls: "bg-surface-container-highest text-on-surface-variant", dot: "bg-outline" },
  in_transit: { cls: "bg-surface-container-highest text-on-surface-variant", dot: "bg-outline" },
  on_order: { cls: "bg-surface-container-highest text-on-surface-variant", dot: "bg-outline" },
};

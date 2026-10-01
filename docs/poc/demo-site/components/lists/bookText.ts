import type { Book } from "@/types";

const creators = (b: Book) => {
  const c = b.contributors.filter((x) => x.role === "author" || x.role === "co-author");
  return c.length ? c : b.contributors;
};

export const firstFamily = (b: Book): string => creators(b)[0]?.family ?? "";

/** "A", "A & B", "A, B, & C" */
function joinNames(n: string[]): string {
  if (n.length <= 1) return n[0] ?? "";
  if (n.length === 2) return `${n[0]} & ${n[1]}`;
  return `${n.slice(0, -1).join(", ")}, & ${n[n.length - 1]}`;
}

/** Bold author line: names (+ "et al." for journal articles, ", foreword by X"). */
export function authorLine(b: Book): string {
  if (b.resourceType === "journal-article") return `${creators(b).map((c) => c.name).join(", ")}, et al.`;
  const fw = b.contributors.find((c) => c.role === "foreword");
  return joinNames(creators(b).map((c) => c.name)) + (fw ? `, foreword by ${fw.name}` : "");
}

export function fullTitle(b: Book): string {
  if (!b.subtitle) return b.title;
  return /edition/i.test(b.subtitle) ? `${b.title} (${b.subtitle})` : `${b.title}: ${b.subtitle}`;
}

/** Zero-padded rank ("01"). */
export const pad2 = (n: number) => String(n).padStart(2, "0");

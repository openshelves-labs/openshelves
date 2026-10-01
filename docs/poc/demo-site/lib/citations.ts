import type { Book, Contributor } from "@/types";

export type CitationStyle = "apa" | "mla" | "chicago" | "bibtex";

export const CITATION_STYLES: { id: CitationStyle; label: string }[] = [
  { id: "apa", label: "APA 7th" },
  { id: "mla", label: "MLA 9th" },
  { id: "chicago", label: "Chicago 17th" },
  { id: "bibtex", label: "BibTeX" },
];

const creators = (b: Book): Contributor[] => {
  const c = b.contributors.filter((x) => x.role === "author" || x.role === "co-author");
  return c.length ? c : b.contributors;
};

const initials = (given: string) =>
  given
    .split(/[\s.]+/)
    .filter(Boolean)
    .map((p) => `${p[0].toUpperCase()}.`)
    .join(" ");

const stripDoiUrl = (doi: string) => doi.replace(/^https?:\/\/doi\.org\//, "");
const doiUrl = (doi?: string) => (doi ? ` https://doi.org/${stripDoiUrl(doi)}` : "");
const editionApa = (e?: string) => (e ? ` (${e.replace(/\s+Revised\s+/i, " ").replace(/Edition/i, "ed.")})` : "");

export function formatApa(b: Book): string {
  const a = creators(b).map((c) => `${c.family}, ${initials(c.given)}`);
  const names = a.length <= 2 ? a.join(", & ") : `${a.slice(0, -1).join(", ")}, & ${a[a.length - 1]}`;
  const title = b.subtitle ? `${b.title}: ${b.subtitle}` : b.title;
  return `${names} (${b.publicationYear}). ${title}${editionApa(b.edition)}. ${b.publisher}.${doiUrl(b.doi)}`;
}

export function formatMla(b: Book): string {
  const c = creators(b);
  const first = `${c[0].family}, ${c[0].given}`;
  const names = c.length === 1 ? first : c.length === 2 ? `${first}, and ${c[1].given} ${c[1].family}` : `${first}, et al`;
  const title = b.subtitle ? `${b.title}: ${b.subtitle}` : b.title;
  const ed = b.edition ? ` ${b.edition},` : "";
  return `${names}. ${title}.${ed} ${b.publisher}, ${b.publicationYear}.${b.doi ? ` doi:${stripDoiUrl(b.doi)}.` : ""}`;
}

export function formatChicago(b: Book): string {
  const c = creators(b);
  const first = `${c[0].family}, ${c[0].given}`;
  const rest = c.slice(1).map((x) => `${x.given} ${x.family}`);
  const names = rest.length ? `${first}, and ${rest.join(", ")}` : first;
  const title = b.subtitle ? `${b.title}: ${b.subtitle}` : b.title;
  return `${names}. ${title}.${b.edition ? ` ${b.edition}.` : ""} ${b.publisher}, ${b.publicationYear}.`;
}

export function formatBibtex(b: Book): string {
  const c = creators(b);
  const key = `${c[0].family.toLowerCase().replace(/[^a-z]/g, "")}${b.publicationYear}${b.title.split(/\s+/)[0].toLowerCase().replace(/[^a-z]/g, "")}`;
  const authors = c.map((x) => `${x.family}, ${x.given}`).join(" and ");
  const title = b.subtitle ? `${b.title}: ${b.subtitle}` : b.title;
  const lines = [
    `@book{${key},`,
    `  author    = {${authors}},`,
    `  title     = {${title}},`,
    `  publisher = {${b.publisher}},`,
    `  year      = {${b.publicationYear}},`,
  ];
  if (b.edition) lines.push(`  edition   = {${b.edition}},`);
  if (b.isbn) lines.push(`  isbn      = {${b.isbn}},`);
  if (b.doi) lines.push(`  doi       = {${stripDoiUrl(b.doi)}},`);
  lines.push("}");
  return lines.join("\n");
}

export function formatCitation(b: Book, style: CitationStyle): string {
  switch (style) {
    case "apa":
      return formatApa(b);
    case "mla":
      return formatMla(b);
    case "chicago":
      return formatChicago(b);
    case "bibtex":
      return formatBibtex(b);
  }
}

import type { AvailabilityStatus, Book, CatalogCardMeta, ResourceType } from "@/types";

/* ───────────── Facet definitions ───────────── */

export interface FacetDef {
  key: string;
  label: string;
  test: (b: Book) => boolean;
}

const has = (b: Book, re: RegExp) => re.test(`${b.title} ${b.subtitle ?? ""} ${b.subjects.join(" ")}`);

export const ACCESS_FACETS: FacetDef[] = [
  { key: "shelf", label: "On Shelf Now", test: (b) => b.availabilityStatus === "available" },
  { key: "online", label: "Available Online Full-text", test: (b) => b.availabilityStatus === "online" || b.availabilityStatus === "open_access" },
  { key: "oa", label: "Open Access (OA)", test: (b) => b.availabilityStatus === "open_access" },
  { key: "ill", label: "Inter-Library Loan Eligible", test: (b) => b.copies.length > 0 && b.availabilityStatus !== "available" && b.availabilityStatus !== "restricted" },
];

const inTypes = (...t: ResourceType[]) => (b: Book) => t.includes(b.resourceType);
export const TYPE_FACETS: FacetDef[] = [
  { key: "print", label: "Print Monographs", test: inTypes("monograph") },
  { key: "ebook", label: "E-Books & Folios", test: inTypes("ebook", "folio-edition", "digital-resource") },
  { key: "journal", label: "Scholarly Journals", test: inTypes("journal", "journal-article") },
  { key: "thesis", label: "Dissertations & Theses", test: inTypes("dissertation") },
  { key: "archival", label: "Archival Manuscripts", test: inTypes("archival", "manuscript-guide", "special-collection") },
];

export const SUBJECT_FACETS: FacetDef[] = [
  { key: "ecology", label: "Ecology", test: (b) => has(b, /ecolog|ecosystem/i) },
  { key: "environment", label: "Environmental Science", test: (b) => has(b, /environment|carbon|peat|toxic|urban|wetland/i) },
  { key: "climate", label: "Climate Adaptation", test: (b) => has(b, /climate/i) },
  { key: "conservation", label: "Conservation Biology", test: (b) => has(b, /conservation|biodiversity|restoration/i) },
  { key: "geo", label: "Geosciences", test: (b) => has(b, /geo|hydrolog|soil|wetland|peat/i) },
];

export interface CatalogFilters {
  access: string[];
  types: string[];
  subjects: string[];
  languages: string[];
  yearMin?: number;
  yearMax?: number;
}

export const EMPTY_FILTERS: CatalogFilters = { access: [], types: [], subjects: [], languages: [] };

export type SortKey = "relevance" | "newest" | "oldest" | "title" | "author";
export const SORT_OPTIONS: { key: SortKey; label: string }[] = [
  { key: "relevance", label: "Scholarly Relevance" },
  { key: "newest", label: "Newest First" },
  { key: "oldest", label: "Oldest First" },
  { key: "title", label: "Title (A–Z)" },
  { key: "author", label: "Author (A–Z)" },
];

export const PAGE_SIZES = [6, 24, 48] as const;

/* ───────────── Search ───────────── */

const haystack = (b: Book) =>
  [
    b.title,
    b.subtitle,
    b.contributors.map((c) => c.name).join(" "),
    b.subjects.join(" "),
    b.publisher,
    b.isbn,
    b.doi,
    b.callNumber,
    b.series,
    b.abstract,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

const tokens = (q: string) => q.toLowerCase().split(/\s+/).filter(Boolean);

/** 0 = no match; higher = better. Every token must appear somewhere in the record. */
export function searchScore(b: Book, q: string): number {
  const t = tokens(q);
  if (!t.length) return 1;
  const h = haystack(b);
  if (!t.every((x) => h.includes(x))) return 0;
  const title = `${b.title} ${b.subtitle ?? ""}`.toLowerCase();
  const subj = b.subjects.join(" ").toLowerCase();
  return t.reduce((s, x) => s + 1 + (title.includes(x) ? 5 : 0) + (subj.includes(x) ? 2 : 0), 0);
}

export const matchesQuery = (b: Book, q: string) => searchScore(b, q) > 0;

/* ───────────── Filtering / sorting ───────────── */

const anyOf = (defs: FacetDef[], keys: string[], b: Book) =>
  keys.length === 0 || defs.filter((d) => keys.includes(d.key)).some((d) => d.test(b));

export function applyFilters(books: Book[], f: CatalogFilters): Book[] {
  return books.filter(
    (b) =>
      anyOf(ACCESS_FACETS, f.access, b) &&
      anyOf(TYPE_FACETS, f.types, b) &&
      anyOf(SUBJECT_FACETS, f.subjects, b) &&
      (f.languages.length === 0 || f.languages.includes(b.language)) &&
      (f.yearMin == null || b.publicationYear >= f.yearMin) &&
      (f.yearMax == null || b.publicationYear <= f.yearMax),
  );
}

const authorKey = (b: Book) => b.contributors[0]?.family ?? "";

export function sortBooks(books: Book[], sort: SortKey, q: string): Book[] {
  const indexed = books.map((b, i) => ({ b, i, s: searchScore(b, q) }));
  const cmp: Record<SortKey, (x: (typeof indexed)[number], y: (typeof indexed)[number]) => number> = {
    relevance: (x, y) => y.s - x.s || x.i - y.i,
    newest: (x, y) => y.b.publicationYear - x.b.publicationYear || x.i - y.i,
    oldest: (x, y) => x.b.publicationYear - y.b.publicationYear || x.i - y.i,
    title: (x, y) => x.b.title.localeCompare(y.b.title),
    author: (x, y) => authorKey(x.b).localeCompare(authorKey(y.b)) || x.i - y.i,
  };
  return indexed.sort(cmp[sort]).map((x) => x.b);
}

export function facetCount(books: Book[], def: FacetDef): number {
  return books.filter(def.test).length;
}

export function languageFacets(books: Book[]): { label: string; count: number }[] {
  const m = new Map<string, number>();
  books.forEach((b) => m.set(b.language, (m.get(b.language) ?? 0) + 1));
  return [...m.entries()].map(([label, count]) => ({ label, count })).sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
}

export function yearBounds(books: Book[]): { min: number; max: number } {
  const ys = books.map((b) => b.publicationYear);
  return { min: Math.min(...ys), max: Math.max(...ys) };
}

/* ───────────── Pagination ───────────── */

export function pageWindow(page: number, total: number): (number | "…")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const set = new Set([1, total, page - 1, page, page + 1]);
  if (page <= 3) [2, 3, 4].forEach((n) => set.add(n));
  if (page >= total - 2) [total - 3, total - 2, total - 1].forEach((n) => set.add(n));
  const nums = [...set].filter((n) => n >= 1 && n <= total).sort((a, b) => a - b);
  const out: (number | "…")[] = [];
  nums.forEach((n, i) => {
    if (i > 0 && n - nums[i - 1] > 1) out.push("…");
    out.push(n);
  });
  return out;
}

/* ───────────── Card presentation ───────────── */

export const creatorNames = (b: Book): string[] => {
  const c = b.contributors.filter((x) => x.role === "author" || x.role === "co-author");
  return (c.length ? c : b.contributors).map((x) => x.name);
};

export const toneForStatus = (s: AvailabilityStatus): "ok" | "bad" | "warn" =>
  s === "available" || s === "online" || s === "open_access" ? "ok" : s === "checked_out" ? "bad" : "warn";

const coverToneFor = (b: Book): CatalogCardMeta["cover"]["tone"] =>
  b.coverTone === "charcoal" ? "tertiary" : b.coverTone === "sage" ? "secondary-container" : b.coverTone === "parchment" ? "secondary" : "primary";

/** Card presentation derived from a Book's own fields — used when a book has no `catalogCard` override. */
export function cardMeta(b: Book): CatalogCardMeta {
  if (b.catalogCard) return b.catalogCard;
  const first = b.copies[0];
  const status = b.availabilityStatus;
  const tags: CatalogCardMeta["tags"] = [];
  if (b.peerReviewed) tags.push({ label: "Peer Reviewed", tone: "muted" });
  if (b.formatLabel) tags.push({ label: b.formatLabel, tone: "secondary" });
  if (b.isNewRelease) tags.push({ label: `New Release ${b.publicationYear}`, tone: "plain" });
  if (b.edition && tags.length < 3) tags.push({ label: b.edition, tone: "secondary" });

  const family = b.contributors.filter((x) => x.role === "author" || x.role === "co-author").map((x) => x.family);
  const byline = family.length > 2 ? `${family[0]} et al.` : family.join(" & ") || (b.contributors[0]?.family ?? "");

  const queue = (b.holdsInQueue ?? 0) + 1;
  const actions: CatalogCardMeta["actions"] =
    status === "online"
      ? [
          { kind: "inert", label: "Read Full PDF", icon: "picture_as_pdf", tone: "primary" },
          { kind: "cite", label: "Cite" },
          { kind: "save", label: "Save to Workbench", icon: "dns" },
        ]
      : status === "open_access"
        ? [
            { kind: "inert", label: "Download EPUB / PDF", icon: "download_for_offline", tone: "primary" },
            { kind: "cite", label: "Cite" },
            { kind: "save", label: "Save to Workbench", icon: "dns" },
          ]
        : status === "checked_out"
          ? [
              { kind: "hold", label: `Place Hold (Waitlist #${queue})`, icon: "bookmark", tone: "tertiary" },
              { kind: "cite", label: "Cite" },
              { kind: "list", label: "Add to List", icon: "playlist_add" },
            ]
          : [
              { kind: "hold", label: status === "available" ? "Place Hold" : "Place Hold / Request", icon: "bookmark_add" },
              { kind: "cite", label: "Cite" },
              { kind: "list", label: "Add to List", icon: "playlist_add" },
            ];

  const online = status === "online" || status === "open_access";
  return {
    statusIcon: status === "online" ? "devices" : status === "open_access" ? "lock_open_right" : undefined,
    tags,
    cover: {
      kicker: b.publisher,
      title: b.title,
      byline,
      foot: `${b.formatLabel ?? b.publisher} • ${b.publicationYear}`,
      tone: coverToneFor(b),
    },
    coverRef: online && b.doi ? `DOI: ${b.doi}` : `LC: ${b.callNumber}`,
    note: {
      icon: online ? "check_circle" : status === "checked_out" ? "hourglass_top" : "location_on",
      title: first ? first.branchName : b.availabilityLabel,
      body: first ? `— ${first.location} (${first.callNumber})` : "",
      sub: b.availabilityNote,
    },
    actions,
  };
}

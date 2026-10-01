"use client";

import { useMemo, useState, type ReactNode } from "react";
import type { Book, HistoryEntry, HistoryFormat, ID } from "@/types";
import Icon from "@/components/Icon";
import { toDate } from "@/lib/dates";
import HistoryEntryCard from "./HistoryEntryCard";

type FormatFilter = "all" | HistoryFormat;
type Sort = "recent" | "oldest" | "title";

const CHIPS: { key: FormatFilter; label: string }[] = [
  { key: "all", label: "All Formats" },
  { key: "physical", label: "Physical Borrowings" },
  { key: "digital", label: "Digital Accessions" },
  { key: "ill", label: "Inter-Library Loans" },
];
const SORTS: { key: Sort; label: string }[] = [
  { key: "recent", label: "Most Recently Returned" },
  { key: "oldest", label: "Oldest First" },
  { key: "title", label: "Title (A–Z)" },
];
const DOTS = ["bg-primary", "bg-secondary", "bg-outline"];
const PAGE_SIZE = 5;

const monthKey = (iso: string) => iso.slice(0, 7);
const monthLabel = (iso: string) => toDate(iso).toLocaleDateString("en-US", { month: "long", year: "numeric", timeZone: "UTC" });

interface Props {
  entries: HistoryEntry[];
  books: Book[];
  year: number;
  /** right-hand summary column (server-rendered) */
  aside?: ReactNode;
}

export default function HistoryView({ entries, books, year, aside }: Props) {
  const [format, setFormat] = useState<FormatFilter>("all");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<Sort>("recent");
  const [page, setPage] = useState(1);

  const bookById = useMemo(() => new Map<ID, Book>(books.map((b) => [b.id, b])), [books]);

  const counts = useMemo(
    () => ({
      all: entries.length,
      physical: entries.filter((e) => e.format === "physical").length,
      digital: entries.filter((e) => e.format === "digital").length,
      ill: entries.filter((e) => e.format === "ill").length,
    }),
    [entries],
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = entries.filter((e) => {
      const b = bookById.get(e.bookId);
      if (!b) return false;
      if (format !== "all" && e.format !== format) return false;
      if (!q) return true;
      const hay = [b.title, b.callNumber, e.branchNote, ...b.contributors.map((c) => c.name), ...b.subjects].join(" ").toLowerCase();
      return hay.includes(q);
    });
    const title = (e: HistoryEntry) => bookById.get(e.bookId)?.title ?? "";
    return list.sort((a, b) =>
      sort === "recent" ? b.returnedOn.localeCompare(a.returnedOn) : sort === "oldest" ? a.returnedOn.localeCompare(b.returnedOn) : title(a).localeCompare(title(b)),
    );
  }, [entries, bookById, format, query, sort]);

  const pages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, pages);
  const visible = filtered.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE);

  // month groups in order of first appearance; header count is the group's size within the whole filtered set
  const groups = useMemo(() => {
    const map = new Map<string, { label: string; all: number; items: HistoryEntry[] }>();
    for (const e of filtered) {
      const k = monthKey(e.returnedOn);
      if (!map.has(k)) map.set(k, { label: monthLabel(e.returnedOn), all: 0, items: [] });
      map.get(k)!.all += 1;
    }
    for (const e of visible) map.get(monthKey(e.returnedOn))!.items.push(e);
    return [...map.values()].filter((g) => g.items.length > 0);
  }, [filtered, visible]);

  const reset = <T,>(fn: (v: T) => void) => (v: T) => {
    fn(v);
    setPage(1);
  };

  return (
    <>
      {/* SEARCH & CONTROLS LEDGER BAR */}
      <div className="mt-7 p-4 rounded-xl bg-surface-container-lowest shadow-sm flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4">
        <div className="relative flex-1 min-w-[180px]">
          <Icon name="search" className="absolute left-3.5 top-1/2 -translate-y-1/2 text-outline text-[20px] pointer-events-none" />
          <input
            type="search"
            value={query}
            onChange={(e) => reset(setQuery)(e.target.value)}
            placeholder="Search history by title, author, call number, or keywords…"
            aria-label="Search reading history"
            className="w-full h-11 pl-10 pr-4 bg-surface-container-low rounded-lg font-body-md text-sm text-on-surface placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary transition-all"
          />
        </div>
        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <button type="button" className="h-11 px-3.5 rounded-lg bg-surface-container-low text-on-surface hover:bg-surface-container flex items-center gap-2 font-label-md transition-colors">
            <Icon name="calendar_today" className="text-outline text-[18px]" />
            <span className="font-medium text-sm">Calendar Year: {year} (Jan - Present)</span>
            <Icon name="expand_more" className="text-outline text-[18px]" />
          </button>
          <label className="relative h-11 px-3.5 rounded-lg bg-surface-container-low text-on-surface hover:bg-surface-container flex items-center gap-2 font-label-md text-sm transition-colors cursor-pointer">
            <Icon name="sort" className="text-outline text-[18px]" />
            <span>Sort:</span>
            <select
              value={sort}
              onChange={(e) => reset(setSort)(e.target.value as Sort)}
              aria-label="Sort history"
              className="appearance-none bg-transparent pr-6 font-semibold text-primary text-sm cursor-pointer focus:outline-none"
            >
              {SORTS.map((s) => (
                <option key={s.key} value={s.key}>
                  {s.label}
                </option>
              ))}
            </select>
            <Icon name="expand_more" className="pointer-events-none absolute right-3 text-outline text-[18px]" />
          </label>
        </div>
      </div>

      {/* Format quick filters */}
      <div className="flex flex-wrap items-center gap-2 mt-3 font-label-md text-sm">
        {CHIPS.map((c) => {
          const on = format === c.key;
          return (
            <button
              key={c.key}
              type="button"
              aria-pressed={on}
              onClick={() => reset(setFormat)(c.key)}
              className={`px-3.5 py-1.5 rounded-full transition-all ${
                on ? "bg-[#E4EEE8] text-primary font-semibold" : "bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container shadow-sm"
              }`}
            >
              {c.label} ({counts[c.key]})
            </button>
          );
        })}
      </div>

      {/* Two-column spread: timeline + summary */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-7 items-start">
      <div className="lg:col-span-8 flex flex-col space-y-9">
        {groups.map((g, i) => (
          <section key={g.label} className="space-y-4">
            <div className="flex items-center justify-between pb-2">
              <div className="flex items-center gap-3">
                <span className={`w-2.5 h-2.5 rounded-full ${DOTS[i % DOTS.length]}`} />
                <h2 className="font-title-editorial text-on-surface">{g.label}</h2>
              </div>
              <span className="px-2.5 py-1 rounded bg-surface-container text-on-surface-variant font-metadata-caps tracking-wide font-semibold">
                {g.all} {g.all === 1 ? "Work" : "Works"} Returned
              </span>
            </div>
            {g.items.map((e) => {
              const b = bookById.get(e.bookId);
              return b ? <HistoryEntryCard key={e.id} entry={e} book={b} /> : null;
            })}
          </section>
        ))}

        {filtered.length === 0 && (
          <div className="p-10 rounded-xl bg-surface-container-lowest shadow-sm text-center text-sm text-on-surface-variant">No records match your search.</div>
        )}

        {/* Pagination / ledger footer */}
        <div className="p-4 rounded-xl bg-surface-container-lowest flex items-center justify-between text-sm text-on-surface-variant shadow-sm">
          <span>
            Showing <strong>{visible.length}</strong> of <strong>{filtered.length}</strong> recorded accessions in {year}
          </span>
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled={current === 1}
              onClick={() => setPage(current - 1)}
              className="px-3 py-1 rounded bg-surface-container-low text-on-surface-variant font-label-md hover:bg-surface-container transition-colors disabled:opacity-50"
            >
              Previous
            </button>
            {Array.from({ length: pages }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                aria-current={n === current ? "page" : undefined}
                onClick={() => setPage(n)}
                className={`w-8 h-8 rounded font-label-md flex items-center justify-center ${n === current ? "bg-[#E4EEE8] text-primary font-semibold" : "hover:bg-surface-container text-on-surface"}`}
              >
                {n}
              </button>
            ))}
            <button
              type="button"
              disabled={current === pages}
              onClick={() => setPage(current + 1)}
              className="px-3 py-1 rounded bg-surface-container-low text-on-surface hover:bg-surface-container font-label-md transition-colors disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      </div>
      {aside}
      </div>
    </>
  );
}

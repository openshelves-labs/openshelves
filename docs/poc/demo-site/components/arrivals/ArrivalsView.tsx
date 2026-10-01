"use client";

import { useMemo, useState } from "react";
import type { Book } from "@/types";
import Icon from "@/components/Icon";
import { NOW, daysUntil } from "@/lib/dates";
import ArrivalTile from "./ArrivalTile";
import ArrivalRow from "./ArrivalRow";
import { FORMAT_LABEL, formatKey, type FormatKey } from "./arrivalUtils";

type Period = "week" | "month" | "quarter" | "all";
type Sort = "accession" | "year" | "call" | "title";
type View = "grid" | "list";

const PERIODS: { key: Period; label: string; days: number }[] = [
  { key: "week", label: "This week", days: 7 },
  { key: "month", label: "This month", days: 30 },
  { key: "quarter", label: "Past 90 days", days: 90 },
  { key: "all", label: "All recent", days: Infinity },
];
const SORTS: { key: Sort; label: string }[] = [
  { key: "accession", label: "Sort: Accession Date (Newest)" },
  { key: "year", label: "Sort: Publication Year (Desc)" },
  { key: "call", label: "Sort: Shelfmark / Call Number" },
  { key: "title", label: "Sort: Title (A–Z)" },
];
const PAGE_SIZE = 10;
const SELECT =
  "w-full h-10 pl-3 pr-8 bg-surface-container-low text-on-surface rounded-lg font-label-md text-sm focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary focus:outline-none appearance-none cursor-pointer";

function Select({ value, onChange, label, children }: { value: string; onChange: (v: string) => void; label: string; children: React.ReactNode }) {
  return (
    <div className="relative">
      <select value={value} onChange={(e) => onChange(e.target.value)} aria-label={label} className={SELECT}>
        {children}
      </select>
      <Icon name="expand_more" className="pointer-events-none absolute right-2.5 top-2.5 text-outline text-[18px]" />
    </div>
  );
}

export default function ArrivalsView({ books }: { books: Book[] }) {
  const [period, setPeriod] = useState<Period>("week");
  const [discipline, setDiscipline] = useState("");
  const [format, setFormat] = useState("");
  const [library, setLibrary] = useState("");
  const [sort, setSort] = useState<Sort>("accession");
  const [view, setView] = useState<View>("grid");

  const disciplines = useMemo(() => [...new Set(books.map((b) => b.discipline).filter((d): d is string => !!d))].sort(), [books]);
  const formats = useMemo(() => [...new Set(books.map(formatKey))], [books]);
  const libraries = useMemo(() => {
    const m = new Map<string, string>();
    books.forEach((b) => b.copies.forEach((c) => m.set(c.branchId, c.branchName)));
    return [...m.entries()];
  }, [books]);

  const filtered = useMemo(() => {
    const days = PERIODS.find((p) => p.key === period)!.days;
    const list = books.filter((b) => {
      if (b.accessionDate && -daysUntil(b.accessionDate, NOW) > days) return false;
      if (discipline && b.discipline !== discipline) return false;
      if (format && formatKey(b) !== format) return false;
      if (library && !b.copies.some((c) => c.branchId === library)) return false;
      return true;
    });
    return list.sort((a, b) => {
      switch (sort) {
        case "year":
          return b.publicationYear - a.publicationYear || (b.accessionDate ?? "").localeCompare(a.accessionDate ?? "");
        case "call":
          return a.callNumber.localeCompare(b.callNumber);
        case "title":
          return a.title.localeCompare(b.title);
        default:
          return (b.accessionDate ?? "").localeCompare(a.accessionDate ?? "");
      }
    });
  }, [books, period, discipline, format, library, sort]);

  const shown = filtered.slice(0, PAGE_SIZE);

  return (
    <>
      {/* CONTROL & FILTER REGISTERS */}
      <div className="mt-6 flex flex-col gap-4 bg-surface-container-lowest p-5 rounded-xl shadow-sm border border-border-warm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-1.5 p-1 bg-surface-container rounded-lg" role="tablist" aria-label="Accession window">
            {PERIODS.map((p) => {
              const on = period === p.key;
              return (
                <button
                  key={p.key}
                  type="button"
                  role="tab"
                  aria-selected={on}
                  onClick={() => setPeriod(p.key)}
                  className={`px-3.5 py-1.5 rounded-md font-label-md text-sm transition-all ${
                    on ? "bg-surface-container-lowest text-primary font-semibold" : "text-on-surface-variant hover:text-on-surface"
                  }`}
                >
                  {p.label}
                </button>
              );
            })}
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center bg-surface-container p-1 rounded-lg">
              {([
                { key: "grid", label: "Grid View", icon: "grid_view" },
                { key: "list", label: "List Ledger View", icon: "format_list_bulleted" },
              ] as const).map((v) => (
                <button
                  key={v.key}
                  type="button"
                  aria-label={v.label}
                  aria-pressed={view === v.key}
                  onClick={() => setView(v.key)}
                  className={`p-1.5 rounded-md transition-colors ${view === v.key ? "bg-surface-container-lowest text-primary" : "text-on-surface-variant hover:text-on-surface"}`}
                >
                  <Icon name={v.icon} className="text-[18px]" />
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2 text-on-surface-variant font-label-md text-sm">
              <span className="font-metadata-caps text-outline uppercase">Showing</span>
              <span className="font-semibold text-on-surface">
                {shown.length ? `1–${shown.length}` : "0"}
              </span>
              <span>of {filtered.length}</span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 pt-2">
          <Select value={discipline} onChange={setDiscipline} label="Discipline">
            <option value="">All Disciplines (Ecology, Law, Ethics)</option>
            {disciplines.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </Select>
          <Select value={format} onChange={setFormat} label="Format">
            <option value="">All Formats (Print, Digital, Folio)</option>
            {formats.map((f: FormatKey) => (
              <option key={f} value={f}>
                {FORMAT_LABEL[f]}
              </option>
            ))}
          </Select>
          <Select value={library} onChange={setLibrary} label="Library branch">
            <option value="">All Cambridge Libraries</option>
            {libraries.map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </Select>
          <Select value={sort} onChange={(v) => setSort(v as Sort)} label="Sort order">
            {SORTS.map((s) => (
              <option key={s.key} value={s.key}>
                {s.label}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {/* REGISTER */}
      <section className="mt-8 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-metadata-caps uppercase text-outline">Current Register</span>
            <span className="text-outline">/</span>
            <span className="font-label-md font-semibold text-on-surface">Cambridge Central Repository Catalog</span>
          </div>
          <span className="font-metadata-caps text-secondary font-medium">Batch ID: CAM-ACQ-2025-04</span>
        </div>

        {shown.length === 0 ? (
          <div className="p-10 rounded-xl bg-surface-container-lowest shadow-sm text-center text-sm text-on-surface-variant">No accessions match these filters.</div>
        ) : view === "grid" ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-5">
            {shown.map((b) => (
              <ArrivalTile key={b.id} book={b} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {shown.map((b) => (
              <ArrivalRow key={b.id} book={b} />
            ))}
          </div>
        )}
      </section>
    </>
  );
}

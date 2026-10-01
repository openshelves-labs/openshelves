"use client";

import { useMemo, useState } from "react";
import type { LedgerEntry } from "@/lib/api";
import type { ListsOverview } from "@/mock/lists";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import Breadcrumbs from "@/components/Breadcrumbs";
import FolioCard from "./FolioCard";
import CreateListTile from "./CreateListTile";
import LedgerSection from "./LedgerSection";
import { FOLIO_FILTERS, type FolioFilter } from "./listFilters";

type SortKey = "recent" | "title";

export default function ListsIndexView({ overview, ledger }: { overview: ListsOverview; ledger: LedgerEntry[] }) {
  const { lists, getBook, openNewList } = useLibrary();
  const [filter, setFilter] = useState<FolioFilter>("all");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<SortKey>("recent");
  const [view, setView] = useState<"grid" | "table">("grid");

  const counts = useMemo(
    () => Object.fromEntries(FOLIO_FILTERS.map((f) => [f.id, lists.filter(f.test).length])) as Record<FolioFilter, number>,
    [lists],
  );

  const visible = useMemo(() => {
    const test = FOLIO_FILTERS.find((f) => f.id === filter)!.test;
    const q = query.trim().toLowerCase();
    const rows = lists.filter(
      (l) =>
        test(l) &&
        (!q || [l.title, l.description, l.category, ...l.tags].some((s) => s.toLowerCase().includes(q))),
    );
    if (sort === "title") return [...rows].sort((a, b) => a.title.localeCompare(b.title));
    const rank = (l: (typeof rows)[number]) => (l.updatedLabel.includes("just now") ? 0 : 1);
    return [...rows].sort((a, b) => rank(a) - rank(b));
  }, [lists, filter, query, sort]);

  const totalItems = lists.reduce((n, l) => n + l.itemCount, 0);
  const totalPdfs = lists.reduce((n, l) => n + (l.pdfCount ?? 0), 0);
  const courseN = lists.filter((l) => l.categoryKind === "course" || l.categoryKind === "study").length;
  const researchN = lists.filter((l) => l.categoryKind === "grant" || l.categoryKind === "shared").length;
  const personalN = lists.filter((l) => l.categoryKind === "archive").length;
  const sharedN = counts.shared;

  return (
    <main className="mx-auto w-full max-w-[1600px] space-y-9 px-8 py-8">
      <div className="flex w-full flex-col space-y-8">
        <section className="flex flex-col gap-5">
          <Breadcrumbs homeIcon items={[{ label: "Home", href: "/" }, { label: "My Library" }, { label: "Reading Lists & Bibliographic Folios" }]} />
          <div className="flex flex-col justify-between gap-6 lg:flex-row lg:items-end">
            <div className="max-w-2xl space-y-2">
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-secondary-fixed/50 px-2.5 py-0.5 font-metadata-caps uppercase tracking-widest text-secondary">Scholarly Repository</span>
                <span className="text-xs text-outline">•</span>
                <span className="font-metadata-caps uppercase tracking-wider text-outline">Cambridge Library System</span>
              </div>
              <h1 className="font-headline-lg tracking-tight text-on-surface">Curated Reading Lists &amp; Folios</h1>
              <p className="font-body-md leading-relaxed text-on-surface-variant">
                Curate syllabi, organize literature for research grants, and manage bibliographic folios across courses and collaborative laboratory projects.
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-3">
              <button type="button" className="flex h-10 items-center gap-2 rounded-lg bg-surface-container-lowest px-4 font-label-lg shadow-sm transition-colors hover:bg-surface-container">
                <Icon name="file_download" className="text-[18px] text-secondary" />
                <span>Export All (BibTeX/RIS)</span>
              </button>
              <button type="button" onClick={openNewList} className="flex h-10 items-center gap-2 rounded-lg bg-primary px-5 font-label-lg text-on-primary shadow-md transition-colors hover:bg-primary-container">
                <Icon name="add" className="text-[18px]" />
                <span>Create New List</span>
              </button>
            </div>
          </div>
        </section>

        <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Metric label="Total Curations" value={`${lists.length} Folios`} icon="auto_stories">
            <span>{courseN} Course</span>
            <span className="text-outline">•</span>
            <span>{researchN} Research</span>
            <span className="text-outline">•</span>
            <span>{personalN} Personal</span>
          </Metric>
          <Metric label="Catalog Works" value={`${totalItems} Items`} icon="menu_book">
            <Icon name="picture_as_pdf" className="text-sm text-primary" />
            <span className="truncate">{totalPdfs} PDFs Attached with DOIs</span>
          </Metric>
          <Metric label="Saved Citations" value={`${overview.savedCitations} Formatted`} icon="format_quote">
            <span className="rounded bg-[#c6ebd3] px-1.5 py-0.5 font-metadata-caps text-[10px] font-bold text-[#2F5D45]">APA</span>
            <span className="rounded bg-[#eae8e3] px-1.5 py-0.5 font-metadata-caps text-[10px] font-bold text-on-surface-variant">MLA</span>
            <span className="rounded bg-[#eae8e3] px-1.5 py-0.5 font-metadata-caps text-[10px] font-bold text-on-surface-variant">BibTeX</span>
          </Metric>
          <Metric label="Network Engagement" value={`${sharedN} Shared`} icon="group">
            <span className="h-2 w-2 shrink-0 rounded-full bg-primary" />
            <span className="truncate">{overview.activeScholars} Active Students &amp; Fellows</span>
          </Metric>
        </section>

        <section className="flex flex-col items-stretch justify-between gap-4 rounded-xl border border-[#E5E0D8] bg-white p-3.5 lg:flex-row lg:items-center">
          <div className="relative max-w-md flex-1">
            <Icon name="search" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-outline" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Filter lists by title, course code, tag, or topic…"
              className="h-10 w-full rounded-lg border border-[#E5E0D8] bg-surface-container-low pl-10 pr-4 font-body-md text-sm text-on-surface transition-all placeholder:text-outline focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0">
            {FOLIO_FILTERS.map((f) => (
              <button
                key={f.id}
                type="button"
                aria-pressed={filter === f.id}
                onClick={() => setFilter(f.id)}
                className={`shrink-0 whitespace-nowrap rounded-full px-3.5 py-1.5 font-label-md text-xs transition-colors ${
                  filter === f.id
                    ? "border border-primary/20 bg-[#c6ebd3] font-semibold text-[#2F5D45]"
                    : "border border-[#E5E0D8] bg-white text-on-surface-variant hover:bg-stone-50 hover:text-on-surface"
                }`}
              >
                {f.label} ({counts[f.id]})
              </button>
            ))}
          </div>
          <div className="flex shrink-0 items-center gap-2 self-end lg:self-auto">
            <div className="flex items-center rounded-lg border border-[#E5E0D8] bg-surface-container-low p-0.5">
              {([
                ["grid", "grid_view", "Grid View"],
                ["table", "view_list", "Table View"],
              ] as const).map(([k, icon, title]) => (
                <button
                  key={k}
                  type="button"
                  title={title}
                  onClick={() => setView(k)}
                  className={`rounded-md p-1.5 ${view === k ? "bg-white text-primary" : "text-on-surface-variant transition-colors hover:text-on-surface"}`}
                >
                  <Icon name={icon} className="text-[18px]" />
                </button>
              ))}
            </div>
            <div className="relative">
              <select
                aria-label="Sort folios"
                value={sort}
                onChange={(e) => setSort(e.target.value as SortKey)}
                className="h-9 cursor-pointer appearance-none rounded-lg border border-[#E5E0D8] bg-white pl-3 pr-8 font-label-md text-xs text-on-surface transition-colors hover:bg-stone-50 focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="recent">Recently Updated</option>
                <option value="title">Title (A–Z)</option>
              </select>
              <Icon name="expand_more" className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-[16px] text-outline" />
            </div>
          </div>
        </section>

        <section className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {visible.map((l) => (
            <FolioCard key={l.id} list={l} bookOf={getBook} />
          ))}
          {visible.length === 0 && (
            <p className="col-span-full rounded-xl border border-dashed border-[#E5E0D8] bg-white p-8 text-center font-body-sm text-sm text-on-surface-variant">
              No folios match your filters.
            </p>
          )}
          <CreateListTile />
        </section>

        <LedgerSection entries={ledger} />
      </div>
    </main>
  );
}

function Metric({ label, value, icon, children }: { label: string; value: string; icon: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col justify-between rounded-xl border border-[#E5E0D8] bg-white p-4 transition-all hover:border-primary/40">
      <div className="flex items-start justify-between">
        <div>
          <span className="block font-metadata-caps text-[11px] font-medium uppercase tracking-wider text-outline">{label}</span>
          <div className="mt-1 font-title-editorial text-2xl font-semibold text-on-surface">{value}</div>
        </div>
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#c6ebd3]/60 text-primary">
          <Icon name={icon} className="text-xl" />
        </div>
      </div>
      <div className="mt-3 flex items-center gap-1.5 rounded-lg border border-[#E5E0D8]/60 bg-surface-container-low px-2.5 py-1.5 pt-2.5 font-body-sm text-xs text-on-surface-variant">{children}</div>
    </div>
  );
}

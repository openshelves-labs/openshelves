"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { Book } from "@/types";
import Icon from "@/components/Icon";
import {
  ACCESS_FACETS,
  EMPTY_FILTERS,
  PAGE_SIZES,
  SUBJECT_FACETS,
  TYPE_FACETS,
  applyFilters,
  languageFacets,
  matchesQuery,
  sortBooks,
  yearBounds,
  type CatalogFilters,
  type SortKey,
} from "@/lib/catalog";
import CatalogResultCard from "./CatalogResultCard";
import FacetSidebar from "./FacetSidebar";
import Pagination from "./Pagination";
import SearchPanel, { type RefinementChip } from "./SearchPanel";

type Group = "access" | "types" | "subjects" | "languages";

const VIEWS = [
  { id: "grid", label: "Grid", icon: "grid_view" },
  { id: "list", label: "Detailed List", icon: "view_agenda" },
  { id: "matrix", label: "Citation Matrix", icon: "account_tree" },
] as const;

export default function CatalogView({ books }: { books: Book[] }) {
  const router = useRouter();
  const params = useSearchParams();
  const query = (params.get("q") ?? "").trim();

  const [draft, setDraft] = useState(query);
  const [filters, setFilters] = useState<CatalogFilters>(EMPTY_FILTERS);
  const [sort, setSort] = useState<SortKey>("relevance");
  const [perPage, setPerPage] = useState<number>(PAGE_SIZES[0]);
  const [page, setPage] = useState(1);
  const [view, setView] = useState<(typeof VIEWS)[number]["id"]>("grid");
  const [selectAll, setSelectAll] = useState(false);

  // keep the in-page search ledger in sync with ?q= (top bar pushes /catalog?q=…)
  useEffect(() => {
    setDraft(query);
    setPage(1);
  }, [query]);

  const bounds = useMemo(() => yearBounds(books), [books]);
  const pool = useMemo(() => books.filter((b) => matchesQuery(b, query)), [books, query]);
  const languages = useMemo(() => languageFacets(pool), [pool]);
  const results = useMemo(() => sortBooks(applyFilters(pool, filters), sort, query), [pool, filters, sort, query]);

  const totalPages = Math.max(1, Math.ceil(results.length / perPage));
  const current = Math.min(page, totalPages);
  const start = (current - 1) * perPage;
  const visible = results.slice(start, start + perPage);

  const update = (f: (prev: CatalogFilters) => CatalogFilters) => {
    setFilters(f);
    setPage(1);
  };
  const toggle = (group: Group, key: string) =>
    update((f) => ({ ...f, [group]: f[group].includes(key) ? f[group].filter((k) => k !== key) : [...f[group], key] }));

  const applyQuery = (q: string) => {
    const t = q.trim();
    router.replace(t ? `/catalog?q=${encodeURIComponent(t)}` : "/catalog", { scroll: false });
  };

  const chips: RefinementChip[] = [
    ...ACCESS_FACETS.filter((d) => filters.access.includes(d.key)).map((d) => ({ group: "access" as const, key: d.key, label: d.label, icon: d.key === "oa" ? "lock_open" : "inventory_2" })),
    ...TYPE_FACETS.filter((d) => filters.types.includes(d.key)).map((d) => ({ group: "types" as const, key: d.key, label: d.label, icon: "category" })),
    ...SUBJECT_FACETS.filter((d) => filters.subjects.includes(d.key)).map((d) => ({ group: "subjects" as const, key: d.key, label: d.label, icon: "label" })),
    ...filters.languages.map((l) => ({ group: "languages" as const, key: l, label: l, icon: "translate" })),
  ];

  const clearAll = () => {
    update(() => EMPTY_FILTERS);
    setSort("relevance");
    if (query) applyQuery("");
  };

  return (
    <div className="flex flex-col w-full">
      <section className="mb-space-lg">
        <div className="flex flex-wrap items-center justify-between gap-space-sm mb-space-sm">
          <div className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
            <span className="hover:text-primary transition-colors cursor-pointer">Catalog Archives</span>
            <span className="text-outline-variant font-metadata-caps">/</span>
            <span className="text-on-surface font-semibold">Discovery Index</span>
            <span className="text-outline-variant font-metadata-caps">/</span>
            <span className="font-metadata-caps text-metadata-caps uppercase text-secondary font-semibold bg-secondary-fixed/50 px-2 py-0.5 rounded-full">Biological &amp; Earth Sciences</span>
          </div>
          <div className="flex items-center gap-space-sm text-outline font-label-md text-label-md">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-primary inline-block" /> Integrated Library System
            </span>
            <span className="text-outline-variant">•</span>
            <span>Index Sync: 14m ago</span>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row lg:items-baseline justify-between gap-space-md pb-space-md">
          <div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight font-medium">University Catalog &amp; Discovery Index</h1>
            <p className="font-body-md text-body-md text-on-surface-variant mt-1">
              Showing <span className="font-semibold text-on-surface font-label-lg">{results.length.toLocaleString("en-US")}</span> peer-reviewed titles, monographs, and rare holdings across all Cambridge University repositories.
            </p>
          </div>
          <div className="flex items-center gap-1.5 p-1 bg-surface-container rounded-xl self-start lg:self-auto">
            {VIEWS.map((v) => (
              <button
                key={v.id}
                type="button"
                aria-pressed={view === v.id}
                onClick={() => setView(v.id)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-label-lg text-label-md transition-all ${
                  view === v.id
                    ? "bg-surface-container-lowest text-primary shadow-sm font-semibold"
                    : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high"
                }`}
              >
                <Icon name={v.icon} className="text-base" />
                <span>{v.label}</span>
              </button>
            ))}
          </div>
        </div>

        <SearchPanel
          draft={draft}
          onDraft={setDraft}
          onApply={() => applyQuery(draft)}
          onClearQuery={() => {
            setDraft("");
            applyQuery("");
          }}
          chips={chips}
          onRemoveChip={(c) => toggle(c.group, c.key)}
          bounds={bounds}
          yearMin={filters.yearMin}
          yearMax={filters.yearMax}
          onYear={(min, max) => update((f) => ({ ...f, yearMin: min, yearMax: max }))}
          sort={sort}
          onSort={(s) => {
            setSort(s);
            setPage(1);
          }}
        />
      </section>

      <div className="grid grid-cols-12 gap-space-lg items-start">
        <FacetSidebar filters={filters} pool={pool} languages={languages} onToggle={toggle} onClear={clearAll} />

        <div className="col-span-9 order-first space-y-space-md">
          <div className="flex items-center justify-between px-space-xs py-1">
            <div className="flex items-center gap-space-sm">
              <label className="inline-flex items-center gap-2 text-body-sm font-body-sm text-on-surface-variant cursor-pointer">
                <input type="checkbox" checked={selectAll} onChange={(e) => setSelectAll(e.target.checked)} className="w-4 h-4 rounded text-primary focus:ring-0 accent-primary" />
                <span>Select all on page</span>
              </label>
              <span className="text-outline-variant font-metadata-caps">•</span>
              <span className="font-metadata-caps text-metadata-caps text-outline uppercase tracking-wider">Indexed By Dewey &amp; Library of Congress</span>
            </div>
            <div className="hidden sm:flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
              <span>Results per page:</span>
              {PAGE_SIZES.map((n) => (
                <button
                  key={n}
                  type="button"
                  aria-pressed={perPage === n}
                  onClick={() => {
                    setPerPage(n);
                    setPage(1);
                  }}
                  className={
                    perPage === n
                      ? "font-semibold text-on-surface px-2 py-0.5 rounded bg-surface-container"
                      : "px-2 py-0.5 rounded hover:bg-surface-container cursor-pointer transition-colors"
                  }
                >
                  {n}
                </button>
              ))}
            </div>
          </div>

          {visible.length ? (
            visible.map((b) => <CatalogResultCard key={b.id} book={b} />)
          ) : (
            <div className="bg-surface-container-lowest rounded-xl p-space-xl shadow-sm text-center">
              <Icon name="search_off" className="text-3xl text-outline" />
              <p className="font-headline-sm text-headline-sm text-on-surface mt-2">No catalog matches</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">Try a broader search or remove some refinements.</p>
              <button type="button" onClick={clearAll} className="mt-3 h-9 px-4 rounded-lg bg-primary text-on-primary font-label-lg text-label-md font-semibold hover:bg-primary-container transition-colors">
                Clear all
              </button>
            </div>
          )}

          <Pagination
            page={current}
            totalPages={totalPages}
            from={start + 1}
            to={start + visible.length}
            total={results.length}
            onPage={(p) => {
              setPage(p);
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
          />
        </div>
      </div>
    </div>
  );
}

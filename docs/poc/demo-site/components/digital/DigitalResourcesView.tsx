"use client";

import { useMemo, useState } from "react";
import Icon from "@/components/Icon";
import type { DigitalResource, DigitalResourceTab, DigitalResourceType } from "@/types";
import ResourceCard from "./ResourceCard";
import Pagination from "./Pagination";
import AccessSidebar from "./AccessSidebar";

const PAGE_SIZE = 6;
const FILTERS = ["All Access", "Institutional SSO", "Open Access", "Peer-Reviewed", "API / Raw Data"] as const;
type AccessFilter = (typeof FILTERS)[number];
const SORTS = ["Curated", "A–Z", "Recent"] as const;
type Sort = (typeof SORTS)[number];

const matchesFilter = (r: DigitalResource, f: AccessFilter) =>
  f === "All Access" ||
  (f === "Institutional SSO" && r.access === "sso") ||
  (f === "Open Access" && r.access === "open") ||
  (f === "Peer-Reviewed" && r.peerReviewed) ||
  (f === "API / Raw Data" && r.apiAccess);

const LAUNCH_LABEL: Record<DigitalResourceType, string> = {
  ebooks: "Launch Collection",
  databases: "Launch Database",
  theses: "Launch Repository",
  datasets: "Launch Dataset",
};

export default function DigitalResourcesView({
  resources,
  tabs,
}: {
  resources: DigitalResource[];
  tabs: DigitalResourceTab[];
}) {
  const [type, setType] = useState<DigitalResourceType>("databases");
  const [filter, setFilter] = useState<AccessFilter>("All Access");
  const [sort, setSort] = useState<Sort>("Curated");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [saved, setSaved] = useState<Set<string>>(new Set());

  const tab = tabs.find((t) => t.type === type) ?? tabs[0];

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = resources.filter(
      (r) =>
        r.type === type &&
        matchesFilter(r, filter) &&
        (!q || [r.title, r.vendor, r.collection, ...r.subjects].some((s) => s.toLowerCase().includes(q))),
    );
    if (sort === "A–Z") list.sort((a, b) => a.title.localeCompare(b.title));
    if (sort === "Recent") list.sort((a, b) => b.addedOn.localeCompare(a.addedOn));
    return list;
  }, [resources, type, filter, sort, query]);

  const narrowed = filter !== "All Access" || query.trim() !== "";
  const total = narrowed ? filtered.length : Math.max(tab.total, filtered.length);
  const last = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const current = Math.min(page, last);
  // Mock data holds one page of entries per tab; further pages are visual only.
  const shown = filtered.slice(0, PAGE_SIZE);

  const reset = () => setPage(1);
  const toggleSave = (id: string) =>
    setSaved((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  return (
    <div className="flex flex-col w-full space-y-8">
      <header className="relative rounded-2xl bg-surface-container-low p-8 shadow-sm overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-secondary-container/30 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
        <div className="relative z-10 flex flex-col space-y-4">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-primary inline-block" />
            <span className="font-metadata-caps uppercase text-primary tracking-wider font-semibold">
              Central University Libraries • Electronic Resource Gateway
            </span>
          </div>
          <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6">
            <div className="max-w-3xl space-y-2">
              <h1 className="font-display-lg text-on-surface tracking-tight">Digital Resources &amp; Repositories</h1>
              <p className="font-body-lg text-on-surface-variant max-w-2xl leading-relaxed">
                Curated academic databases, electronic books, institutional theses, and open scientific datasets licensed for university scholars and research affiliates.
              </p>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              <div className="bg-surface-container-lowest px-4 py-2.5 rounded-xl shadow-sm flex items-center gap-3">
                <Icon name="vpn_lock" className="text-primary text-xl" />
                <div className="flex flex-col">
                  <span className="font-label-md text-on-surface font-semibold">Central SSO Active</span>
                  <span className="font-metadata-caps text-[10px] text-outline">Full Remote Privileges</span>
                </div>
              </div>
            </div>
          </div>
          <div className="pt-4 flex flex-col sm:flex-row items-stretch gap-3">
            <div className="relative flex-1">
              <Icon name="search" className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-xl pointer-events-none" />
              <input
                className="w-full h-12 pl-12 pr-28 bg-surface-container-lowest rounded-xl font-body-md text-on-surface placeholder:text-outline focus:outline-none shadow-sm transition-all"
                placeholder="Search databases by title, vendor, ISSN, discipline (e.g. IEEE, Elsevier, MeSH)..."
                type="text"
                aria-label="Search digital resources"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  reset();
                }}
              />
              <span className="absolute right-3.5 top-1/2 -translate-y-1/2 font-metadata-caps text-[10px] uppercase tracking-wider text-outline bg-surface-container px-2 py-1 rounded">
                Title / Topic
              </span>
            </div>
            <div className="flex items-center gap-2">
              <div className="relative min-w-[180px]">
                <select
                  aria-label="Discipline"
                  className="w-full h-12 px-4 pr-10 bg-surface-container-lowest rounded-xl font-label-lg text-on-surface appearance-none focus:outline-none shadow-sm cursor-pointer"
                >
                  <option>All Disciplines</option>
                  <option>Natural &amp; Physical Sciences</option>
                  <option>Arts &amp; Humanities</option>
                  <option>Social &amp; Political Sciences</option>
                  <option>Engineering &amp; Technology</option>
                  <option>Clinical Medicine &amp; Health</option>
                </select>
                <Icon name="expand_more" className="absolute right-3.5 top-1/2 -translate-y-1/2 text-outline pointer-events-none" />
              </div>
              <div className="relative min-w-[170px]">
                <select
                  aria-label="Licensure"
                  className="w-full h-12 px-4 pr-10 bg-surface-container-lowest rounded-xl font-label-lg text-on-surface appearance-none focus:outline-none shadow-sm cursor-pointer"
                >
                  <option>All Licensures</option>
                  <option>Institutional (SSO)</option>
                  <option>Open Access (OA)</option>
                  <option>On-Campus Only</option>
                </select>
                <Icon name="tune" className="absolute right-3.5 top-1/2 -translate-y-1/2 text-outline pointer-events-none" />
              </div>
            </div>
          </div>
        </div>
      </header>

      <section className="flex flex-col space-y-4">
        <div className="flex flex-wrap items-center gap-2" role="tablist">
          {tabs.map((t) => {
            const active = t.type === type;
            return (
              <button
                key={t.type}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => {
                  setType(t.type);
                  reset();
                }}
                className={`group flex items-center gap-2.5 px-5 py-3 rounded-xl transition-all ${
                  active
                    ? "bg-primary text-on-primary shadow-md"
                    : "bg-surface-container-lowest text-on-surface shadow-sm hover:bg-surface-container"
                }`}
              >
                <Icon name={t.icon} className={`text-xl ${active ? "text-secondary-fixed" : "text-outline"}`} />
                <span className="font-label-lg font-semibold">{t.label}</span>
                <span
                  className={`font-metadata-caps text-[11px] px-2 py-0.5 rounded-full font-medium ${
                    active ? "bg-primary-container text-on-primary-container" : "bg-surface-container-high text-on-surface-variant"
                  }`}
                >
                  {t.countLabel}
                </span>
              </button>
            );
          })}
        </div>

        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 py-2 px-1">
          <div className="flex flex-wrap items-center gap-1.5">
            {FILTERS.map((f) => (
              <button
                key={f}
                type="button"
                aria-pressed={f === filter}
                onClick={() => {
                  setFilter(f);
                  reset();
                }}
                className={`px-3.5 py-1.5 rounded-full font-label-md transition-all ${
                  f === filter
                    ? "bg-secondary-container text-on-secondary-container font-semibold"
                    : "bg-surface-container-lowest text-on-surface-variant hover:text-on-surface"
                }`}
              >
                {f}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <span className="font-metadata-caps text-outline uppercase tracking-wider tabular-nums" data-testid="displaying">
              Displaying {shown.length} of {total.toLocaleString("en-US")} {tab.noun}
            </span>
            <div className="flex items-center bg-surface-container-lowest rounded-lg p-1">
              {SORTS.map((s) => (
                <button
                  key={s}
                  type="button"
                  aria-pressed={s === sort}
                  onClick={() => {
                    setSort(s);
                    reset();
                  }}
                  className={`px-2.5 py-1 rounded-md font-label-md ${
                    s === sort ? "text-on-surface bg-surface-container font-semibold" : "text-outline hover:text-on-surface"
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div className="lg:col-span-8 flex flex-col space-y-5">
          {shown.length === 0 ? (
            <div className="bg-surface-container-lowest rounded-2xl p-10 shadow-sm text-center font-body-md text-on-surface-variant">
              No {tab.noun} match your search or filters.
            </div>
          ) : (
            shown.map((r) => (
              <ResourceCard
                key={r.id}
                resource={r}
                saved={saved.has(r.id)}
                onToggleSave={() => toggleSave(r.id)}
                launchLabel={LAUNCH_LABEL[r.type]}
              />
            ))
          )}
          <Pagination page={current} last={last} onChange={setPage} />
        </div>
        <AccessSidebar />
      </div>
    </div>
  );
}

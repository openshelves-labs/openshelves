"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import type { RecentSerial, Serial } from "@/types";
import Icon from "@/components/Icon";
import SerialCard from "./SerialCard";
import RecentPanel, { type RecentRow } from "./RecentPanel";
import SidePanels from "./SidePanels";
import IssnModal from "./IssnModal";

type FlagKey = "peerReviewed" | "openAccess" | "fullText" | "print" | "indexed";
type SortKey = "relevance" | "title" | "impact" | "depth";

const FILTERS: { key: FlagKey; label: string; icon: string }[] = [
  { key: "peerReviewed", label: "Peer Reviewed", icon: "verified" },
  { key: "openAccess", label: "Open Access", icon: "lock_open" },
  { key: "fullText", label: "Full-Text Online", icon: "description" },
  { key: "print", label: "Print in Stacks", icon: "shelves" },
  { key: "indexed", label: "Scopus / WoS Indexed", icon: "insights" },
];

const SORTS: { key: SortKey; label: string }[] = [
  { key: "relevance", label: "Sort: Relevance" },
  { key: "title", label: "Sort: Title (A–Z)" },
  { key: "impact", label: "Sort: Impact Factor (High to Low)" },
  { key: "depth", label: "Sort: Archival Depth" },
];

const ALL_SUBJECTS = "All Disciplines";
const LETTERS = ["All", "#", ..."ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("")];
const PAGE_SIZE = 5;

const initialOf = (title: string): string => {
  const c = title.trim().charAt(0).toUpperCase();
  return /[A-Z]/.test(c) ? c : "#";
};

const CHIP_BASE = "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full font-label-md transition-all";
const SELECT = "appearance-none h-9 pl-3 pr-8 rounded-lg bg-surface-container-lowest text-on-surface font-label-md focus:outline-none cursor-pointer";

interface JournalsViewProps {
  serials: Serial[];
  recent: RecentSerial[];
}

/** Journals & Serials directory with client-side filtering, sorting and desk ledger. */
export default function JournalsView({ serials, recent }: JournalsViewProps) {
  const [query, setQuery] = useState("");
  const [letter, setLetter] = useState("All");
  const [flags, setFlags] = useState<Record<FlagKey, boolean>>({
    peerReviewed: true,
    openAccess: false,
    fullText: true,
    print: false,
    indexed: false,
  });
  const [subject, setSubject] = useState(ALL_SUBJECTS);
  const [sort, setSort] = useState<SortKey>("relevance");
  const [page, setPage] = useState(1);
  const [savedIds, setSavedIds] = useState<string[]>([]);
  const [recentList, setRecentList] = useState<RecentSerial[]>(recent);
  const [issnOpen, setIssnOpen] = useState(false);

  const subjects = useMemo(() => Array.from(new Set(serials.map((s) => s.category))).sort(), [serials]);
  const availableLetters = useMemo(() => new Set(serials.map((s) => initialOf(s.title))), [serials]);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = serials.filter((s) => {
      if (letter !== "All" && initialOf(s.title) !== letter) return false;
      if (subject !== ALL_SUBJECTS && s.category !== subject) return false;
      if (FILTERS.some((f) => flags[f.key] && !s[f.key])) return false;
      if (!q) return true;
      return [s.title, s.issn, s.eIssn, s.publisher, s.category, ...s.taxonomy].some((v) => v.toLowerCase().includes(q));
    });
    if (sort === "title") list.sort((a, b) => a.title.localeCompare(b.title));
    else if (sort === "impact") list.sort((a, b) => b.impactFactor - a.impactFactor);
    else if (sort === "depth") list.sort((a, b) => a.holdingsFrom - b.holdingsFrom);
    return list;
  }, [serials, query, letter, subject, flags, sort]);

  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const current = Math.min(page, pageCount);
  const start = (current - 1) * PAGE_SIZE;
  const visible = results.slice(start, start + PAGE_SIZE);

  const recentRows: RecentRow[] = recentList.flatMap((r) => {
    const serial = serials.find((s) => s.id === r.serialId);
    return serial ? [{ serial, viewedLabel: r.viewedLabel }] : [];
  });

  const resetPage = () => setPage(1);
  const browse = (id: string) =>
    setRecentList((prev) => [{ serialId: id, viewedLabel: "Viewed just now" }, ...prev.filter((r) => r.serialId !== id)]);
  const toggleSave = (id: string) => setSavedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const indexedUnder = [letter !== "All" ? `'${letter}'` : null, query.trim() ? `'${query.trim()}'` : null].filter(Boolean).join(" & ");

  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <div className="flex flex-col w-full">
        <div className="flex items-center justify-between gap-4 pb-4">
          <nav aria-label="Breadcrumb" className="flex items-center gap-2 font-body-sm text-on-surface-variant">
            <Link href="/" className="hover:text-primary transition-colors flex items-center gap-1">
              <Icon name="home" className="text-sm" />
              <span>Home</span>
            </Link>
            <span className="text-outline text-xs">/</span>
            <Link href="/catalog" className="hover:text-primary transition-colors">
              Catalog
            </Link>
            <span className="text-outline text-xs">/</span>
            <span className="font-medium text-on-surface">Journals &amp; Serials</span>
          </nav>
          <div className="hidden sm:flex items-center gap-2.5">
            <button type="button" className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-lowest text-on-surface-variant hover:text-primary hover:bg-surface-container-low transition-colors shadow-sm">
              <Icon name="account_tree" className="text-[17px] text-primary" />
              <span className="font-label-md">Subject Directory</span>
            </button>
            <button type="button" className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-lowest text-on-surface-variant hover:text-primary hover:bg-surface-container-low transition-colors shadow-sm">
              <Icon name="notifications_active" className="text-[17px] text-primary" />
              <span className="font-label-md">Coverage Alerts</span>
            </button>
            <button type="button" onClick={() => setIssnOpen(true)} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-on-primary hover:bg-on-primary-fixed-variant transition-colors shadow-sm">
              <Icon name="search_check" className="text-[17px]" />
              <span className="font-label-md">ISSN Quick Lookup</span>
            </button>
          </div>
        </div>

        <div className="bg-surface-container-lowest rounded-xl p-7 shadow-sm relative overflow-hidden mb-6">
          <div className="absolute -right-8 -bottom-8 w-56 h-56 rounded-full bg-primary/5 pointer-events-none blur-2xl" />
          <div className="relative z-10 max-w-3xl space-y-2">
            <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-secondary-fixed text-on-secondary-fixed">
              <span className="w-1.5 h-1.5 rounded-full bg-primary" />
              <span className="font-metadata-caps tracking-widest uppercase">Scholarly Holdings • Subscribed &amp; Archival</span>
            </div>
            <h1 className="font-headline-lg text-on-surface tracking-tight">Journals &amp; Periodical Serials</h1>
            <p className="font-body-lg text-on-surface-variant max-w-2xl leading-relaxed">
              Discover 42,800+ peer-reviewed scholarly journals, proceedings, and university serial subscriptions with institutional full-text
              access and stacks preservation archives.
            </p>
          </div>
          <div className="relative mt-6 pt-5">
            <div className="flex flex-col md:flex-row items-stretch gap-2.5">
              <div className="relative flex-1">
                <Icon name="auto_stories" className="absolute left-3.5 top-1/2 -translate-y-1/2 text-outline pointer-events-none text-xl" />
                <input
                  type="search"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    resetPage();
                  }}
                  placeholder="Search by journal title, ISSN (e.g. 0028-0836), subject, or publisher…"
                  className="w-full h-12 pl-11 pr-10 bg-surface-container-low text-on-surface rounded-lg font-body-md placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest transition-all [&::-webkit-search-cancel-button]:appearance-none"
                />
                {query && (
                  <button
                    type="button"
                    title="Clear input"
                    onClick={() => {
                      setQuery("");
                      resetPage();
                    }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-outline hover:text-on-surface p-1 rounded transition-colors"
                  >
                    <Icon name="close" className="text-base" />
                  </button>
                )}
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button type="button" className="h-12 px-6 rounded-lg bg-primary text-on-primary hover:bg-on-primary-fixed-variant font-label-lg flex items-center justify-center gap-2 transition-all shadow-sm">
                  <Icon name="search" className="text-lg" />
                  <span>Search Serials</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-surface-container-lowest rounded-xl p-3 shadow-sm mb-6 flex flex-col md:flex-row items-center gap-3">
          <span className="font-metadata-caps uppercase text-outline px-2 shrink-0">Title Index</span>
          <div className="flex items-center gap-1 overflow-x-auto w-full py-1 scrollbar-none text-center">
            {LETTERS.map((l) => {
              const active = letter === l;
              const disabled = l !== "All" && !availableLetters.has(l);
              return (
                <button
                  key={l}
                  type="button"
                  disabled={disabled}
                  onClick={() => {
                    setLetter(l);
                    resetPage();
                  }}
                  className={`h-8 ${l === "All" ? "px-3" : "w-8"} rounded-lg font-label-md transition-colors shrink-0 ${
                    active
                      ? "bg-primary text-on-primary font-semibold"
                      : disabled
                        ? "text-outline/50 cursor-not-allowed"
                        : "text-on-surface-variant hover:bg-surface-container-high"
                  }`}
                >
                  {l}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6">
          <div className="flex flex-wrap items-center gap-2">
            {FILTERS.map((f) => {
              const on = flags[f.key];
              return (
                <button
                  key={f.key}
                  type="button"
                  aria-pressed={on}
                  onClick={() => {
                    setFlags((p) => ({ ...p, [f.key]: !p[f.key] }));
                    resetPage();
                  }}
                  className={`${CHIP_BASE} ${on ? "bg-secondary-fixed text-primary" : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"}`}
                >
                  <Icon name={f.icon} className="text-[16px]" />
                  <span>{f.label}</span>
                  {on && <Icon name="check" className="text-sm" />}
                </button>
              );
            })}
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <div className="relative">
              <select
                aria-label="Subject"
                value={subject}
                onChange={(e) => {
                  setSubject(e.target.value);
                  resetPage();
                }}
                className={SELECT}
              >
                {[...subjects, ALL_SUBJECTS].map((s) => (
                  <option key={s} value={s}>
                    Subject: {s}
                  </option>
                ))}
              </select>
              <Icon name="expand_more" className="absolute right-2.5 top-1/2 -translate-y-1/2 text-outline pointer-events-none text-base" />
            </div>
            <div className="relative">
              <select
                aria-label="Sort"
                value={sort}
                onChange={(e) => {
                  setSort(e.target.value as SortKey);
                  resetPage();
                }}
                className={SELECT}
              >
                {SORTS.map((s) => (
                  <option key={s.key} value={s.key}>
                    {s.label}
                  </option>
                ))}
              </select>
              <Icon name="sort" className="absolute right-2.5 top-1/2 -translate-y-1/2 text-outline pointer-events-none text-base" />
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between pb-3">
          <div className="flex items-center gap-2">
            <span className="font-metadata-caps uppercase text-outline">Search Register</span>
            <span className="text-outline text-xs">•</span>
            <span className="font-body-sm text-on-surface-variant">
              Showing <strong className="text-on-surface font-semibold tabular-nums">{results.length}</strong> indexed serials matching active criteria
            </span>
          </div>
          {indexedUnder && <div className="font-metadata-caps text-on-surface-variant uppercase">Indexed under {indexedUnder}</div>}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-8 space-y-4">
            {visible.length === 0 ? (
              <div className="bg-surface-container-lowest rounded-xl p-10 shadow-sm text-center space-y-2">
                <Icon name="search_off" className="text-outline text-3xl" />
                <p className="font-headline-sm text-on-surface">No serials match the active criteria</p>
                <p className="font-body-sm text-on-surface-variant">Adjust the title index, filters or search terms to widen the register.</p>
              </div>
            ) : (
              visible.map((s) => (
                <SerialCard key={s.id} serial={s} saved={savedIds.includes(s.id)} onToggleSave={() => toggleSave(s.id)} onBrowse={() => browse(s.id)} />
              ))
            )}

            <div className="pt-6 pb-2 flex flex-col sm:flex-row items-center justify-between gap-4">
              <span className="font-body-sm text-on-surface-variant">
                {results.length === 0 ? (
                  "No serial records"
                ) : (
                  <>
                    Showing{" "}
                    <strong className="text-on-surface tabular-nums">
                      {start + 1}–{start + visible.length}
                    </strong>{" "}
                    of {results.length} serial records (Page {current} of {pageCount})
                  </>
                )}
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  aria-label="Previous page"
                  disabled={current === 1}
                  onClick={() => setPage(current - 1)}
                  className={`h-9 w-9 rounded-lg flex items-center justify-center ${
                    current === 1 ? "bg-surface-container-low text-outline cursor-not-allowed" : "bg-surface-container-lowest hover:bg-surface-container-low text-on-surface transition-colors"
                  }`}
                >
                  <Icon name="chevron_left" className="text-base" />
                </button>
                {Array.from({ length: pageCount }, (_, i) => i + 1).map((n) => (
                  <button
                    key={n}
                    type="button"
                    onClick={() => setPage(n)}
                    className={`h-9 w-9 rounded-lg font-label-md tabular-nums ${
                      n === current ? "bg-primary text-on-primary font-semibold" : "bg-surface-container-lowest hover:bg-surface-container-low text-on-surface transition-colors"
                    }`}
                  >
                    {n}
                  </button>
                ))}
                <button
                  type="button"
                  aria-label="Next page"
                  disabled={current === pageCount}
                  onClick={() => setPage(current + 1)}
                  className={`h-9 w-9 rounded-lg flex items-center justify-center ${
                    current === pageCount ? "bg-surface-container-low text-outline cursor-not-allowed" : "bg-surface-container-lowest hover:bg-surface-container-low text-on-surface transition-colors"
                  }`}
                >
                  <Icon name="chevron_right" className="text-base" />
                </button>
              </div>
            </div>
          </div>

          <div className="lg:col-span-4 space-y-6">
            <RecentPanel rows={recentRows} onClear={() => setRecentList([])} />
            <SidePanels />
          </div>
        </div>
      </div>
      {issnOpen && <IssnModal onClose={() => setIssnOpen(false)} />}
    </main>
  );
}

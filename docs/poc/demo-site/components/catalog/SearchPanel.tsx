"use client";

import { useEffect, useRef, useState } from "react";
import Icon from "@/components/Icon";
import { SORT_OPTIONS, type SortKey } from "@/lib/catalog";

export interface RefinementChip {
  group: "access" | "types" | "subjects" | "languages";
  key: string;
  label: string;
  icon: string;
}

interface Props {
  draft: string;
  onDraft: (v: string) => void;
  onApply: () => void;
  onClearQuery: () => void;
  chips: RefinementChip[];
  onRemoveChip: (chip: RefinementChip) => void;
  bounds: { min: number; max: number };
  yearMin?: number;
  yearMax?: number;
  onYear: (min?: number, max?: number) => void;
  sort: SortKey;
  onSort: (s: SortKey) => void;
}

const STATIC_PILL =
  "inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-secondary-fixed text-primary font-label-lg text-label-md font-medium hover:bg-secondary-fixed-dim transition-colors";

function YearPill({ bounds, yearMin, yearMax, onYear }: Pick<Props, "bounds" | "yearMin" | "yearMax" | "onYear">) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => ref.current && !ref.current.contains(e.target as Node) && setOpen(false);
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const active = yearMin != null || yearMax != null;
  const parse = (v: string) => (v === "" ? undefined : Number(v));
  const label = active ? `${yearMin ?? bounds.min}–${yearMax ?? bounds.max}` : "Any";
  const input = "w-24 h-8 px-2 rounded-lg bg-surface-container-low border border-outline-variant text-body-sm font-body-sm text-on-surface focus:outline-none focus:border-primary";

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-label-lg text-label-md transition-colors ${
          active ? "bg-secondary-fixed text-primary font-medium" : "bg-surface-container hover:bg-surface-container-high text-on-surface"
        }`}
      >
        <span>Publication Year: {label}</span>
        <Icon name="tune" className="text-xs" />
      </button>
      {open && (
        <div className="absolute left-0 top-full z-30 mt-1.5 w-max rounded-xl border border-outline-variant bg-surface-container-lowest p-space-md shadow-lg">
          <div className="flex items-center gap-2 font-body-sm text-body-sm text-on-surface-variant">
            <label className="flex items-center gap-1.5">
              From
              <input
                type="number"
                aria-label="Publication year from"
                className={input}
                min={bounds.min}
                max={bounds.max}
                placeholder={String(bounds.min)}
                value={yearMin ?? ""}
                onChange={(e) => onYear(parse(e.target.value), yearMax)}
              />
            </label>
            <label className="flex items-center gap-1.5">
              To
              <input
                type="number"
                aria-label="Publication year to"
                className={input}
                min={bounds.min}
                max={bounds.max}
                placeholder={String(bounds.max)}
                value={yearMax ?? ""}
                onChange={(e) => onYear(yearMin, parse(e.target.value))}
              />
            </label>
          </div>
          <div className="mt-2.5 flex justify-end">
            <button type="button" onClick={() => onYear(undefined, undefined)} className="font-label-lg text-label-md text-outline hover:text-primary underline underline-offset-2">
              Reset range
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function SearchPanel({ draft, onDraft, onApply, onClearQuery, chips, onRemoveChip, bounds, yearMin, yearMax, onYear, sort, onSort }: Props) {
  const sortLabel = SORT_OPTIONS.find((o) => o.key === sort)?.label ?? "";
  return (
    <div className="bg-surface-container-lowest rounded-xl p-space-md mb-space-md shadow-sm">
      <form
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          onApply();
        }}
        className="flex flex-col md:flex-row items-stretch md:items-center gap-space-sm"
      >
        <div className="relative flex-1">
          <Icon name="manage_search" className="absolute left-3.5 top-1/2 -translate-y-1/2 text-primary text-xl" />
          <input
            type="search"
            aria-label="Search the catalog"
            value={draft}
            onChange={(e) => onDraft(e.target.value)}
            placeholder="Search keywords, subjects, call numbers, or persistent identifiers…"
            className="w-full h-11 pl-11 pr-24 bg-surface-container-low rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-outline"
          />
          {draft && (
            <button
              type="button"
              onClick={onClearQuery}
              className="absolute right-2 top-1/2 -translate-y-1/2 px-2.5 py-1 text-on-surface-variant hover:text-on-surface text-label-md font-label-lg rounded"
            >
              Clear
            </button>
          )}
        </div>
        <div className="flex items-center gap-space-xs shrink-0">
          <button
            type="submit"
            className="h-11 px-4 bg-primary hover:bg-primary-container text-on-primary rounded-lg font-label-lg text-label-md font-semibold flex items-center gap-2 transition-colors"
          >
            <Icon name="sync" className="text-lg" />
            <span>Update Search</span>
          </button>
          <button
            type="button"
            title="Save query to alerts"
            className="h-11 px-3 bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface rounded-lg font-label-lg text-label-md transition-colors flex items-center"
          >
            <Icon name="bookmark_add" className="text-lg" />
          </button>
        </div>
      </form>

      <div className="flex flex-wrap items-center gap-2 mt-3 pt-3">
        <span className="font-metadata-caps text-metadata-caps uppercase text-outline mr-1 tracking-wider">Applied Refinements:</span>
        <button type="button" className={STATIC_PILL}>
          <Icon name="domain" className="text-sm" />
          <span>All University Libraries</span>
          <Icon name="expand_more" className="text-xs" />
        </button>
        <button type="button" className={STATIC_PILL}>
          <span>Format: Books &amp; Serials</span>
          <Icon name="expand_more" className="text-xs" />
        </button>
        {chips.map((c) => (
          <span key={`${c.group}-${c.key}`} className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-primary text-on-primary font-label-lg text-label-md font-medium">
            <Icon name={c.icon} className="text-sm" />
            <span>{c.label}</span>
            <button type="button" aria-label={`Remove ${c.label}`} onClick={() => onRemoveChip(c)} className="ml-0.5 flex items-center hover:opacity-75">
              <Icon name="close" className="text-xs" />
            </button>
          </span>
        ))}
        <YearPill bounds={bounds} yearMin={yearMin} yearMax={yearMax} onYear={onYear} />
        <div className="relative ml-auto">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container hover:bg-surface-container-high text-on-surface font-label-lg text-label-md transition-colors">
            <span className="text-outline">Sort:</span>
            <span className="font-medium text-on-surface">{sortLabel}</span>
            <Icon name="arrow_drop_down" className="text-xs" />
          </span>
          <select
            aria-label="Sort results"
            value={sort}
            onChange={(e) => onSort(e.target.value as SortKey)}
            className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.key} value={o.key}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}

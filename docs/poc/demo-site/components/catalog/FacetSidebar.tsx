import type { ReactNode } from "react";
import Icon from "@/components/Icon";
import { ACCESS_FACETS, SUBJECT_FACETS, TYPE_FACETS, type CatalogFilters, type FacetDef } from "@/lib/catalog";

interface OptionProps {
  label: string;
  count: number;
  checked: boolean;
  onChange: () => void;
  accent?: boolean;
}

function Option({ label, count, checked, onChange, accent }: OptionProps) {
  return (
    <label className="flex items-center justify-between p-1.5 rounded-lg hover:bg-surface-container cursor-pointer transition-colors">
      <span className="flex items-center gap-2.5">
        <input type="checkbox" checked={checked} onChange={onChange} className="w-4 h-4 rounded text-primary focus:ring-0 accent-primary cursor-pointer" />
        <span className={checked ? "text-on-surface font-medium" : "text-on-surface-variant"}>{label}</span>
      </span>
      <span
        className={`font-metadata-caps text-metadata-caps px-2 py-0.5 rounded-full ${
          accent && checked ? "bg-secondary-fixed text-primary font-bold" : "bg-surface-container-high text-on-surface-variant"
        }`}
      >
        {count.toLocaleString("en-US")}
      </span>
    </label>
  );
}

function Group({ title, first, children }: { title: string; first?: boolean; children: ReactNode }) {
  return (
    <div className={`space-y-2.5 ${first ? "" : "pt-space-sm"}`}>
      <div className="flex items-center justify-between">
        <span className="font-metadata-caps text-metadata-caps uppercase tracking-wider text-on-surface font-bold">{title}</span>
        <Icon name="expand_less" className="text-outline text-base" />
      </div>
      <div className="space-y-1.5 text-body-sm font-body-sm">{children}</div>
    </div>
  );
}

interface Props {
  filters: CatalogFilters;
  /** books matching the current search text (before facet filters) — used for counts */
  pool: import("@/types").Book[];
  languages: { label: string; count: number }[];
  onToggle: (group: "access" | "types" | "subjects" | "languages", key: string) => void;
  onClear: () => void;
}

export default function FacetSidebar({ filters, pool, languages, onToggle, onClear }: Props) {
  const count = (d: FacetDef) => pool.filter(d.test).length;
  return (
    <aside className="order-last col-span-3 space-y-space-md sticky top-20 max-h-[calc(100vh/0.9_-_6rem)] overflow-y-auto">
      <div className="bg-surface-container-lowest rounded-xl p-5 shadow-sm space-y-space-lg">
        <div className="flex items-center justify-between pb-space-sm">
          <div className="flex items-center gap-2">
            <Icon name="filter_alt" className="text-primary text-lg" />
            <h2 className="font-headline-sm text-headline-sm text-on-surface text-base">Refine Facets</h2>
          </div>
          <div className="flex items-center gap-3">
            <button type="button" onClick={onClear} className="font-label-lg text-label-md text-outline hover:text-primary transition-colors underline underline-offset-2">
              Clear All
            </button>
          </div>
        </div>

        <div className="space-y-space-lg">

        <Group title="Holdings & Access" first>
          {ACCESS_FACETS.map((d) => (
            <Option key={d.key} label={d.label} count={count(d)} checked={filters.access.includes(d.key)} onChange={() => onToggle("access", d.key)} />
          ))}
        </Group>
        <Group title="Resource Type">
          {TYPE_FACETS.map((d) => (
            <Option key={d.key} label={d.label} count={count(d)} checked={filters.types.includes(d.key)} onChange={() => onToggle("types", d.key)} />
          ))}
        </Group>
        <Group title="Taxonomy & Subject">
          {SUBJECT_FACETS.map((d) => (
            <Option key={d.key} label={d.label} count={count(d)} accent checked={filters.subjects.includes(d.key)} onChange={() => onToggle("subjects", d.key)} />
          ))}
        </Group>
        <Group title="Language">
          {languages.map((l) => (
            <Option key={l.label} label={l.label} count={l.count} checked={filters.languages.includes(l.label)} onChange={() => onToggle("languages", l.label)} />
          ))}
        </Group>

        <div className="pt-space-sm space-y-space-xs">
          <button
            type="button"
            className="w-full py-2.5 px-3 bg-surface-container-low hover:bg-surface-container text-on-surface font-label-lg text-label-md rounded-lg flex items-center justify-center gap-2 transition-colors"
          >
            <Icon name="rss_feed" className="text-base" />
            <span>Save Search Query / RSS</span>
          </button>
        </div>
        </div>
      </div>

      <div className="p-space-md rounded-xl bg-surface-container-low space-y-space-xs">
        <div className="flex items-center gap-2 text-primary font-semibold font-label-lg text-label-md">
          <Icon name="apartment" className="text-base" />
          <span>Inter-Library Logistics</span>
        </div>
        <p className="font-body-sm text-body-sm text-on-surface-variant">
          Courier transits between Cambridge Biological Archives and Central Science run daily at 10:00 &amp; 15:30. Holds placed today arrive by 11:00 AM tomorrow.
        </p>
      </div>
    </aside>
  );
}

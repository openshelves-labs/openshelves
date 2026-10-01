import Icon from "@/components/Icon";

export type IdentifierTabId = "isbn" | "title" | "asin" | "doi";

interface IdentifierTab {
  id: IdentifierTabId;
  label: string;
  short: string;
  fieldLabel: string;
  placeholder: string;
  icon: string;
}

export const IDENTIFIER_TABS: IdentifierTab[] = [
  { id: "isbn", label: "ISBN-10 / 13", short: "ISBN", fieldLabel: "Standard Identifier (ISBN)", placeholder: "e.g. 978-0-19-883492-2", icon: "barcode" },
  { id: "title", label: "Title + Author", short: "Title", fieldLabel: "Title & Author Query", placeholder: "e.g. Ecosystem Resilience, Sterling", icon: "title" },
  { id: "asin", label: "ASIN / EAN", short: "ASIN/EAN", fieldLabel: "Retail Identifier (ASIN / EAN-13)", placeholder: "e.g. B08XYZ1234 or 9780198834922", icon: "qr_code_2" },
  { id: "doi", label: "DOI / OCLC", short: "DOI/OCLC", fieldLabel: "Persistent Identifier (DOI / OCLC)", placeholder: "e.g. 10.1093/oso/9780198834922.001.0001", icon: "link" },
];

const FORMATS = [
  { id: "general", label: "General (Monograph / Print)" },
  { id: "comics", label: "Comics / Graphic Novel" },
  { id: "audio", label: "Audiobook & Spoken Word" },
  { id: "ebook", label: "Electronic Folio / E-Book" },
];

const REGISTRIES = [
  { label: "Open Library (Live)", on: true },
  { label: "Google Books (API v1)", on: true },
  { label: "Hardcover Community", on: true },
  { label: "Crossref Metadata", on: false },
];

interface Props {
  tab: IdentifierTabId;
  onTab: (t: IdentifierTabId) => void;
  identifier: string;
  onIdentifier: (v: string) => void;
  format: string;
  onFormat: (v: string) => void;
  loading: boolean;
  onRerun: () => void;
}

export default function IdentifierLookup({ tab, onTab, identifier, onIdentifier, format, onFormat, loading, onRerun }: Props) {
  const active = IDENTIFIER_TABS.find((t) => t.id === tab) ?? IDENTIFIER_TABS[0];
  return (
    <section className="space-y-space-lg rounded-xl bg-surface-container-lowest p-space-lg shadow-sm">
      <div className="flex flex-col justify-between gap-space-sm md:flex-row md:items-center">
        <div className="space-y-0.5">
          <h2 className="flex items-center gap-2 font-title-editorial text-title-editorial text-on-surface">
            <Icon name="manage_search" className="text-[20px] text-primary" />
            Multi-Source Bibliographic Ingestion
          </h2>
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Federated query across Library of Congress, Open Library, Google Books, and Crossref registers.
          </p>
        </div>
        <div role="tablist" className="inline-flex rounded-lg bg-surface-container p-1 font-label-md text-label-md">
          {IDENTIFIER_TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={t.id === tab}
              onClick={() => onTab(t.id)}
              className={`rounded px-3 py-1 transition-colors ${
                t.id === tab ? "bg-surface-container-lowest font-semibold text-primary shadow-sm" : "text-on-surface-variant hover:text-on-surface"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 items-end gap-space-md md:grid-cols-12">
        <div className="space-y-1.5 md:col-span-4">
          <label htmlFor="identifier-input" className="block font-metadata-caps text-metadata-caps font-semibold uppercase text-on-surface-variant">
            {active.fieldLabel}
          </label>
          <div className="relative flex items-center">
            <Icon name={active.icon} className="absolute left-3 text-[18px] text-outline" />
            <input
              id="identifier-input"
              type="text"
              value={identifier}
              placeholder={active.placeholder}
              onChange={(e) => onIdentifier(e.target.value)}
              className="h-10 w-full rounded-lg bg-surface-container-low pl-10 pr-3 font-body-md text-body-md tabular-nums text-on-surface placeholder:text-outline focus:bg-surface-container-lowest focus:outline-none"
            />
          </div>
        </div>
        <div className="space-y-1.5 md:col-span-4">
          <div className="flex items-center justify-between">
            <label htmlFor="format-select" className="font-metadata-caps text-metadata-caps font-semibold uppercase text-on-surface-variant">
              Resource Format Type
            </label>
            <span className="font-metadata-caps text-[10px] text-primary">MARC21 Record 008</span>
          </div>
          <div className="relative">
            <select
              id="format-select"
              value={format}
              onChange={(e) => onFormat(e.target.value)}
              className="h-10 w-full cursor-pointer appearance-none rounded-lg bg-surface-container-low pl-3 pr-8 font-body-md text-body-md text-on-surface focus:outline-none"
            >
              {FORMATS.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.label}
                </option>
              ))}
            </select>
            <Icon name="unfold_more" className="pointer-events-none absolute right-2.5 top-2.5 text-[20px] text-outline" />
          </div>
        </div>
        <div className="md:col-span-4">
          <button
            type="button"
            onClick={onRerun}
            disabled={loading}
            className="flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-primary-container px-space-md font-label-lg text-label-lg text-on-primary transition-colors hover:bg-primary disabled:opacity-80"
          >
            <Icon name="sync" className={`text-[18px] ${loading ? "animate-spin" : ""}`} />
            {loading ? "Querying registries…" : "Re-run Multi-Source Lookup"}
          </button>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-space-sm rounded-lg bg-surface-container-low p-space-sm pt-space-sm text-body-sm">
        <div className="flex flex-wrap items-center gap-space-md">
          <span className="font-metadata-caps text-metadata-caps uppercase text-outline">Target Registries:</span>
          {REGISTRIES.map((r) => (
            <span key={r.label} className={`inline-flex items-center gap-1.5 font-label-md text-label-md ${r.on ? "text-on-surface" : "text-outline"}`}>
              <span className={`h-2 w-2 rounded-full ${r.on ? "bg-primary" : "bg-outline-variant"}`} />
              {r.label}
            </span>
          ))}
        </div>
        <span className="font-body-sm text-body-sm italic text-on-surface-variant">Changing type adjusts cataloging rules &amp; field schemas</span>
      </div>
    </section>
  );
}

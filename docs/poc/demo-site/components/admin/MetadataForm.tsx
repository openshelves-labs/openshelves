"use client";

import { useRef, useState, type KeyboardEvent, type RefObject } from "react";
import type { CatalogProviderMatch, CatalogRecord } from "@/types";
import Icon from "@/components/Icon";

export interface ResolvedState {
  authors: boolean;
  year: boolean;
}

const LABEL = "font-metadata-caps text-metadata-caps uppercase text-on-surface font-bold tracking-wider";
const INPUT_BASE = "w-full rounded-lg font-body-md text-body-md text-on-surface focus:outline-none";
const INPUT_PLAIN = `${INPUT_BASE} h-11 bg-surface-container-low px-3 focus:bg-surface-container-lowest`;
const INPUT_FLAGGED = `${INPUT_BASE} h-11 bg-surface-container-lowest px-3`;

interface ChipOption {
  label: string;
  value: string;
}

function PickerChips({
  options,
  value,
  onPick,
  onCustom,
  customActive,
}: {
  options: ChipOption[];
  value: string;
  onPick: (v: string) => void;
  onCustom?: () => void;
  customActive?: boolean;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 pt-1">
      {options.map((o) => {
        const active = o.value === value;
        return (
          <button
            key={o.label}
            type="button"
            aria-pressed={active}
            onClick={() => onPick(o.value)}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 font-label-md text-label-md transition-colors ${
              active
                ? "bg-primary text-on-primary shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container"
            }`}
          >
            {active && <Icon name="check" className="text-[14px]" />}
            {o.label}
          </button>
        );
      })}
      {onCustom && (
        <button
          type="button"
          onClick={onCustom}
          className={`rounded-full px-3 py-1.5 font-label-md text-label-md transition-colors ${
            customActive ? "bg-primary text-on-primary shadow-sm" : "bg-surface-container-lowest text-outline hover:bg-surface-container"
          }`}
        >
          Custom Edit
        </button>
      )}
    </div>
  );
}

function FlagBadge({ resolved, text }: { resolved: boolean; text: string }) {
  return (
    <span className="rounded bg-surface-container-high px-2 py-0.5 font-metadata-caps text-metadata-caps font-bold uppercase text-on-surface-variant">
      {resolved ? "Resolved" : text}
    </span>
  );
}

interface Props {
  record: CatalogRecord;
  providers: CatalogProviderMatch[];
  resolved: ResolvedState;
  onEdit: (patch: Partial<CatalogRecord>) => void;
  onResolve: (k: keyof ResolvedState) => void;
  onAcceptAll: () => void;
}

export default function MetadataForm({ record, providers, resolved, onEdit, onResolve, onAcceptAll }: Props) {
  const authorsRef = useRef<HTMLInputElement>(null);
  const [adding, setAdding] = useState(false);
  const [draft, setDraft] = useState("");

  const open = providers.filter((p) => p.id === "open-library" || p.id === "google-books");
  const authorOptions = open.map((p) => ({ label: p.pickers.authors, value: p.record.authors }));
  const yearOptions = open.map((p) => ({ label: p.pickers.year, value: p.record.year }));
  const pending = Number(!resolved.authors) + Number(!resolved.year);

  const commitSubject = () => {
    const v = draft.trim();
    if (v && !record.subjects.some((s) => s.toLowerCase() === v.toLowerCase())) onEdit({ subjects: [...record.subjects, v] });
    setDraft("");
    setAdding(false);
  };
  const onSubjectKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      commitSubject();
    } else if (e.key === "Escape") {
      setDraft("");
      setAdding(false);
    }
  };

  const pickAuthors = (v: string) => {
    onEdit({ authors: v });
    onResolve("authors");
  };
  const pickYear = (v: string) => {
    onEdit({ year: v });
    onResolve("year");
  };

  return (
    <section className="space-y-space-lg rounded-xl bg-surface-container-lowest p-space-lg shadow-sm">
      <div className="flex flex-col justify-between gap-space-sm md:flex-row md:items-center">
        <div className="space-y-0.5">
          <h2 className="font-headline-sm text-headline-sm text-on-surface">Resource Metadata &amp; Conflict Resolution</h2>
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Fields with discrepancy between providers are flagged in amber. Select preferred source or type manual override.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-metadata-caps text-metadata-caps uppercase text-outline">Authority Standard:</span>
          <span className="rounded bg-surface-container px-2 py-0.5 font-metadata-caps text-metadata-caps font-semibold text-on-surface">Dublin Core + MARC21</span>
        </div>
      </div>

      <div className="flex items-start gap-space-sm rounded-lg bg-surface-container-high p-space-md text-on-surface">
        <Icon name={pending ? "rule_settings" : "task_alt"} className="mt-0.5 shrink-0 text-[22px] text-primary" />
        <div className="flex-1 space-y-0.5">
          {pending ? (
            <>
              <div className="font-label-lg text-label-lg font-semibold text-on-surface">
                {pending} Discrepanc{pending > 1 ? "ies" : "y"} detected between Open Library and Google Books
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Review the highlighted <span className="font-medium text-primary">Authors &amp; Contributors</span> and{" "}
                <span className="font-medium text-primary">Publication Date</span> entries before committing to the main catalog ledger.
              </p>
            </>
          ) : (
            <>
              <div className="font-label-lg text-label-lg font-semibold text-on-surface">All discrepancies resolved</div>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Authors &amp; Contributors and Publication Date are ready to be committed to the main catalog ledger.
              </p>
            </>
          )}
        </div>
        <button type="button" onClick={onAcceptAll} disabled={!pending} className="font-label-md text-label-md font-semibold text-primary hover:underline disabled:opacity-50 disabled:hover:no-underline">
          Accept All Recommended
        </button>
      </div>

      <div className="grid grid-cols-1 gap-space-lg md:grid-cols-2">
        <div className="space-y-1.5 md:col-span-2">
          <div className="flex items-center justify-between">
            <label htmlFor="f-title" className={LABEL}>
              Resource Title (245 $a)
            </label>
            <span className="flex items-center gap-1 font-metadata-caps text-metadata-caps font-semibold text-primary">
              <Icon name="check" className="text-[14px]" />
              Source: Open Library (Verified)
            </span>
          </div>
          <input id="f-title" type="text" value={record.title} onChange={(e) => onEdit({ title: e.target.value })} className={`${INPUT_PLAIN} font-body-lg text-body-lg`} />
        </div>

        <div className="space-y-2 rounded-xl bg-surface-container-low p-space-md md:col-span-2">
          <div className="flex items-center justify-between">
            <label htmlFor="f-authors" className={`${LABEL} flex items-center gap-1.5`}>
              <Icon name="warning" className="text-[18px] text-primary" />
              Authors &amp; Contributors (100 / 700)
            </label>
            <FlagBadge resolved={resolved.authors} text="Discrepancy Detected" />
          </div>
          <input
            id="f-authors"
            ref={authorsRef}
            type="text"
            value={record.authors}
            onChange={(e) => {
              onEdit({ authors: e.target.value });
              onResolve("authors");
            }}
            className={INPUT_FLAGGED}
          />
          <PickerChips
            options={authorOptions}
            value={record.authors}
            onPick={pickAuthors}
            onCustom={() => authorsRef.current?.focus()}
            customActive={resolved.authors && !authorOptions.some((o) => o.value === record.authors)}
          />
        </div>

        <div className="space-y-2 rounded-xl bg-surface-container-low p-space-md">
          <div className="flex items-center justify-between">
            <label htmlFor="f-year" className={`${LABEL} flex items-center gap-1.5`}>
              <Icon name="calendar_clock" className="text-[18px] text-primary" />
              Publication Date / Year (260 $c)
            </label>
            <FlagBadge resolved={resolved.year} text="Year Mismatch" />
          </div>
          <input
            id="f-year"
            type="text"
            inputMode="numeric"
            value={record.year}
            onChange={(e) => {
              onEdit({ year: e.target.value });
              onResolve("year");
            }}
            className={`${INPUT_FLAGGED} tabular-nums`}
          />
          <PickerChips options={yearOptions} value={record.year} onPick={pickYear} />
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="f-publisher" className={LABEL}>
              Publisher &amp; Imprint (260 $b)
            </label>
            <span className="flex items-center gap-1 font-metadata-caps text-metadata-caps font-semibold text-primary">
              <Icon name="check" className="text-[14px]" />
              Source: Open Library
            </span>
          </div>
          <input id="f-publisher" type="text" value={record.publisher} onChange={(e) => onEdit({ publisher: e.target.value })} className={INPUT_PLAIN} />
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="f-call" className={LABEL}>
              LC Classification / Call Number (050)
            </label>
            <span className="font-metadata-caps text-metadata-caps font-semibold text-secondary">LC Authority Linked</span>
          </div>
          <div className="relative flex items-center">
            <Icon name="shelves" className="absolute left-3 text-[18px] text-outline" />
            <input id="f-call" type="text" value={record.callNumber} onChange={(e) => onEdit({ callNumber: e.target.value })} className={`${INPUT_PLAIN} pl-10 tabular-nums`} />
          </div>
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="f-ddc" className={LABEL}>
              Dewey Decimal (DDC 082)
            </label>
            <span className="font-metadata-caps text-metadata-caps text-outline">DDC 23rd Edition</span>
          </div>
          <input id="f-ddc" type="text" value={record.ddc} onChange={(e) => onEdit({ ddc: e.target.value })} className={`${INPUT_PLAIN} tabular-nums`} />
        </div>

        <div className="space-y-2 md:col-span-2">
          <span className={`block ${LABEL}`}>FAST / MeSH Controlled Subject Headings (650)</span>
          <div className="flex flex-wrap items-center gap-2 rounded-lg bg-surface-container-low p-2">
            {record.subjects.map((s) => (
              <span key={s} className="inline-flex items-center gap-1.5 rounded-full bg-surface-container-lowest px-3 py-1 font-label-md text-label-md text-on-surface shadow-sm">
                {s}
                <button type="button" aria-label={`Remove ${s}`} onClick={() => onEdit({ subjects: record.subjects.filter((x) => x !== s) })} className="flex text-outline hover:text-error">
                  <Icon name="close" className="text-[14px]" />
                </button>
              </span>
            ))}
            {adding ? (
              <input
                autoFocus
                type="text"
                value={draft}
                placeholder="New heading, press Enter"
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={onSubjectKey}
                onBlur={commitSubject}
                className="h-7 w-48 rounded-full bg-surface-container-lowest px-3 font-label-md text-label-md text-on-surface placeholder:text-outline focus:outline-none"
              />
            ) : (
              <button
                type="button"
                onClick={() => setAdding(true)}
                className="inline-flex items-center gap-1 rounded-full bg-surface-container px-3 py-1 font-label-md text-label-md font-medium text-primary transition-colors hover:bg-surface-container-high"
              >
                <Icon name="add" className="text-[14px]" />
                Add Subject Heading
              </button>
            )}
          </div>
        </div>

        <div className="space-y-1.5 md:col-span-2">
          <div className="flex items-center justify-between">
            <label htmlFor="f-abstract" className={LABEL}>
              Abstract &amp; Scope Summary (520)
            </label>
            <span className="font-metadata-caps text-metadata-caps text-outline">English (en-GB)</span>
          </div>
          <textarea id="f-abstract" rows={4} value={record.abstract} onChange={(e) => onEdit({ abstract: e.target.value })} className={`${INPUT_BASE} bg-surface-container-low p-3 leading-relaxed focus:bg-surface-container-lowest`} />
        </div>
      </div>
    </section>
  );
}

"use client";

import { useMemo, useState } from "react";
import type { Book } from "@/types";
import { CITATION_STYLES, formatCitation, type CitationStyle } from "@/lib/citations";
import Icon from "@/components/Icon";
import { firstFamily } from "./bookText";

const SHORT: Record<CitationStyle, string> = { apa: "APA", mla: "MLA", chicago: "Chicago", bibtex: "BibTeX" };
const TAB_LABEL: Record<CitationStyle, string> = { apa: "APA 7th", mla: "MLA 9th", chicago: "Chicago", bibtex: "BibTeX" };

export default function BibliographyPreview({ books, total }: { books: Book[]; total: number }) {
  const [style, setStyle] = useState<CitationStyle>("apa");
  const [copied, setCopied] = useState(false);

  const entries = useMemo(
    () =>
      [...books]
        .sort((a, b) => firstFamily(a).localeCompare(firstFamily(b)))
        .map((b) => ({ id: b.id, text: formatCitation(b, style) })),
    [books, style],
  );

  const copyAll = async () => {
    const text = entries.map((e) => e.text).join(style === "bibtex" ? "\n\n" : "\n");
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      /* clipboard unavailable — still confirm in the demo */
    }
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  return (
    <section className="space-y-4 rounded-xl border border-[#E5E0D8] bg-surface-container-lowest p-5 shadow-sm">
      <div className="space-y-1">
        <div className="flex items-center justify-between">
          <h3 className="flex items-center gap-2 font-headline-sm text-base font-semibold text-on-surface">
            <Icon name="library_books" className="text-lg text-primary" />
            Bibliography Preview
          </h3>
          <span className="rounded bg-surface-container px-2 py-0.5 font-metadata-caps text-[10px] font-bold text-on-surface-variant">{total} Works</span>
        </div>
        <p className="font-body-sm text-xs text-on-surface-variant">Live formatted references generated from catalog records.</p>
      </div>
      <div className="flex rounded-lg bg-surface-container p-0.5 font-label-md text-xs" role="tablist">
        {CITATION_STYLES.map((s) => (
          <button
            key={s.id}
            type="button"
            role="tab"
            aria-selected={style === s.id}
            onClick={() => setStyle(s.id)}
            className={`flex-1 rounded-md py-1 text-center ${style === s.id ? "bg-surface-container-lowest font-semibold text-primary" : "text-on-surface-variant hover:text-on-surface"}`}
          >
            {TAB_LABEL[s.id]}
          </button>
        ))}
      </div>
      <div className="max-h-64 space-y-3 overflow-y-auto rounded-lg bg-surface-container-low p-3.5 font-body-sm text-[12px] leading-relaxed text-on-surface">
        {entries.length === 0 && <p className="text-outline">No works in this list yet.</p>}
        {entries.map((e) =>
          style === "bibtex" ? (
            <pre key={e.id} className="whitespace-pre-wrap break-all font-mono text-[11px]">
              {e.text}
            </pre>
          ) : (
            <p key={e.id} className="-indent-5 pl-5">
              {e.text}
            </p>
          ),
        )}
      </div>
      <div className="space-y-2 pt-1">
        <button
          type="button"
          onClick={copyAll}
          disabled={entries.length === 0}
          className="flex h-9 w-full items-center justify-center gap-2 rounded-lg bg-primary font-label-md text-xs text-on-primary transition-colors hover:bg-primary-container disabled:opacity-50"
        >
          <Icon name={copied ? "check" : "content_copy"} className="text-base" />
          <span>{copied ? "Copied to Clipboard" : `Copy All Citations (${SHORT[style]})`}</span>
        </button>
        <div className="grid grid-cols-2 gap-2">
          <button type="button" className="flex h-8 items-center justify-center gap-1.5 rounded-lg bg-surface-container font-label-md text-xs text-on-surface transition-colors hover:bg-surface-container-high">
            <Icon name="download" className="text-sm text-outline" />
            <span>Download .bib</span>
          </button>
          <button type="button" className="flex h-8 items-center justify-center gap-1.5 rounded-lg bg-surface-container font-label-md text-xs text-on-surface transition-colors hover:bg-surface-container-high">
            <Icon name="import_contacts" className="text-sm text-outline" />
            <span>Export Zotero</span>
          </button>
        </div>
      </div>
    </section>
  );
}

"use client";

import { useState } from "react";
import type { Book } from "@/types";
import Icon from "@/components/Icon";
import { CITATION_STYLES, formatCitation, type CitationStyle } from "@/lib/citations";

export default function CitePanel({ book }: { book: Book }) {
  const [style, setStyle] = useState<CitationStyle>("apa");
  const [copied, setCopied] = useState(false);
  const text = formatCitation(book, style);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      /* clipboard unavailable — still confirm so the flow is visible in the demo */
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 2200);
  };

  return (
    <div id="citationSection" className="bg-surface-container-lowest rounded-2xl p-6 sm:p-7 border border-[#E5E0D8] space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-[#E5E0D8]">
        <div className="flex items-center gap-2">
          <Icon name="format_quote" className="text-primary text-xl" />
          <h3 className="font-title-editorial font-bold text-on-surface text-base">Cite This Monograph</h3>
        </div>
        <div role="tablist" aria-label="Citation style" className="flex items-center bg-surface-container-low p-1 rounded-lg border border-[#E5E0D8] text-xs">
          {CITATION_STYLES.map((s) => (
            <button
              key={s.id}
              type="button"
              role="tab"
              aria-selected={style === s.id}
              onClick={() => setStyle(s.id)}
              className={`px-2.5 py-1 rounded ${style === s.id ? "bg-white text-primary font-bold" : "text-outline hover:text-on-surface font-medium"}`}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>
      <div className="relative bg-surface-container-low p-4 rounded-xl border border-[#E5E0D8]">
        <p data-testid="citation-text" className="font-mono text-xs text-on-surface leading-relaxed select-all whitespace-pre-wrap break-words">
          {text}
        </p>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
        <button
          type="button"
          onClick={copy}
          className="h-9 px-4 bg-primary hover:bg-[#1e3f2e] text-on-primary font-label-lg text-xs rounded-lg transition-colors flex items-center gap-1.5"
        >
          <Icon name={copied ? "check" : "content_copy"} className="text-base" />
          <span>{copied ? "Citation Copied!" : "Copy Formatted Citation"}</span>
        </button>
        <div className="flex items-center gap-2 font-body-sm text-xs">
          <button type="button" className="h-9 px-3 bg-surface-container-lowest hover:bg-surface-container border border-[#E5E0D8] rounded-lg text-on-surface flex items-center gap-1.5 transition-colors">
            <Icon name="download" className="text-sm text-primary" />
            <span>Export .RIS / EndNote</span>
          </button>
          <button type="button" className="h-9 px-3 bg-surface-container-lowest hover:bg-surface-container border border-[#E5E0D8] rounded-lg text-on-surface flex items-center gap-1.5 transition-colors">
            <Icon name="code" className="text-sm text-primary" />
            <span>Download .Bib</span>
          </button>
        </div>
      </div>
    </div>
  );
}

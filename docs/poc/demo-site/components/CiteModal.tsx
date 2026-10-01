"use client";

import { useEffect, useState } from "react";
import type { Book } from "@/types";
import { formatCitation, type CitationStyle } from "@/lib/citations";
import Icon from "./Icon";

const TABS: { id: CitationStyle; label: string }[] = [
  { id: "apa", label: "APA 7th" },
  { id: "mla", label: "MLA 9th" },
  { id: "bibtex", label: "BibTeX" },
];

export default function CiteModal({ book, onClose }: { book: Book | null; onClose: () => void }) {
  const [style, setStyle] = useState<CitationStyle>("apa");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!book) return;
    setStyle("apa");
    setCopied(false);
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [book, onClose]);

  if (!book) return null;
  const text = formatCitation(book, style);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      /* clipboard unavailable — ignore */
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 1600);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-label="Cite this work">
      <div className="absolute inset-0 bg-[#1b1c19]/30" onClick={onClose} />
      <div className="relative w-full max-w-xl rounded-2xl border border-border-warm-strong bg-white p-6 shadow-[0_4px_20px_-2px_rgba(30,35,32,0.06)]">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <Icon name="format_quote" className="text-xl text-primary" />
              <h2 className="font-headline-sm text-xl font-semibold text-on-surface">Cite this work</h2>
            </div>
            <p className="mt-1 truncate font-title-editorial text-sm text-on-surface-variant">{book.title}</p>
          </div>
          <button type="button" aria-label="Close" onClick={onClose} className="rounded-lg p-1.5 text-outline hover:bg-surface-paper hover:text-on-surface">
            <Icon name="close" className="text-xl" />
          </button>
        </div>

        <div className="mt-5 inline-flex rounded-lg border border-border-warm bg-surface-paper p-1">
          {TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => {
                setStyle(t.id);
                setCopied(false);
              }}
              className={`rounded-md px-3 py-1.5 font-label-lg text-xs transition-colors ${
                style === t.id ? "bg-white text-primary shadow-sm border border-border-warm" : "text-on-surface-variant hover:text-on-surface border border-transparent"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="mt-4 rounded-lg border-l-[3px] border-primary bg-surface-paper p-4">
          <pre
            className={`whitespace-pre-wrap break-words text-[13px] leading-relaxed text-on-surface ${
              style === "bibtex" ? "font-mono" : "font-title-editorial"
            }`}
          >
            {text}
          </pre>
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="h-10 rounded-lg border border-border-warm-strong bg-white px-5 font-label-lg text-sm text-on-surface hover:border-primary hover:bg-surface-paper hover:text-primary">
            Close
          </button>
          <button type="button" onClick={copy} className="inline-flex h-10 items-center gap-2 rounded-lg border border-primary-dark bg-primary px-5 font-label-lg text-sm text-white hover:bg-primary-dark">
            <Icon name={copied ? "check" : "content_copy"} className="text-base" />
            {copied ? "Copied" : "Copy Citation"}
          </button>
        </div>
      </div>
    </div>
  );
}

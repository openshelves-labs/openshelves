"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import type { Book, CatalogRecord, Contributor } from "@/types";
import { formatCitation } from "@/lib/citations";
import Icon from "@/components/Icon";

export type CiteFormat = "apa" | "bibtex";

function toContributor(name: string): Contributor {
  const clean = name.replace(/^(prof|dr|mr|ms|mrs)\.?\s+/i, "").trim();
  const parts = clean.split(/\s+/);
  const family = parts.pop() ?? clean;
  return { name, family, given: parts.join(" "), role: "author" };
}

/** Builds a minimal Book from the live form fields so lib/citations can format it. */
function toBook(r: CatalogRecord, isbn: string): Book | null {
  const contributors = r.authors
    .split(";")
    .map((a) => a.trim())
    .filter(Boolean)
    .map(toContributor);
  if (!r.title.trim() || contributors.length === 0) return null;
  return {
    id: "draft",
    title: r.title.trim(),
    contributors,
    publisher: r.publisher.trim(),
    publicationYear: Number(r.year) || 0,
    edition: r.edition || undefined,
    isbn: isbn || undefined,
    callNumber: r.callNumber,
    language: "en",
    resourceType: "monograph",
    subjects: r.subjects,
    availabilityStatus: "on_order",
    availabilityLabel: "",
    copies: [],
  };
}

function withItalicTitle(text: string, title: string): ReactNode {
  const i = text.indexOf(title);
  if (i < 0) return text;
  return (
    <>
      {text.slice(0, i)}
      <span className="italic">{title}</span>
      {text.slice(i + title.length)}
    </>
  );
}

interface Props {
  record: CatalogRecord;
  isbn: string;
  format: CiteFormat;
  onFormat: (f: CiteFormat) => void;
}

export default function CitationStrip({ record, isbn, format, onFormat }: Props) {
  const [copied, setCopied] = useState<CiteFormat | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);

  const book = toBook(record, isbn);
  const text = book ? formatCitation(book, format) : "";

  const copy = async (f: CiteFormat) => {
    onFormat(f);
    if (!book) return;
    try {
      await navigator.clipboard.writeText(formatCitation(book, f));
    } catch {
      /* clipboard unavailable — the format switch still applies */
    }
    setCopied(f);
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => setCopied(null), 1500);
  };

  const linkCls = (f: CiteFormat) =>
    `font-label-md text-label-md font-semibold text-primary hover:underline ${format === f ? "underline underline-offset-4" : ""}`;

  return (
    <div className="flex items-center justify-between gap-space-md rounded-lg bg-surface-container-low p-space-md">
      <div className="flex min-w-0 items-center gap-space-sm">
        <Icon name="format_quote" className="shrink-0 text-[24px] text-primary" />
        {book ? (
          <div className={`min-w-0 font-title-editorial text-body-md text-on-surface ${format === "apa" ? "truncate" : "whitespace-pre-wrap text-[13px] leading-5 tabular-nums"}`}>
            {format === "apa" ? withItalicTitle(text, book.title) : text}
          </div>
        ) : (
          <div className="font-title-editorial text-body-md text-outline">Add a title and at least one author to generate a citation.</div>
        )}
      </div>
      <div className="flex shrink-0 items-center gap-1.5">
        {copied ? (
          <span className="font-label-md text-label-md font-semibold text-primary">Copied {copied === "apa" ? "APA" : "BibTeX"}</span>
        ) : (
          <>
            <button type="button" aria-pressed={format === "apa"} onClick={() => copy("apa")} className={linkCls("apa")}>
              Copy APA
            </button>
            <span className="text-outline">/</span>
            <button type="button" aria-pressed={format === "bibtex"} onClick={() => copy("bibtex")} className={linkCls("bibtex")}>
              BibTeX
            </button>
          </>
        )}
      </div>
    </div>
  );
}

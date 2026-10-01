"use client";

import type { Book } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";

interface Props {
  book: Book;
  /** visible label; defaults to "Cite" */
  label?: string;
  /** full class override — otherwise the standard secondary-button look */
  className?: string;
  iconClassName?: string;
}

const DEFAULT =
  "inline-flex items-center gap-1.5 h-8 px-3 rounded-lg bg-white border border-border-warm text-on-surface font-label-lg text-xs hover:bg-stone-50 transition-colors";

/** Opens the shared citation modal (APA / MLA / BibTeX generated from the book's metadata). */
export default function CiteButton({ book, label = "Cite", className = DEFAULT, iconClassName = "text-sm text-outline" }: Props) {
  const { openCite } = useLibrary();
  return (
    <button type="button" className={className} onClick={() => openCite(book)}>
      <Icon name="format_quote" className={iconClassName} />
      {label && <span>{label}</span>}
    </button>
  );
}

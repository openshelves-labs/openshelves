"use client";

import { useState } from "react";
import type { Book } from "@/types";
import Icon from "./Icon";

const TONE: Record<NonNullable<Book["coverTone"]>, string> = {
  forest: "bg-[#2f5d45] text-white border-[#1E3F2E]",
  sage: "bg-[#c3e9d0] text-[#1E3F2E] border-[#aacfb8]",
  parchment: "bg-[#f0eee9] text-[#1E2320] border-[#D8D2C7]",
  charcoal: "bg-[#363f3b] text-white border-[#1b1c19]",
};

/**
 * 2:3 cover. Renders the remote image when present; otherwise (or if it fails
 * to load) a generated cover in the design-system palette. The wrapper fills its
 * parent — size it from outside (e.g. "w-14 h-20" or "w-full aspect-[2/3]").
 */
export default function BookCover({
  book,
  className = "",
  showTitleFallback = true,
}: {
  book: Pick<Book, "title" | "coverUrl" | "coverTone" | "contributors">;
  className?: string;
  showTitleFallback?: boolean;
}) {
  const [failed, setFailed] = useState(false);
  const tone = TONE[book.coverTone ?? "forest"];
  const showImg = book.coverUrl && !failed;
  return (
    <div className={`relative overflow-hidden rounded-[6px] border border-border-warm bg-surface-container-high ${className}`}>
      {showImg ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={book.coverUrl}
          alt={`Cover of ${book.title}`}
          className="h-full w-full object-cover"
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className={`flex h-full w-full flex-col items-center justify-center gap-1 border p-2 text-center ${tone}`}>
          <Icon name="book_2" className="text-2xl opacity-80" />
          {showTitleFallback && (
            <span className="font-title-editorial text-[11px] font-semibold leading-tight line-clamp-4">{book.title}</span>
          )}
        </div>
      )}
    </div>
  );
}

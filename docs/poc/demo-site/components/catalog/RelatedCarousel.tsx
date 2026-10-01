"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Book } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import { toneForStatus } from "@/lib/catalog";

const NAV = "w-8 h-8 rounded-lg bg-surface-container-lowest border border-[#E5E0D8] flex items-center justify-center transition-colors";

export default function RelatedCarousel({ books }: { books: Book[] }) {
  const track = useRef<HTMLDivElement>(null);
  const [edges, setEdges] = useState({ start: true, end: true });

  const measure = useCallback(() => {
    const el = track.current;
    if (!el) return;
    setEdges({ start: el.scrollLeft <= 2, end: el.scrollLeft + el.clientWidth >= el.scrollWidth - 2 });
  }, []);

  useEffect(() => {
    measure();
    window.addEventListener("resize", measure);
    return () => window.removeEventListener("resize", measure);
  }, [measure, books.length]);

  const scroll = (dir: 1 | -1) => {
    const el = track.current;
    const first = el?.firstElementChild as HTMLElement | null;
    if (!el || !first) return;
    el.scrollBy({ left: dir * (first.offsetWidth + 20), behavior: "smooth" });
  };

  if (!books.length) return null;

  return (
    <div className="pt-10 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-title-editorial font-bold text-xl text-on-surface">Related Resources &amp; Recommended Monographs</h2>
          <p className="font-body-sm text-xs text-outline mt-0.5">Complementary catalog records classified under Applied Ecology and Resilience Dynamics</p>
        </div>
        <div className="flex items-center gap-1.5">
          <button type="button" aria-label="Previous monographs" disabled={edges.start} onClick={() => scroll(-1)} className={`${NAV} ${edges.start ? "text-outline-variant cursor-not-allowed" : "text-outline hover:text-on-surface"}`}>
            <Icon name="chevron_left" className="text-base" />
          </button>
          <button type="button" aria-label="Next monographs" disabled={edges.end} onClick={() => scroll(1)} className={`${NAV} ${edges.end ? "text-outline-variant cursor-not-allowed" : "text-outline hover:text-on-surface"}`}>
            <Icon name="chevron_right" className="text-base" />
          </button>
        </div>
      </div>
      <div ref={track} onScroll={measure} className="flex gap-5 overflow-x-auto snap-x snap-mandatory scroll-smooth pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {books.map((b) => {
          const ok = toneForStatus(b.availabilityStatus) === "ok";
          return (
            <div
              key={b.id}
              className="bg-surface-container-lowest rounded-2xl p-4 border border-[#E5E0D8] hover:border-[#D8D2C7] transition-all flex flex-col justify-between group snap-start shrink-0 w-full sm:w-[calc((100%-1.25rem)/2)] lg:w-[calc((100%-3.75rem)/4)]"
            >
              <div>
                <div className="relative w-full aspect-[2/3] rounded-lg overflow-hidden bg-surface-container mb-3.5">
                  <BookCover book={b} className="h-full w-full !rounded-none !border-0" />
                  <span className="absolute top-2 right-2 bg-white/90 font-metadata-caps text-[9px] px-2 py-0.5 rounded text-primary font-bold border border-[#E5E0D8]">{b.publicationYear}</span>
                </div>
                <div className="font-metadata-caps text-[10px] text-outline">{b.callNumber}</div>
                <h3 className="font-title-editorial font-bold text-sm text-on-surface group-hover:text-primary transition-colors line-clamp-2 mt-1">
                  <Link href={`/catalog/${b.id}`}>{b.title}</Link>
                </h3>
                <p className="font-body-sm text-xs text-on-surface-variant mt-1">{b.contributors[0]?.name}</p>
              </div>
              <div className="mt-4 pt-3 border-t border-[#E5E0D8] flex items-center justify-between">
                <span className={`font-metadata-caps text-[10px] font-bold ${ok ? "text-primary" : "text-[#975A16]"}`}>{b.availabilityLabel}</span>
                <Link href={`/catalog/${b.id}`} className="text-xs font-label-md text-primary hover:underline flex items-center gap-0.5">
                  <span>View</span>
                  <Icon name="arrow_forward" className="text-xs" />
                </Link>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

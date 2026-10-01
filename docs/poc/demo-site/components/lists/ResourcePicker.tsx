"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { ID } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import { authorLine } from "./bookText";

/** "Add Resource" button + dropdown of catalog books not yet in the list. */
export default function ResourcePicker({ listId }: { listId: ID }) {
  const { books, lists, addToList } = useLibrary();
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const available = useMemo(() => {
    const inList = new Set(lists.find((l) => l.id === listId)?.items.map((i) => i.bookId));
    const needle = q.trim().toLowerCase();
    return books.filter((b) => !inList.has(b.id) && (!needle || `${b.title} ${authorLine(b)}`.toLowerCase().includes(needle)));
  }, [books, lists, listId, q]);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        className="flex h-10 items-center gap-2 rounded-lg bg-primary px-4 font-label-lg text-sm text-on-primary shadow-sm transition-colors hover:bg-primary-container"
      >
        <Icon name="add" className="text-lg" />
        <span>Add Resource</span>
      </button>
      {open && (
        <div role="menu" className="absolute right-0 z-30 mt-1.5 w-80 rounded-xl border border-border-warm-strong bg-white p-1.5 shadow-[0_4px_20px_-2px_rgba(30,35,32,0.06)]">
          <div className="px-2.5 py-1.5 font-metadata-caps text-[10px] uppercase tracking-wider text-outline">Add catalog resource</div>
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search catalog…"
            className="mx-1 mb-1 h-8 w-[calc(100%-0.5rem)] rounded-lg bg-surface-container-low px-2.5 text-xs text-on-surface placeholder:text-outline focus:outline-none focus:ring-1 focus:ring-primary"
          />
          <div className="max-h-72 overflow-y-auto">
            {available.map((b) => (
              <button
                key={b.id}
                type="button"
                role="menuitem"
                onClick={() => addToList(listId, b.id)}
                className="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left hover:bg-surface-paper"
              >
                <Icon name="add_circle" className="text-[18px] text-primary" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-xs font-medium text-on-surface">{b.title}</span>
                  <span className="block truncate text-[11px] text-outline">
                    {authorLine(b)} · {b.publicationYear}
                  </span>
                </span>
              </button>
            ))}
            {available.length === 0 && <p className="px-2.5 py-3 text-xs text-outline">No more catalog resources to add.</p>}
          </div>
        </div>
      )}
    </div>
  );
}

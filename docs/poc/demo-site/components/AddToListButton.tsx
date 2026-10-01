"use client";

import { useEffect, useRef, useState } from "react";
import type { ID } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";

interface Props {
  bookId: ID;
  label?: string;
  className: string;
  icon?: string;
  iconClassName?: string;
  /** which side the menu opens toward */
  align?: "left" | "right";
  /** classes for the outer wrapper (default "relative inline-block") */
  wrapperClassName?: string;
}

/** "Add to Reading List" — opens a small menu of the user's lists; each row toggles membership. */
export default function AddToListButton({ bookId, label = "Add to List", className, icon = "bookmark_add", iconClassName = "text-sm", align = "left", wrapperClassName = "relative inline-block" }: Props) {
  const { lists, isInList, addToList, removeFromList, listsContaining } = useLibrary();
  const [open, setOpen] = useState(false);
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

  const count = listsContaining(bookId).length;

  return (
    <div ref={ref} className={wrapperClassName}>
      <button type="button" aria-haspopup="menu" aria-expanded={open} className={className} onClick={() => setOpen((o) => !o)}>
        <Icon name={count ? "bookmark_added" : icon} className={iconClassName} />
        <span>{count ? `In ${count} List${count > 1 ? "s" : ""}` : label}</span>
      </button>
      {open && (
        <div
          role="menu"
          className={`absolute z-30 mt-1.5 w-64 rounded-xl border border-border-warm-strong bg-white p-1.5 shadow-[0_4px_20px_-2px_rgba(30,35,32,0.06)] ${
            align === "right" ? "right-0" : "left-0"
          }`}
        >
          <div className="px-2.5 py-1.5 font-metadata-caps text-[10px] uppercase tracking-wider text-outline">Add to reading list</div>
          {lists.map((l) => {
            const inList = isInList(l.id, bookId);
            return (
              <button
                key={l.id}
                type="button"
                role="menuitemcheckbox"
                aria-checked={inList}
                onClick={() => (inList ? removeFromList(l.id, bookId) : addToList(l.id, bookId))}
                className="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left hover:bg-surface-paper"
              >
                <span className={`flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded-[4px] border ${inList ? "border-primary bg-primary" : "border-border-warm-strong bg-white"}`}>
                  {inList && <Icon name="check" className="text-[14px] text-white" />}
                </span>
                <span className="min-w-0 truncate text-xs text-on-surface">{l.title}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

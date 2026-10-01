"use client";

import { useEffect, useState } from "react";
import Icon from "./Icon";

export default function NewListModal({ open, onClose, onCreate }: { open: boolean; onClose: () => void; onCreate: (title: string) => void }) {
  const [title, setTitle] = useState("");

  useEffect(() => {
    if (!open) return;
    setTitle("");
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  const submit = () => {
    const t = title.trim();
    if (!t) return;
    onCreate(t);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-label="Create new reading list">
      <div className="absolute inset-0 bg-[#1b1c19]/30" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-2xl border border-border-warm-strong bg-white p-6 shadow-[0_4px_20px_-2px_rgba(30,35,32,0.06)]">
        <div className="flex items-center justify-between">
          <h2 className="font-headline-sm text-xl font-semibold text-on-surface">Create New Reading List</h2>
          <button type="button" aria-label="Close" onClick={onClose} className="rounded-lg p-1.5 text-outline hover:bg-surface-paper">
            <Icon name="close" className="text-xl" />
          </button>
        </div>
        <label className="mt-5 block font-metadata-caps text-[11px] uppercase tracking-wider text-outline">List title</label>
        <input
          autoFocus
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="e.g. Field Methods Reading Group"
          className="mt-1.5 h-[42px] w-full rounded-lg border border-border-warm-strong bg-white px-3.5 text-sm text-on-surface placeholder:text-ink-muted focus:border-primary focus:outline-none focus:ring-0"
        />
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="h-10 rounded-lg border border-border-warm-strong bg-white px-5 font-label-lg text-sm hover:bg-surface-paper">
            Cancel
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={!title.trim()}
            className="h-10 rounded-lg border border-primary-dark bg-primary px-5 font-label-lg text-sm text-white hover:bg-primary-dark disabled:opacity-50"
          >
            Create List
          </button>
        </div>
      </div>
    </div>
  );
}

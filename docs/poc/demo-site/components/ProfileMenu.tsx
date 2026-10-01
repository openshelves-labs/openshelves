"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";
import Avatar from "./Avatar";

/** Avatar + name in the top bar; opens a small account menu (profile link, sign out). */
export default function ProfileMenu() {
  const { user } = useLibrary();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const item = "flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left font-label-lg text-sm text-on-surface-variant transition-colors hover:bg-[#F3F1EC] hover:text-on-surface";

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className={`flex items-center gap-2.5 rounded-lg py-1 pl-1 pr-1.5 transition-colors hover:bg-white/60 ${open ? "bg-white/60" : ""}`}
      >
        <Avatar name={user.name} initial={user.initial} src={user.avatarUrl} className="h-8 w-8 text-xs" />
        <div className="flex flex-col whitespace-nowrap text-left">
          <span className="font-label-lg text-xs font-semibold leading-none text-on-surface">{user.name}</span>
          <span className="mt-0.5 font-metadata-caps text-[10px] text-outline">{user.department}</span>
        </div>
        <Icon name="expand_more" className={`text-lg text-outline transition-transform ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div role="menu" className="absolute right-0 top-[calc(100%+8px)] z-50 w-72 overflow-hidden rounded-xl border border-border-warm bg-white shadow-lg">
          <div className="flex items-center gap-3 border-b border-border-warm p-4">
            <Avatar name={user.name} initial={user.initial} src={user.avatarUrl} className="h-11 w-11 text-base" />
            <div className="min-w-0">
              <div className="truncate font-title-editorial text-base font-semibold text-on-surface">{user.name}</div>
              <div className="truncate font-body-sm text-xs text-on-surface-variant">{user.email}</div>
              <div className="mt-1 font-metadata-caps text-[10px] uppercase tracking-wider tabular-nums text-outline">
                {user.accountType} · {user.cardNumber}
              </div>
            </div>
          </div>
          <div className="p-1.5">
            <Link href="/profile" role="menuitem" onClick={() => setOpen(false)} className={item}>
              <Icon name="person" className="text-[20px]" />
              View profile
            </Link>
            <Link href="/profile" role="menuitem" onClick={() => setOpen(false)} className={item}>
              <Icon name="settings" className="text-[20px]" />
              Settings
            </Link>
            <Link href="/sign-in" role="menuitem" onClick={() => setOpen(false)} className={item}>
              <Icon name="logout" className="text-[20px]" />
              Sign out
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

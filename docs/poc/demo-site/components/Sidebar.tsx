"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";
import Logo from "./Logo";

interface NavItem {
  label: string;
  icon: string;
  href?: string; // no href = section not part of this build (rendered inert)
  badge?: string | number;
  match?: (path: string) => boolean;
}

const ACTIVE = "bg-[#c6ebd3]/70 text-[#2F5D45] font-semibold border border-[#2F5D45]/20 shadow-sm";
const IDLE = "text-on-surface-variant hover:bg-white/60 hover:text-on-surface transition-colors";

function Group({ title, items, pathname }: { title: string; items: NavItem[]; pathname: string }) {
  return (
    <nav className="space-y-1">
      <div className="px-3 py-1 font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">{title}</div>
      {items.map((it) => {
        const active = it.href ? (it.match ? it.match(pathname) : pathname === it.href) : false;
        const inner = (
          <>
            <div className="flex items-center gap-2.5">
              <Icon name={it.icon} className="text-[20px]" />
              <span className="font-label-lg text-sm">{it.label}</span>
            </div>
            {it.badge !== undefined && (
              <span className="rounded-full border border-border-warm bg-white/70 px-2 py-0.5 font-metadata-caps text-[11px] text-on-surface-variant">{it.badge}</span>
            )}
          </>
        );
        const cls = `flex items-center justify-between rounded-lg px-3 py-2 ${active ? ACTIVE : IDLE}`;
        return it.href ? (
          <Link key={it.label} href={it.href} aria-current={active ? "page" : undefined} className={cls}>
            {inner}
          </Link>
        ) : (
          <a key={it.label} href="#" onClick={(e) => e.preventDefault()} className={cls}>
            {inner}
          </a>
        );
      })}
    </nav>
  );
}

export default function Sidebar() {
  const pathname = usePathname();
  const { loans, holds, lists, outstandingBalance, newArrivalsCount, recommendedCount } = useLibrary();

  const discover: NavItem[] = [
    { label: "Home", icon: "explore", href: "/" },
    { label: "Browse Catalog", icon: "menu_book", href: "/catalog", match: (p) => p.startsWith("/catalog") },
    { label: "New Arrivals", icon: "auto_awesome", href: "/new-arrivals", badge: newArrivalsCount },
    { label: "Recommended", icon: "star", badge: recommendedCount },
  ];
  const library: NavItem[] = [
    { label: "Current Loans", icon: "schedule", href: "/loans", badge: loans.length },
    { label: "Holds", icon: "bookmark_added", href: "/loans#holds", badge: holds.length, match: () => false },
    { label: "Reading Lists", icon: "folder", href: "/lists", badge: lists.length, match: (p) => p.startsWith("/lists") },
    { label: "Reading History", icon: "history", href: "/history" },
    { label: "Fines & Fees", icon: "receipt_long", href: "/fines", badge: `$${outstandingBalance.toFixed(2)}` },
  ];
  const collections: NavItem[] = [
    { label: "Journals & Serials", icon: "auto_stories", href: "/journals" },
    { label: "Digital Resources", icon: "database", href: "/digital-resources" },
  ];

  return (
    <>
    {/* in-flow spacer reserves the sidebar's width; the aside itself is fixed so it always spans the full viewport height (no vh maths, so the footer badge stays pinned to the bottom at any zoom) */}
    <div aria-hidden className="w-64 shrink-0" />
    <aside className="fixed inset-y-0 left-0 z-40 flex w-64 select-none flex-col border-r border-border-warm bg-[#F3F1EC]">
      <Link href="/" className="flex h-16 shrink-0 items-center gap-3 border-b border-border-warm px-5">
          <div className="h-8 w-8 shrink-0">
            <Logo size={32} />
          </div>
          <div className="flex flex-col">
            <span className="font-title-editorial text-lg font-bold leading-tight tracking-tight text-on-surface">OpenShelves</span>
            <span className="font-metadata-caps text-[10px] uppercase tracking-wider text-outline">Central University Library</span>
          </div>
      </Link>
      <div className="min-h-0 flex-1 space-y-6 overflow-y-auto p-4">
        <Group title="Discover" items={discover} pathname={pathname} />
        <Group title="My Library" items={library} pathname={pathname} />
        <Group title="Collections" items={collections} pathname={pathname} />
      </div>
      <div className="shrink-0 border-t border-border-warm p-4">
        <div className="flex items-start gap-2.5 rounded-lg border border-border-warm bg-white/80 p-3">
          <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
          <div className="flex flex-col">
            <span className="font-label-lg text-xs font-semibold leading-tight text-on-surface">Central University Library Network</span>
            <span className="mt-0.5 font-metadata-caps text-[10px] font-medium text-primary">Access Active &amp; Verified</span>
          </div>
        </div>
      </div>
    </aside>
    </>
  );
}

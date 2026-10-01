"use client";

import Link from "next/link";
import { useLibrary } from "@/lib/store";
import { daysUntil } from "@/lib/dates";
import Icon from "@/components/Icon";
import { DUE_SOON_DAYS } from "@/components/LoanRow";

interface Action {
  key: string;
  icon: string;
  title: string;
  text: string;
  href: string;
}

const CARD =
  "group flex items-start gap-3.5 rounded-xl border border-border-warm bg-white p-4 transition-all hover:border-primary/40";

/** Four quick-action cards at the top of Home; copy is driven by client state. */
export default function QuickActions() {
  const { loans, holds, outstandingBalance } = useLibrary();
  const dueSoon = loans.filter((l) => daysUntil(l.dueDate) <= DUE_SOON_DAYS).length;
  const ready = holds.filter((h) => h.status === "ready").length;
  const actions: Action[] = [
    { key: "renew", icon: "autorenew", title: "Renew Loans", text: `${loans.length} items checked out • ${dueSoon} due soon`, href: "/loans" },
    { key: "hold", icon: "bookmark_added", title: "Hold Ready", text: `${ready} ${ready === 1 ? "item" : "items"} ready at Main Circulation`, href: "/loans#holds" },
    { key: "search", icon: "search", title: "Search Catalog", text: "Browse 2.4M monographs & journals", href: "/catalog" },
    {
      key: "fines",
      icon: "receipt_long",
      title: "Pay Fines",
      text: `$${outstandingBalance.toFixed(2)} current balance${outstandingBalance === 0 ? " • Good standing" : ""}`,
      href: "/fines",
    },
  ];
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {actions.map((a) => (
        <Link key={a.key} href={a.href} className={CARD}>
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#c6ebd3]/60 text-primary transition-colors group-hover:bg-[#c6ebd3]">
            <Icon name={a.icon} className="text-xl" />
          </div>
          <div className="min-w-0">
            <h3 className="font-title-editorial text-sm font-semibold text-on-surface transition-colors group-hover:text-primary">{a.title}</h3>
            <p className="mt-0.5 line-clamp-2 font-body-sm text-xs text-on-surface-variant">{a.text}</p>
          </div>
        </Link>
      ))}
    </div>
  );
}

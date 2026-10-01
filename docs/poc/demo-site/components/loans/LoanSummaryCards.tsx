"use client";

import { useLibrary } from "@/lib/store";
import { daysUntil } from "@/lib/dates";
import Icon from "@/components/Icon";
import { DUE_SOON_DAYS } from "@/components/LoanRow";

function Card({ label, icon, value, valueClass = "text-on-surface", children }: { label: string; icon: string; value: string; valueClass?: string; children: React.ReactNode }) {
  return (
    <div className="relative flex flex-col justify-between overflow-hidden rounded-xl bg-surface-container-lowest p-space-md shadow-sm">
      <div className="flex items-start justify-between">
        <span className="font-metadata-caps text-metadata-caps uppercase tracking-wider text-outline">{label}</span>
        <Icon name={icon} className="text-xl text-primary" />
      </div>
      <div className="mt-3">
        <div className={`font-headline-md text-headline-md font-semibold ${valueClass}`}>{value}</div>
        {children}
      </div>
    </div>
  );
}

/** Four metric cards; every figure comes from the store. */
export default function LoanSummaryCards() {
  const { loans, holds, lists, outstandingBalance } = useLibrary();
  const soon = loans.filter((l) => daysUntil(l.dueDate) <= DUE_SOON_DAYS);
  const soonest = soon.length ? Math.min(...soon.map((l) => daysUntil(l.dueDate))) : null;
  const ready = holds.filter((h) => h.status === "ready").length;
  const saved = lists.reduce((s, l) => s + l.itemCount, 0);
  return (
    <div className="mb-space-lg grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-4">
      <Card label="Current Checkouts" icon="auto_stories" value={`${loans.length} ${loans.length === 1 ? "Book" : "Books"}`}>
        {soonest !== null ? (
          <div className="mt-1 inline-flex items-center gap-1.5 font-metadata-caps text-metadata-caps font-medium text-error">
            <span className="h-1.5 w-1.5 rounded-full bg-error" />
            {soon.length} {soon.length === 1 ? "title" : "titles"} due soon ({soonest} days)
          </div>
        ) : (
          <div className="mt-1 font-metadata-caps text-metadata-caps text-on-surface-variant">Nothing due soon</div>
        )}
      </Card>
      <Card label="Active Holds" icon="bookmark_added" value={`${holds.length} ${holds.length === 1 ? "Item" : "Items"}`}>
        <div className="mt-1 inline-flex items-center gap-1.5 font-metadata-caps text-metadata-caps font-medium text-secondary">
          <span className={`h-1.5 w-1.5 rounded-full bg-primary ${ready ? "animate-pulse" : ""}`} />
          {ready ? "Ready for desk pickup" : "In queue"}
        </div>
      </Card>
      <Card label="Archival Folios" icon="folder_managed" value={`${lists.length} Lists`}>
        <div className="mt-1 font-metadata-caps text-metadata-caps text-on-surface-variant">{saved} saved citations &amp; notes</div>
      </Card>
      <Card label="Fines & Standing" icon="verified_user" value={`$${outstandingBalance.toFixed(2)}`} valueClass="text-primary">
        <div className="mt-1 truncate font-metadata-caps text-metadata-caps text-on-surface-variant">Grace status active</div>
      </Card>
    </div>
  );
}

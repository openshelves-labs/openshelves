import Link from "next/link";
import type { Fine } from "@/types";
import { formatShortDate } from "@/lib/dates";
import Icon from "@/components/Icon";
import { money } from "./money";

export type FineFilter = "all" | "recall" | "digital" | "waived";

const REASON_ICON: Partial<Record<Fine["reasonKind"], string>> = {
  overdue_recall: "notification_important",
  processing_fee: "document_scanner",
};

const CHECK =
  "w-4 h-4 rounded text-primary focus:ring-primary bg-surface-container-lowest border-surface-variant cursor-pointer";

function StatusPill({ status }: { status: Fine["status"] }) {
  const tone =
    status === "unpaid" ? "bg-error-container text-error" : "bg-secondary-container text-on-secondary-container";
  const label = status === "unpaid" ? "Unpaid" : status === "waived" ? "Waived" : "Paid";
  return <span className={`px-2 py-0.5 rounded-full text-[10px] font-label-md font-semibold ${tone}`}>{label}</span>;
}

function Row({ fine, checked, onToggle }: { fine: Fine; checked: boolean; onToggle: () => void }) {
  const waived = fine.status === "waived";
  const paid = fine.status === "paid";
  const icon = REASON_ICON[fine.reasonKind];
  const recall = fine.reasonKind === "overdue_recall";
  const dateLine = recall && fine.daysOverdue
    ? `${fine.daysOverdue} days overdue`
    : waived
      ? `Resolved ${formatShortDate(fine.assessedOn)}`
      : `Assessed ${formatShortDate(fine.assessedOn)}`;
  const actions = fine.actions ?? [];
  return (
    <tr
      className={`hover:bg-surface-container/40 transition-colors ${
        waived ? "bg-surface-container/20 opacity-85" : paid ? "opacity-70" : ""
      }`}
    >
      <td className="py-4 px-4 align-top">
        {waived || paid ? (
          <input
            type="checkbox"
            disabled
            aria-label={`Select ${fine.title}`}
            className="w-4 h-4 rounded text-outline-variant bg-surface-container-high border-surface-variant opacity-40 cursor-not-allowed"
          />
        ) : (
          <input
            type="checkbox"
            checked={checked}
            onChange={onToggle}
            aria-label={`Select ${fine.title}`}
            className={CHECK}
          />
        )}
      </td>
      <td className="py-4 px-3 align-top max-w-xs">
        <div className="space-y-1">
          <span
            className={`font-title-editorial ${waived ? "font-medium" : "font-semibold"} text-on-surface block leading-snug`}
          >
            {fine.bookId ? (
              <Link href={`/catalog/${fine.bookId}`} className="hover:text-primary transition-colors">
                {fine.title}
              </Link>
            ) : (
              fine.title
            )}
          </span>
          {fine.detail && <span className="font-body-sm text-on-surface-variant block">{fine.detail}</span>}
          {fine.catalogRefs && (
            <div className="flex items-center gap-2 pt-1 font-mono text-[11px] text-outline">
              {fine.catalogRefs.map((r, i) => (
                <span key={r} className="contents">
                  {i > 0 && <span>•</span>}
                  <span>{r}</span>
                </span>
              ))}
            </div>
          )}
        </div>
      </td>
      <td className="py-4 px-3 align-top">
        <div className="space-y-1">
          <span
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-label-md font-medium ${
              recall ? "bg-amber-100/70 text-amber-900" : "bg-surface-container-high text-on-surface-variant"
            }`}
          >
            {icon && <Icon name={icon} className="text-[13px]" />}
            {fine.reasonLabel}
          </span>
          <span
            className={`text-xs font-body-sm block ${waived ? "text-secondary font-medium" : "text-on-surface-variant"}`}
          >
            {fine.reasonNote}
          </span>
        </div>
      </td>
      <td className={`py-4 px-3 align-top text-xs font-body-sm ${waived ? "text-outline" : "text-on-surface"}`}>
        <span className={recall ? "font-semibold text-error" : ""}>{dateLine}</span>
        <span className="text-outline block text-[11px]">{fine.accrualLabel}</span>
      </td>
      <td className="py-4 px-3 align-top text-right font-mono font-semibold text-on-surface">
        {waived && fine.originalAmount !== undefined ? (
          <>
            <span className="line-through text-outline text-xs mr-1 font-normal">{money(fine.originalAmount)}</span>
            <span className="font-semibold text-secondary">{money(fine.amount)}</span>
          </>
        ) : (
          money(fine.amount)
        )}
      </td>
      <td className="py-4 px-3 align-top text-center">
        <StatusPill status={fine.status} />
      </td>
      <td className="py-4 px-4 align-top text-right">
        {actions[0] && (
          <button
            type="button"
            className={`text-xs font-label-md hover:underline block ml-auto ${
              waived ? "text-on-surface-variant" : "text-primary"
            }`}
          >
            {actions[0]}
          </button>
        )}
        {actions[1] && (
          <button type="button" className="text-[11px] font-body-sm text-outline hover:text-on-surface block ml-auto pt-1">
            {actions[1]}
          </button>
        )}
      </td>
    </tr>
  );
}

export default function AssessmentsTable({
  rows,
  selected,
  filter,
  counts,
  selectableCount,
  onFilter,
  onToggle,
  onToggleAll,
}: {
  rows: Fine[];
  selected: Set<string>;
  filter: FineFilter;
  counts: Record<FineFilter, number>;
  selectableCount: number;
  onFilter: (f: FineFilter) => void;
  onToggle: (id: string) => void;
  onToggleAll: () => void;
}) {
  const chips: { key: FineFilter; label: string }[] = [
    { key: "all", label: "All" },
    { key: "recall", label: "Overdue Recall" },
    { key: "digital", label: "Digital Fees" },
    { key: "waived", label: "Waived" },
  ];
  const allChecked = selectableCount > 0 && selected.size === selectableCount;
  return (
    <div className="lg:col-span-8 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white border border-[#E5E0D8] p-4 rounded-xl">
        <div className="flex items-center gap-2">
          <Icon name="gavel" className="text-primary text-xl" />
          <h2 className="font-headline-sm text-on-surface">Current Assessments</h2>
          <span className="font-metadata-caps bg-surface-container-high text-on-surface-variant px-2.5 py-0.5 rounded-full">
            {counts.all} {counts.all === 1 ? "Item" : "Items"}
          </span>
        </div>
        <div className="flex items-center gap-1.5 overflow-x-auto text-xs font-label-md">
          {chips.map((c) => (
            <button
              key={c.key}
              type="button"
              onClick={() => onFilter(c.key)}
              className={`px-2.5 py-1 rounded-full whitespace-nowrap transition-colors ${
                filter === c.key
                  ? "bg-primary text-on-primary font-medium"
                  : "bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
              }`}
            >
              {c.label} ({counts[c.key]})
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white border border-[#E5E0D8] rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-surface-container-low text-on-surface-variant font-metadata-caps uppercase tracking-wider">
                <th className="py-3 px-4 w-10">
                  <input
                    type="checkbox"
                    checked={allChecked}
                    disabled={selectableCount === 0}
                    onChange={onToggleAll}
                    aria-label="Select all assessments"
                    className={CHECK}
                  />
                </th>
                <th className="py-3 px-3">Item / Catalog Reference</th>
                <th className="py-3 px-3">Reason / Details</th>
                <th className="py-3 px-3">Days / Accrual</th>
                <th className="py-3 px-3 text-right">Amount</th>
                <th className="py-3 px-3 text-center">Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-variant font-body-md">
              {rows.map((f) => (
                <Row key={f.id} fine={f} checked={selected.has(f.id)} onToggle={() => onToggle(f.id)} />
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-8 px-4 text-center text-xs font-body-sm text-outline">
                    No assessments in this view.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="p-4 bg-surface-container-low flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs font-body-sm text-on-surface-variant">
          <div className="flex items-center gap-2">
            <Icon name="receipt_long" className="text-primary text-base" />
            <span>Items not cleared within 30 days are automatically forwarded to University Bursar withholding.</span>
          </div>
          <div className="flex items-center gap-4 shrink-0">
            <button type="button" className="text-primary hover:underline font-label-md">
              Dispute Procedure
            </button>
            <button type="button" className="text-primary hover:underline font-label-md">
              Appeal Board Schedule
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

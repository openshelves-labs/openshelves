import type { Payment } from "@/types";
import { formatDate } from "@/lib/dates";
import Icon from "@/components/Icon";
import { money } from "./money";

export default function PaymentHistory({ payments }: { payments: Payment[] }) {
  return (
    <div className="space-y-4 pt-4 border-t border-surface-variant" id="payment-history">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h2 className="font-headline-sm text-on-surface">Settled Payment History (2023–2024 Academic Term)</h2>
          <p className="font-body-sm text-on-surface-variant">
            Complete audit ledger of finalized circulation settlements, indemnities, and university reimbursements.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-metadata-caps text-outline uppercase">Filter:</span>
          <select
            defaultValue="Michaelmas Term 2024"
            aria-label="Filter by term"
            className="h-8 pl-2 pr-6 text-xs font-label-md bg-surface-container-lowest rounded border-surface-variant text-on-surface"
          >
            <option>All Past Terms</option>
            <option>Michaelmas Term 2024</option>
            <option>Easter Term 2024</option>
            <option>Lent Term 2024</option>
          </select>
        </div>
      </div>

      <div className="bg-white border border-[#E5E0D8] rounded-xl divide-y divide-[#E5E0D8] overflow-hidden">
        {payments.map((p) => (
          <div
            key={p.id}
            className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-surface-container/20 transition-colors"
          >
            <div className="flex items-start gap-4">
              <div className="w-10 h-10 rounded-lg bg-surface-container flex items-center justify-center shrink-0 text-primary">
                <Icon name={p.icon ?? "receipt"} className="text-xl" />
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-label-lg text-sm text-on-surface font-semibold">{p.title}</span>
                  <span className="font-mono text-xs text-outline font-normal">#{p.transactionRef}</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-label-md bg-secondary-container text-on-secondary-container font-medium">
                    Settled ({p.receiptRef})
                  </span>
                </div>
                <p className="text-xs font-body-sm text-on-surface-variant">
                  Paid on {formatDate(p.paidOn)} • {p.method} • {p.desk}
                </p>
              </div>
            </div>
            <div className="flex items-center justify-between md:justify-end gap-6 shrink-0">
              <div className="text-right">
                <span className="font-mono font-bold text-on-surface block text-base">{money(p.amount)}</span>
                {p.taxExempt && <span className="text-[11px] font-metadata-caps text-secondary uppercase">Tax Exempt</span>}
              </div>
              <button
                type="button"
                className="h-8 px-3 inline-flex items-center gap-1.5 bg-surface-container hover:bg-surface-container-high rounded text-xs font-label-md text-primary transition-colors"
              >
                <Icon name="download" className="text-sm" />
                <span>PDF Receipt</span>
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

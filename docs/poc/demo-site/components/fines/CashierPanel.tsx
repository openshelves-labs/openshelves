import type { User } from "@/types";
import Icon from "@/components/Icon";
import { money } from "./money";

export const REMITTANCE_SOURCES = [
  { id: "grant", historyLabel: "Departmental Research Allowance Account (•••• 4092)" },
  { id: "smartcard", historyLabel: "Cambridge Scholar SmartCard" },
  { id: "card", historyLabel: "Personal Card (Visa, MC, Apple Pay)" },
] as const;

const OPT = "flex items-start gap-3 p-3 rounded-lg cursor-pointer transition-colors";
const RADIO = "mt-1 text-primary focus:ring-primary w-4 h-4";

export default function CashierPanel({
  user,
  selectedTotal,
  selectedCount,
  recall,
  ill,
  sourceId,
  onSource,
  onPay,
}: {
  user: User;
  selectedTotal: number;
  selectedCount: number;
  recall: number;
  ill: number;
  sourceId: string;
  onSource: (id: string) => void;
  onPay: () => void;
}) {
  const empty = selectedTotal === 0;
  const optClass = (id: string) =>
    sourceId === id
      ? `${OPT} bg-surface-container/60 hover:bg-surface-container border-2 border-primary`
      : `${OPT} bg-surface-container/30 hover:bg-surface-container/60 border border-transparent`;
  return (
    <div className="lg:col-span-4 space-y-5">
      <div className="bg-white border border-[#E5E0D8] p-6 rounded-xl space-y-6">
        <div className="flex items-center justify-between pb-3 border-b border-surface-variant">
          <div className="space-y-0.5">
            <span className="font-metadata-caps text-outline uppercase">Circulation Cashier</span>
            <h3 className="font-headline-sm text-on-surface">Settle Balance</h3>
          </div>
          <Icon name="credit_card" className="text-primary text-2xl" />
        </div>

        <div className="bg-surface-container p-4 rounded-xl text-center space-y-1">
          <span className="text-xs font-metadata-caps text-outline uppercase tracking-wider block">
            Selected Total for Immediate Clearance
          </span>
          <div className="flex items-center justify-center gap-1">
            <span className="text-3xl font-bold font-mono text-on-surface">{money(selectedTotal)}</span>
          </div>
          <span className="text-xs font-body-sm text-secondary">
            {selectedCount} {selectedCount === 1 ? "assessment" : "assessments"} selected
          </span>
        </div>

        <div className="space-y-2 text-xs font-body-sm">
          <div className="flex justify-between text-on-surface-variant">
            <span>Recall Penalties:</span>
            <span className="font-mono text-on-surface">{money(recall)}</span>
          </div>
          <div className="flex justify-between text-on-surface-variant">
            <span>ILL Digital Fulfillment:</span>
            <span className="font-mono text-on-surface">{money(ill)}</span>
          </div>
          <div className="flex justify-between text-on-surface-variant">
            <span>University Statutory Surcharge:</span>
            <span className="font-mono text-secondary">EXEMPT (Student)</span>
          </div>
          <div className="pt-2 border-t border-surface-variant flex justify-between font-label-lg text-sm text-on-surface">
            <span>Payable Today:</span>
            <span className="font-mono font-bold text-primary">{money(selectedTotal)}</span>
          </div>
        </div>

        <div className="space-y-3" role="radiogroup" aria-label="Remittance source">
          <span className="text-xs font-label-md text-on-surface font-semibold block">Select Remittance Source</span>
          <label className={optClass("grant")}>
            <input
              type="radio"
              name="payment-method"
              className={RADIO}
              checked={sourceId === "grant"}
              onChange={() => onSource("grant")}
            />
            <div className="flex flex-col text-xs font-body-sm">
              <span className="font-label-md font-semibold text-on-surface">Departmental Research Allowance</span>
              <span className="text-outline font-mono">
                •••• 4092 • {user.department}
              </span>
              <span className="text-primary font-medium text-[11px] mt-0.5">Pre-authorized institutional charge</span>
            </div>
          </label>
          <label className={optClass("smartcard")}>
            <input
              type="radio"
              name="payment-method"
              className={RADIO}
              checked={sourceId === "smartcard"}
              onChange={() => onSource("smartcard")}
            />
            <div className="flex flex-col text-xs font-body-sm">
              <span className="font-label-md font-semibold text-on-surface">Cambridge Scholar SmartCard</span>
              <span className="text-outline">
                Stored Library Stacks Balance: <strong className="text-on-surface font-mono">$84.20</strong>
              </span>
            </div>
          </label>
          <label className={optClass("card")}>
            <input
              type="radio"
              name="payment-method"
              className={RADIO}
              checked={sourceId === "card"}
              onChange={() => onSource("card")}
            />
            <div className="flex flex-col text-xs font-body-sm">
              <span className="font-label-md font-semibold text-on-surface">Personal Card (Visa, MC, Apple Pay)</span>
              <span className="text-outline">Encrypted via Cambridge University Gateway</span>
            </div>
          </label>
        </div>

        <button
          type="button"
          onClick={onPay}
          disabled={empty}
          className={`w-full h-11 bg-primary text-on-primary font-label-lg text-sm rounded-lg hover:bg-primary-container transition-all flex items-center justify-center gap-2 shadow-sm ${
            empty ? "opacity-50 cursor-not-allowed" : ""
          }`}
        >
          <Icon name="lock" className="text-base" />
          <span>Authorize &amp; Pay {money(selectedTotal)}</span>
        </button>
        <p className="text-[11px] font-body-sm text-outline leading-tight text-center">
          Instant clearing. All circulation holds removed immediately upon processing. Official signed receipt
          forwarded to <span className="font-mono text-on-surface">{user.email}</span>.
        </p>

        <div className="pt-3 border-t border-surface-variant text-center">
          <button type="button" className="inline-flex items-center gap-1 text-xs font-label-md text-primary hover:underline">
            <Icon name="assignment_return" className="text-sm" />
            <span>Download Form FIN-4 (Dept. Reimbursement)</span>
          </button>
        </div>
      </div>

      <div className="bg-white border border-[#E5E0D8] border-l-4 border-l-primary p-5 rounded-xl space-y-2">
        <span className="font-metadata-caps text-[10px] text-outline uppercase tracking-wider block">
          Ordinance XII • Cambridge University Library
        </span>
        <blockquote className="font-title-editorial text-sm text-on-surface italic leading-relaxed">
          “Library recall penalties preserve the democratic circulation of rare and primary scholarly materials. All
          accrued recall fines directly support the Cambridge Rare Book Preservation Fellowship.”
        </blockquote>
        <span className="font-label-md text-[11px] text-on-surface-variant block font-medium">
          — Board of Syndics, Library Regulations § 14.b
        </span>
      </div>
    </div>
  );
}

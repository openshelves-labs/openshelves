import Icon from "@/components/Icon";
import { money } from "./money";

const CARD = "bg-white border border-[#E5E0D8] p-5 rounded-xl flex flex-col justify-between hover:border-primary/40 transition-all";
const LABEL = "font-metadata-caps text-outline uppercase tracking-wider";
const FOOT = "pt-4 border-t border-surface-variant flex items-center justify-between text-xs font-body-sm text-on-surface-variant";

export default function SummaryCards({
  balance,
  settledTotal,
  settledCount,
  recallCount,
  illCount,
  borrowLimit,
}: {
  balance: number;
  settledTotal: number;
  settledCount: number;
  recallCount: number;
  illCount: number;
  borrowLimit: number;
}) {
  const cleared = balance === 0;
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 py-8">
      <div className={`${CARD} relative overflow-hidden`}>
        <div className="absolute -top-3 -right-3 w-16 h-16 bg-error/5 rounded-full pointer-events-none" />
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className={LABEL}>Outstanding Balance</span>
            {cleared ? (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-label-md font-semibold bg-secondary-container text-on-secondary-container flex items-center gap-1">
                <Icon name="verified" className="text-xs" /> Cleared
              </span>
            ) : (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-label-md font-semibold bg-error-container text-error flex items-center gap-1">
                <Icon name="pending" className="text-xs" /> Payment Pending
              </span>
            )}
          </div>
          <div className="pt-2 flex items-baseline gap-2">
            <span className="font-display-lg text-on-surface tracking-tight">{money(balance)}</span>
            <span className="font-metadata-caps text-outline">USD</span>
          </div>
        </div>
        <div className={FOOT}>
          <span>
            {cleared ? "No open assessments" : `${recallCount} Recall • ${illCount} ILL Processing`}
          </span>
          {!cleared && <span className="text-error font-medium">Due in 5 days</span>}
        </div>
      </div>

      <div className={CARD}>
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className={LABEL}>Settled (2023–24)</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-label-md font-semibold bg-secondary-container text-on-secondary-container flex items-center gap-1">
              <Icon name="verified" className="text-xs" /> Cleared
            </span>
          </div>
          <div className="pt-2 flex items-baseline gap-2">
            <span className="font-display-lg text-on-surface tracking-tight">{money(settledTotal)}</span>
            <span className="font-metadata-caps text-outline">USD</span>
          </div>
        </div>
        <div className={FOOT}>
          <span>{settledCount} Completed Transactions</span>
          <a href="#payment-history" className="text-primary hover:underline font-medium">
            History →
          </a>
        </div>
      </div>

      <div className={CARD}>
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className={LABEL}>Borrowing Status</span>
            <span className="w-2.5 h-2.5 rounded-full bg-primary ring-4 ring-secondary-container/50" />
          </div>
          <div className="pt-2 flex items-center gap-2">
            <span className="font-headline-md text-primary font-semibold">Good Standing</span>
          </div>
        </div>
        <div className={FOOT}>
          <span>Borrowing privileges intact</span>
          <span className="text-outline font-mono">Limit: {money(borrowLimit)}</span>
        </div>
      </div>

      <div className={CARD}>
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className={LABEL}>Audit &amp; Renewal</span>
            <Icon name="event_repeat" className="text-outline text-lg" />
          </div>
          <div className="pt-2">
            <span className="font-headline-md text-on-surface">Nov 15, 2024</span>
          </div>
        </div>
        <div className={FOOT}>
          <span>Automated tenure cycle</span>
          <span className="text-secondary font-medium">Student Exemption Active</span>
        </div>
      </div>
    </div>
  );
}

"use client";

import type { Book, Hold } from "@/types";
import { formatWeekdayDate } from "@/lib/dates";
import Icon from "@/components/Icon";

/** One hold on Home: "ready for desk pickup" layout, or a queue-status variant for unfilled holds. */
export default function HomeHoldCard({ hold, book }: { hold: Hold; book: Book }) {
  const ready = hold.status === "ready";
  const lead = book.contributors[0]?.name ?? "";
  return (
    <div className="rounded-xl border border-border-warm bg-white p-5 transition-all hover:border-primary/40">
      <div className="flex flex-col justify-between gap-5 md:flex-row md:items-center">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-primary/20 bg-[#c6ebd3]/70 text-primary">
            <Icon name={ready ? "local_library" : "hourglass_top"} className="text-2xl" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-metadata-caps text-[11px] font-bold uppercase tracking-wider text-primary">
                {ready ? "Ready for Desk Pickup" : `In Queue • Position ${hold.queuePosition} of ${hold.queueLength}`}
              </span>
              {ready && hold.heldUntil && (
                <>
                  <span className="text-xs text-outline">•</span>
                  <span className="font-metadata-caps text-[11px] text-outline">Held until {formatWeekdayDate(hold.heldUntil)}</span>
                </>
              )}
            </div>
            <h3 className="mt-0.5 font-title-editorial text-lg font-semibold text-on-surface">{book.title}</h3>
            <p className="font-body-sm text-xs text-on-surface-variant">by {lead}</p>
            <p className="mt-1.5 font-body-sm text-xs text-on-surface">
              {ready ? "Waiting at " : "Pickup at "}
              <span className="font-semibold text-primary">{hold.pickupLocation}</span>
              {hold.pickupDetail ? ` (${hold.pickupDetail}).` : "."}
            </p>
          </div>
        </div>
        {ready && (
          <div className="flex shrink-0 items-center gap-2.5 self-end md:self-center">
            <button type="button" className="rounded-lg border border-border-warm bg-surface-paper px-4 py-2 font-label-lg text-xs text-on-surface transition-colors hover:bg-stone-100">
              Pickup Desk Hours
            </button>
            <button type="button" className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 font-label-lg text-xs text-white transition-colors hover:bg-[#214f38]">
              <Icon name="qr_code" className="text-base" />
              <span>View Pickup Code</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

"use client";

import type { Book, Hold } from "@/types";
import { daysUntil, formatDate } from "@/lib/dates";
import Icon from "@/components/Icon";

/** Hold entry on the Loans page (ready-for-pickup or queued). Cancel calls back into the store. */
export default function HoldCard({ hold, book, onCancel }: { hold: Hold; book: Book; onCancel: (hold: Hold, book: Book) => void }) {
  const ready = hold.status === "ready";
  const lead = book.contributors[0]?.name ?? "";
  const remaining = hold.heldUntil ? daysUntil(hold.heldUntil) : null;
  const where = book.copies[0]?.branchName ?? hold.pickupLocation;
  return (
    <div className="relative overflow-hidden rounded-xl bg-surface-container-lowest p-space-md shadow-sm sm:p-space-lg">
      <div className="flex flex-col justify-between gap-space-md lg:flex-row lg:items-center">
        <div className="flex items-start gap-space-md">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-secondary-fixed text-primary">
            <Icon name={ready ? "local_library" : "hourglass_top"} className="text-2xl" />
          </div>
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary-fixed px-2.5 py-0.5 font-metadata-caps text-metadata-caps font-bold uppercase tracking-wider text-primary">
                {ready && <span className="h-2 w-2 animate-ping rounded-full bg-primary" />}
                {ready ? "Ready for Pickup at Circulation Desk" : "Waiting in Queue"}
              </span>
              <span className="font-metadata-caps text-metadata-caps text-outline">
                Queue Position: {hold.queuePosition} of {hold.queueLength}
              </span>
            </div>
            <h3 className="font-title-editorial text-title-editorial font-semibold leading-snug text-on-surface">{book.title}</h3>
            <p className="font-body-md text-body-sm text-on-surface-variant">
              by <span className="font-medium text-on-surface">{lead}</span> • Call: <span className="font-mono text-xs">{book.callNumber}</span>
            </p>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 pt-1 font-body-sm text-body-sm text-on-surface-variant">
              <span className="inline-flex items-center gap-1 text-primary">
                <Icon name="pin_drop" className="text-base" />
                {where}
              </span>
              {hold.heldUntil && remaining !== null && (
                <>
                  <span className="text-outline">/</span>
                  <span className="inline-flex items-center gap-1 font-medium text-on-surface">
                    <Icon name="event_available" className="text-base text-outline" />
                    Held until {formatDate(hold.heldUntil)} ({remaining} days remaining)
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-space-sm pt-2 lg:pt-0">
          <button type="button" className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-container px-space-md font-label-lg text-label-md text-on-primary transition-colors hover:bg-primary">
            <Icon name="schedule" className="text-base" />
            <span>Pickup Desk Hours</span>
          </button>
          <button type="button" className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-surface-container-high px-space-md font-label-lg text-label-md text-on-surface transition-colors hover:bg-surface-variant">
            <Icon name="swap_horiz" className="text-base text-outline" />
            <span>Transfer to Central</span>
          </button>
          <button
            type="button"
            onClick={() => onCancel(hold, book)}
            className="h-9 rounded-lg px-3 font-label-lg text-label-md text-error transition-colors hover:bg-error-container"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

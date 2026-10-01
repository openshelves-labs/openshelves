import type { Serial } from "@/types";
import Icon from "@/components/Icon";

export interface RecentRow {
  serial: Serial;
  viewedLabel: string;
}

/** "Recently Consulted" desk ledger — data-driven, clearable. */
export default function RecentPanel({ rows, onClear }: { rows: RecentRow[]; onClear: () => void }) {
  return (
    <section className="bg-surface-container-lowest rounded-xl p-5 shadow-sm space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon name="history_toggle_off" className="text-primary text-lg" />
          <h3 className="font-headline-sm text-on-surface font-semibold">Recently Consulted</h3>
        </div>
        <span className="font-metadata-caps uppercase text-outline">Desk Ledger</span>
      </div>
      {rows.length === 0 ? (
        <p className="rounded-lg bg-surface-container-low p-3 font-body-sm text-on-surface-variant">
          No recently consulted serials. Use Browse Issues on any record to build your ledger.
        </p>
      ) : (
        <ul className="space-y-3">
          {rows.map(({ serial, viewedLabel }) => (
            <li key={serial.id} className="p-3 rounded-lg bg-surface-container-low hover:bg-surface-container-high/60 transition-colors">
              <a className="block space-y-1" href="#">
                <h4 className="font-title-editorial text-on-surface font-medium hover:text-primary transition-colors leading-snug">
                  {serial.title}
                </h4>
                <div className="flex items-center justify-between font-body-sm text-on-surface-variant">
                  <span className="tabular-nums">ISSN: {serial.issn}</span>
                  <span className="text-xs text-outline">{viewedLabel}</span>
                </div>
              </a>
            </li>
          ))}
        </ul>
      )}
      <div className="pt-1 flex items-center justify-between">
        <button type="button" onClick={onClear} className="text-primary hover:underline font-label-md transition-colors">
          Clear recent history
        </button>
        <Icon name="manage_history" className="text-outline text-sm" />
      </div>
    </section>
  );
}

import type { LedgerEntry } from "@/lib/api";
import Icon from "@/components/Icon";
import { ledgerStatus, ledgerTitle } from "./ledger";

export default function LedgerSection({ entries }: { entries: LedgerEntry[] }) {
  return (
    <section className="space-y-4 rounded-xl border border-[#E5E0D8] bg-white p-6">
      <div className="flex flex-col justify-between gap-2 border-b border-[#E5E0D8] pb-3 sm:flex-row sm:items-center">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#c6ebd3]/60 text-primary">
            <Icon name="history_edu" className="text-xl" />
          </div>
          <div>
            <h3 className="font-title-editorial text-lg font-semibold text-on-surface">Recent Bibliographic Ledger</h3>
            <p className="font-body-sm text-xs text-on-surface-variant">Last accessioned works added across your active research folios</p>
          </div>
        </div>
        <button type="button" className="inline-flex items-center gap-1 self-start font-label-lg text-xs font-medium text-primary hover:underline sm:self-auto">
          <span>View Full Ingestion Log</span>
          <Icon name="open_in_new" className="text-sm" />
        </button>
      </div>
      <div className="space-y-2.5 pt-1">
        {entries.map(({ id, book, listShortTitle }) => {
          const st = ledgerStatus(book);
          const ref = book.doi ? `DOI: ${book.doi}` : book.isbn ? `ISBN: ${book.isbn}` : book.callNumber;
          const journal = book.resourceType === "journal-article";
          return (
            <div key={id} className="flex flex-col justify-between gap-3 rounded-xl border border-[#E5E0D8]/60 bg-surface-container-low p-3.5 transition-all hover:border-primary/30 md:flex-row md:items-center">
              <div className="flex items-start gap-3">
                <div className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-[#E5E0D8] bg-white ${journal ? "text-primary" : "text-secondary"}`}>
                  <Icon name={journal ? "article" : "menu_book"} className="text-lg" />
                </div>
                <div>
                  <h4 className="font-title-editorial text-sm font-semibold text-on-surface">{ledgerTitle(book)}</h4>
                  <div className="mt-1 flex flex-wrap items-center gap-2">
                    <span className="font-metadata-caps text-[10px] uppercase text-outline">{ref}</span>
                    <span className="text-xs text-outline">•</span>
                    <span className="font-metadata-caps text-[10px] font-medium text-primary">Added to: {listShortTitle}</span>
                    <span className="text-xs text-outline">•</span>
                    <span className="font-body-sm text-xs text-on-surface-variant">{book.availabilityNote ?? book.availabilityLabel}</span>
                  </div>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-2.5 self-end md:self-auto">
                <span
                  className={`rounded-full px-2 py-0.5 font-metadata-caps text-[11px] font-medium ${
                    st.indexed ? "bg-[#c6ebd3] font-semibold text-[#2F5D45]" : "border border-[#E5E0D8] bg-[#eae8e3] text-on-surface-variant"
                  }`}
                >
                  {st.label}
                </span>
                <button type="button" title="Download Citation" className="rounded-lg border border-[#E5E0D8] p-1.5 text-outline transition-colors hover:bg-white hover:text-on-surface">
                  <Icon name="file_download" className="text-base" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

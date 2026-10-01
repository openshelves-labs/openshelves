import Link from "next/link";
import type { Book, HistoryEntry } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import CiteButton from "@/components/CiteButton";
import HoldButton from "@/components/HoldButton";
import AddToListButton from "@/components/AddToListButton";
import { formatDate } from "@/lib/dates";

const FORMAT_ICON: Record<HistoryEntry["format"], string> = { physical: "event_repeat", digital: "cloud_done", ill: "swap_horiz" };

const GHOST_BTN =
  "h-8 px-3 rounded-lg bg-surface-container-lowest text-on-surface hover:bg-surface-container font-label-md flex items-center gap-1.5 transition-colors";
const INERT_BTN = "h-8 px-3.5 rounded-lg bg-surface-container-high text-on-surface hover:bg-surface-container-highest font-label-lg flex items-center gap-1.5 transition-colors";

function authorLine(book: Book): string {
  const names = book.contributors.filter((c) => c.role === "author" || c.role === "co-author").map((c) => c.name);
  return names.length === 2 ? names.join(" & ") : names.join(", ");
}

/** "Oct 04, 2024" (design zero-pads the day). */
const stamp = (iso: string) => formatDate(iso).replace(/ (\d),/, " 0$1,");

export default function HistoryEntryCard({ entry, book }: { entry: HistoryEntry; book: Book }) {
  const showDoi = entry.format === "digital" && book.doi;
  return (
    <div className="group flex flex-col sm:flex-row gap-5 p-5 bg-surface-container-lowest rounded-xl shadow-sm hover:shadow-md transition-all">
      <BookCover book={book} className="shrink-0 w-24 sm:w-28 h-36 sm:h-40 !rounded shadow-sm" />

      <div className="flex-1 min-w-0 flex flex-col justify-between space-y-3">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <span className="px-2 py-0.5 rounded bg-surface-container text-on-surface-variant font-metadata-caps text-[10px] uppercase font-semibold">{entry.formatLabel}</span>
            <span className="text-outline text-xs">·</span>
            {showDoi ? (
              <span className="font-metadata-caps text-[11px] text-outline">DOI: {book.doi}</span>
            ) : (
              <span className="font-metadata-caps text-[11px] text-outline">Call No. {book.callNumber}</span>
            )}
            {(entry.barcode || entry.sourceNote) && <span className="text-outline text-xs">·</span>}
            {entry.barcode && <span className="font-metadata-caps text-[11px] text-outline">Barcode #{entry.barcode}</span>}
            {entry.sourceNote && (
              <span className={`font-metadata-caps text-[11px] font-semibold ${entry.format === "ill" ? "text-primary" : "text-secondary"}`}>{entry.sourceNote}</span>
            )}
          </div>
          <Link href={`/catalog/${book.id}`} className="block font-title-editorial text-on-surface font-semibold hover:text-primary transition-colors leading-snug">
            {book.title}
          </Link>
          <p className="font-body-md text-sm text-on-surface-variant mt-0.5">
            {authorLine(book)} <span className="text-outline">({entry.imprint ?? book.publisher})</span>
          </p>
          <div className="flex items-center gap-1.5 text-xs text-on-surface-variant mt-1.5">
            <Icon name={entry.noteIcon ?? "location_on"} className="text-[16px] text-secondary" />
            <span>{entry.branchNote}</span>
          </div>
        </div>

        <div className="p-2.5 rounded-lg bg-surface-container-low flex flex-wrap items-center justify-between gap-3 text-sm">
          <div className="flex items-center gap-2">
            <Icon name={FORMAT_ICON[entry.format]} className="text-primary text-[18px]" />
            <span className="text-on-surface-variant">
              Borrowed: <strong className="text-on-surface font-medium">{stamp(entry.borrowedOn)}</strong>
            </span>
            <span className="text-outline">→</span>
            <span className="text-on-surface-variant">
              Returned: <strong className="text-on-surface font-medium">{stamp(entry.returnedOn)}</strong>
            </span>
          </div>
          <span className="font-metadata-caps text-[11px] font-semibold text-primary px-2 py-0.5 rounded bg-[#E4EEE8]">Loan duration: {entry.loanDays} days</span>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 pt-1">
          {entry.format === "physical" && (
            <HoldButton
              bookId={book.id}
              label="Borrow Again"
              placedLabel="Hold Placed"
              icon="sync"
              iconClassName="text-[16px]"
              className="h-8 px-3.5 rounded-lg bg-primary text-on-primary hover:bg-[#1E3F2E] font-label-lg flex items-center gap-1.5 transition-colors"
            />
          )}
          {entry.format === "digital" && (
            <button type="button" className={INERT_BTN}>
              <Icon name="visibility" className="text-[16px] text-primary" />
              <span>Read Online / Re-borrow</span>
            </button>
          )}
          {entry.format === "ill" && (
            <button type="button" className={INERT_BTN}>
              <Icon name="bookmark_manager" className="text-[16px] text-primary" />
              <span>Request ILL Renewal</span>
            </button>
          )}
          <CiteButton book={book} label="Cite" className={GHOST_BTN} iconClassName="text-[16px] text-primary" />
          <AddToListButton
            bookId={book.id}
            className="h-8 px-2.5 rounded-lg bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container font-label-md flex items-center gap-1 transition-colors"
            iconClassName="text-[16px]"
          />
        </div>
      </div>
    </div>
  );
}

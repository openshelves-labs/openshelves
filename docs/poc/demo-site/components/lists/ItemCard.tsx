"use client";

import Link from "next/link";
import type { Book, ReadingListItem } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import BookCover from "@/components/BookCover";
import CiteButton from "@/components/CiteButton";
import HoldButton from "@/components/HoldButton";
import { authorLine, fullTitle, pad2 } from "./bookText";
import { dueBackLabel } from "./ledger";

interface Props {
  listId: string;
  item: ReadingListItem;
  book: Book;
}

const CHIP_TONE = {
  success: "bg-secondary-container text-primary",
  warning: "bg-amber-100 text-amber-900",
  open: "bg-emerald-100 text-emerald-900",
  neutral: "bg-surface-container text-on-surface-variant",
} as const;

function statusChip(b: Book): { tone: keyof typeof CHIP_TONE; label: string; icon?: string } {
  switch (b.availabilityStatus) {
    case "available":
      return { tone: "success", label: b.availabilityLabel };
    case "online":
      return { tone: "success", label: b.availabilityLabel, icon: "verified" };
    case "open_access":
      return { tone: "open", label: b.availabilityLabel, icon: "lock_open" };
    case "checked_out":
      return { tone: "warning", label: `${b.availabilityLabel}${dueBackLabel(b) ? ` (${dueBackLabel(b)})` : ""}` };
    default:
      return { tone: "neutral", label: b.availabilityLabel };
  }
}

const BTN_SECONDARY = "flex h-8 items-center gap-1.5 rounded-lg bg-surface-container px-3 font-label-md text-xs font-semibold text-on-surface transition-colors hover:bg-surface-container-highest";
const BTN_PRIMARY = "flex h-8 items-center gap-1.5 rounded-lg bg-primary px-3 font-label-md text-xs font-semibold text-on-primary transition-colors hover:bg-primary-container";

export default function ItemCard({ listId, item, book }: Props) {
  const { removeFromList } = useLibrary();
  const chip = statusChip(book);
  const place = book.copies[0]?.branchName ?? book.availabilityNote;
  const digital = book.availabilityStatus === "online" || book.availabilityStatus === "open_access";
  const journal = book.resourceType === "journal-article";
  const refs: { label: string; value: string; link?: boolean }[] = [];
  if (book.callNumber && book.callNumber !== "Online") refs.push({ label: "Call", value: book.callNumber });
  if (book.doi) refs.push({ label: "DOI", value: book.doi, link: true });
  if (book.isbn && !journal) refs.push({ label: "ISBN", value: book.isbn });
  else if (book.holdsInQueue) refs.push({ label: "", value: `${book.holdsInQueue} Hold${book.holdsInQueue > 1 ? "s" : ""} in Queue` });
  else if (book.pages && !book.isbn) refs.push({ label: "", value: `${book.pages} Pages` });

  return (
    <article className="group relative rounded-xl border border-[#E5E0D8] bg-surface-container-lowest p-5 shadow-sm transition-all hover:shadow-md">
      <div className="flex items-start gap-4">
        <div className="flex select-none flex-col items-center gap-1.5 pt-0.5 text-outline group-hover:text-on-surface">
          <Icon name="drag_indicator" className="cursor-grab text-base text-outline/70 hover:text-primary active:cursor-grabbing" />
          <span
            className={`rounded px-1.5 py-0.5 font-metadata-caps text-[11px] font-bold tracking-wider ${
              item.position === 1 ? "bg-secondary-container/40 text-primary" : "bg-surface-container text-on-surface-variant"
            }`}
          >
            {pad2(item.position)}
          </span>
        </div>
        <BookCover book={book} className="aspect-[2/3] w-20 shrink-0 !rounded-lg sm:w-24" />
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="mb-1 flex flex-wrap items-center gap-2">
                <span className={`flex items-center gap-1 rounded px-2 py-0.5 font-metadata-caps text-[10px] font-semibold ${CHIP_TONE[chip.tone]}`}>
                  {chip.icon ? <Icon name={chip.icon} className="text-[12px]" /> : <span className="h-1.5 w-1.5 rounded-full bg-current" />}
                  {chip.label}
                </span>
                {place && (
                  <>
                    <span className="text-xs text-outline">•</span>
                    <span className="font-metadata-caps text-[10px] text-outline">{place}</span>
                  </>
                )}
              </div>
              <h2 className="font-headline-sm text-base font-semibold leading-snug text-on-surface transition-colors group-hover:text-primary sm:text-lg">
                <Link href={`/catalog/${book.id}`}>{fullTitle(book)}</Link>
              </h2>
            </div>
            {item.week && (
              <span className="shrink-0 rounded-md bg-surface-container px-2 py-1 font-metadata-caps text-[10px] font-bold text-on-surface-variant">{item.week}</span>
            )}
          </div>
          <p className="font-body-sm text-xs text-on-surface-variant">
            <span className="font-medium text-on-surface">{authorLine(book)}</span> ({book.publicationYear}) •{" "}
            {journal ? (
              <>
                <em>{book.publisher}</em>
                {book.series ? `, ${book.series}` : ""}
              </>
            ) : (
              book.publisher
            )}
          </p>
          <div className="flex flex-wrap items-center gap-3 font-metadata-caps text-[11px] text-outline">
            {refs.map((r, i) => (
              <span key={`${r.label}-${r.value}`} className="flex items-center gap-3">
                {i > 0 && <span>•</span>}
                <span>
                  {r.label && `${r.label}: `}
                  {r.label === "Call" ? (
                    <strong className="font-semibold text-on-surface-variant">{r.value}</strong>
                  ) : r.link ? (
                    <a className="underline hover:text-primary" href={`https://doi.org/${r.value}`} target="_blank" rel="noreferrer">
                      {r.value}
                    </a>
                  ) : (
                    r.value
                  )}
                </span>
              </span>
            ))}
          </div>
          {item.note && (
            <div className="mt-2.5 flex items-start gap-2.5 rounded-lg bg-surface-container-low p-3">
              <Icon name="edit_note" className="mt-0.5 shrink-0 text-[16px] text-primary" />
              <div className="flex-1 text-xs">
                <span className="font-metadata-caps text-[10px] font-bold uppercase tracking-wide text-primary">Scholar Note:</span>
                <span className="pl-1 font-body-sm text-on-surface">{item.note}</span>
              </div>
              <button type="button" title="Edit note" className="text-outline transition-colors hover:text-primary">
                <Icon name="edit" className="text-sm" />
              </button>
            </div>
          )}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <div className="flex items-center gap-2">
              <CiteButton book={book} className={BTN_SECONDARY} iconClassName="text-sm text-outline" />
              {digital ? (
                <button type="button" className={BTN_PRIMARY}>
                  <Icon name={book.availabilityStatus === "open_access" ? "open_in_new" : "file_open"} className="text-sm" />
                  <span>{book.availabilityStatus === "open_access" ? "Read Open Access (PDF)" : "Read Online (PDF)"}</span>
                </button>
              ) : book.availabilityStatus === "checked_out" ? (
                <HoldButton
                  bookId={book.id}
                  label="Recall / Queue Hold"
                  placedLabel="Hold Queued"
                  icon="schedule"
                  className="flex h-8 items-center gap-1.5 rounded-lg bg-surface-container-highest px-3 font-label-md text-xs font-semibold text-on-surface transition-colors hover:bg-surface-container-high"
                />
              ) : (
                <HoldButton bookId={book.id} label={item.position === 1 ? "Place Hold / Request" : "Place Hold"} icon="bookmark_add" className={BTN_PRIMARY} />
              )}
              {item.tag && (
                <button type="button" className="flex items-center gap-1 rounded-lg px-2.5 font-label-md text-xs text-on-surface-variant hover:bg-surface-container hover:text-on-surface h-8">
                  <Icon name={item.tag.includes("•") ? "bookmark" : "label"} className="text-sm" />
                  <span>{item.tag}</span>
                </button>
              )}
            </div>
            <button
              type="button"
              title="Remove from reading list"
              aria-label={`Remove ${book.title} from reading list`}
              onClick={() => removeFromList(listId, book.id)}
              className="rounded p-1.5 text-outline transition-colors hover:bg-error-container/20 hover:text-error"
            >
              <Icon name="delete_outline" className="text-lg" />
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

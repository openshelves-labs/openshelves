import Link from "next/link";
import type { Book } from "@/types";
import BookCover from "@/components/BookCover";
import CiteButton from "@/components/CiteButton";
import SaveButton from "@/components/SaveButton";
import { formatDate } from "@/lib/dates";
import AvailabilityPill from "./AvailabilityPill";
import { TYPE_LABEL, authorLine } from "./arrivalUtils";

/** Compact ledger row used by the list view. */
export default function ArrivalRow({ book }: { book: Book }) {
  return (
    <div className="flex items-center gap-4 bg-surface-container-lowest rounded-xl p-3 shadow-sm hover:shadow-md transition-all">
      <BookCover book={book} className="w-12 h-[72px] shrink-0 !rounded" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 text-outline font-metadata-caps text-[11px]">
          <span>{TYPE_LABEL[book.resourceType] ?? "Monograph"}</span>
          <span>·</span>
          <span>{book.publicationYear}</span>
          {book.arrivalBadge && <span className="px-1.5 rounded-full bg-secondary-container text-primary text-[10px] uppercase font-bold tracking-wider">{book.arrivalBadge}</span>}
        </div>
        <Link href={`/catalog/${book.id}`} className="block truncate font-title-editorial text-on-surface font-medium hover:text-primary">
          {book.title}
        </Link>
        <p className="truncate font-body-sm text-sm text-on-surface-variant">
          {authorLine(book)} <span className="text-outline">· {book.callNumber}</span>
        </p>
      </div>
      <span className="hidden md:block shrink-0 font-metadata-caps text-[11px] text-outline">{book.accessionDate ? `Accessioned ${formatDate(book.accessionDate)}` : ""}</span>
      <div className="hidden sm:block shrink-0 max-w-[200px]">
        <AvailabilityPill book={book} />
      </div>
      <div className="flex shrink-0 items-center gap-1.5">
        <SaveButton
          bookId={book.id}
          iconOnly
          icon="bookmark_add"
          iconClassName="text-[18px]"
          className="p-1.5 rounded-full text-on-surface-variant hover:text-primary hover:bg-surface-container transition-colors"
          savedClassName="p-1.5 rounded-full text-primary bg-secondary-container transition-colors"
        />
        <CiteButton book={book} label="" className="p-1.5 rounded-full text-on-surface-variant hover:text-primary hover:bg-surface-container transition-colors" iconClassName="text-[18px]" />
      </div>
    </div>
  );
}

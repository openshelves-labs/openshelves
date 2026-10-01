import Link from "next/link";
import type { Book } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import CiteButton from "@/components/CiteButton";
import HoldButton from "@/components/HoldButton";
import SaveButton from "@/components/SaveButton";
import AvailabilityPill from "./AvailabilityPill";
import { TYPE_LABEL, authorLine } from "./arrivalUtils";

const CTA = "w-full py-1.5 px-2 bg-surface-container-lowest text-primary rounded-md font-label-md font-semibold hover:bg-secondary-container transition-colors flex items-center justify-center gap-1";

/** Hover overlay action: circulation-aware call to action (only Place Hold has behaviour). */
function OverlayAction({ book }: { book: Book }) {
  switch (book.availabilityStatus) {
    case "available":
      return <HoldButton bookId={book.id} className={CTA} placedClassName={`${CTA} opacity-80`} />;
    case "online":
    case "open_access":
      return (
        <button type="button" className={CTA}>
          <Icon name="open_in_new" className="text-[14px]" />
          <span>Read Online</span>
        </button>
      );
    case "restricted":
      return (
        <button type="button" className={CTA}>
          Book Reading Desk
        </button>
      );
    default:
      return (
        <button type="button" className={CTA}>
          Request Retrieval
        </button>
      );
  }
}

function overlayRef(book: Book): string {
  if (book.availabilityStatus === "online" && book.doi) return `DOI: ${book.doi}`;
  if (book.resourceType === "manuscript-guide") return `Manuscript ${book.callNumber}`;
  return `Call: ${book.callNumber}`;
}

export default function ArrivalTile({ book }: { book: Book }) {
  const faculty = book.arrivalBadge === "Faculty Author";
  const archival = book.arrivalBadge === "Archival";
  return (
    <div className="group relative flex flex-col bg-surface-container-lowest rounded-xl overflow-hidden p-3.5 shadow-sm hover:shadow-md transition-all">
      <div className="relative w-full aspect-[2/3] rounded-lg overflow-hidden bg-surface-container">
        <BookCover book={book} className="h-full w-full !rounded-lg" />
        <Link href={`/catalog/${book.id}`} aria-label={book.title} className="absolute inset-0" />
        {book.arrivalBadge && (
          <div className="absolute top-2.5 left-2.5 pointer-events-none">
            <span
              className={`px-2 py-0.5 rounded-full font-metadata-caps text-[10px] uppercase font-bold tracking-wider ${
                archival ? "bg-surface-container-highest text-tertiary" : "bg-secondary-container text-primary"
              }`}
            >
              {book.arrivalBadge}
            </span>
          </div>
        )}
        <div className="pointer-events-none absolute inset-0 bg-primary/80 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity p-3 flex flex-col justify-between text-on-primary">
          <div className="pointer-events-auto flex justify-end gap-1.5">
            <SaveButton
              bookId={book.id}
              iconOnly
              icon="bookmark_add"
              iconClassName="text-[16px]"
              className="p-1.5 rounded-full bg-white/20 hover:bg-white/40 text-on-primary transition-colors"
              savedClassName="p-1.5 rounded-full bg-white text-primary transition-colors"
            />
            <CiteButton book={book} label="" className="p-1.5 rounded-full bg-white/20 hover:bg-white/40 text-on-primary transition-colors" iconClassName="text-[16px]" />
          </div>
          <div className="pointer-events-auto space-y-1.5">
            <span className="font-metadata-caps text-[10px] text-secondary-fixed block">{overlayRef(book)}</span>
            <OverlayAction book={book} />
          </div>
        </div>
      </div>
      <div className="mt-3 flex-1 flex flex-col justify-between gap-2">
        <div>
          <div className="flex items-center justify-between text-outline font-metadata-caps text-[11px]">
            <span>{TYPE_LABEL[book.resourceType] ?? "Monograph"}</span>
            <span>{book.publicationYear}</span>
          </div>
          <Link href={`/catalog/${book.id}`} className="block font-title-editorial text-on-surface line-clamp-2 mt-1 leading-snug font-medium hover:text-primary">
            {book.title}
          </Link>
          <p className={`font-body-sm mt-0.5 truncate ${faculty ? "text-primary font-semibold" : "text-on-surface-variant"}`}>{authorLine(book)}</p>
        </div>
        <div className="pt-2">
          <AvailabilityPill book={book} />
        </div>
      </div>
    </div>
  );
}

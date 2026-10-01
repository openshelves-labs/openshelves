import Link from "next/link";
import type { ReactNode } from "react";
import type { Book } from "@/types";
import BookCover from "./BookCover";

/**
 * Compact cover tile (2:3 cover, title, lead author, action slot).
 * Used on Home "New Arrivals" and reusable for any grid of monographs.
 * `actions` is rendered below the metadata — pass HoldButton / SaveButton etc.
 */
export default function BookCard({ book, actions, href }: { book: Book; actions?: ReactNode; href?: string }) {
  const lead = book.contributors[0]?.name ?? "";
  const more = book.contributors.filter((c) => c.role === "author" || c.role === "co-author").length > 1;
  const link = href ?? `/catalog/${book.id}`;
  return (
    <div className="flex flex-col justify-between rounded-xl border border-border-warm bg-white p-3 transition-all hover:border-primary/40">
      <Link href={link} className="block">
        <BookCover book={book} className="mb-2.5 aspect-[2/3] w-full rounded bg-stone-100" />
        <h4 className="font-title-editorial text-xs font-semibold leading-snug text-on-surface line-clamp-2 hover:text-primary">{book.title}</h4>
        <p className="mt-0.5 truncate font-body-sm text-[11px] text-outline">
          {lead}
          {more ? " et al." : ""}
        </p>
      </Link>
      {actions}
    </div>
  );
}

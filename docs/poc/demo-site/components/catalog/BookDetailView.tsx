"use client";

import Link from "next/link";
import type { Book, Contributor } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import HoldButton from "@/components/HoldButton";
import CiteButton from "@/components/CiteButton";
import AddToListButton from "@/components/AddToListButton";
import CitePanel from "./CitePanel";
import DetailTabs from "./DetailTabs";
import RelatedCarousel from "./RelatedCarousel";

const ROLE_LABEL: Record<Contributor["role"], string> = {
  author: "Lead Author",
  "co-author": "Co-Author",
  editor: "Editor",
  foreword: "Foreword",
  translator: "Translator",
  curator: "Curator",
};

const ROW_LABEL = "font-label-lg text-xs uppercase tracking-wider text-outline font-semibold";
const SECONDARY_BTN =
  "h-9 px-2 w-full rounded-lg bg-surface-container-lowest hover:bg-surface-container border border-[#E5E0D8] text-on-surface font-label-md text-xs flex items-center justify-center gap-1.5 transition-colors";

function Attribution({ book }: { book: Book }) {
  const lead = book.contributors.filter((c) => c.role === "author");
  const lead1 = lead.length ? lead : book.contributors.slice(0, 1);
  const rest = book.contributors.filter((c) => !lead1.includes(c));
  const groups = (["co-author", "editor", "foreword", "translator", "curator", "author"] as const)
    .map((role) => ({ role, people: rest.filter((c) => c.role === role) }))
    .filter((g) => g.people.length);

  const rows = [
    { label: ROLE_LABEL[lead1[0]?.role ?? "author"], people: lead1, lead: true },
    ...groups.map((g) => ({ label: g.people.length > 1 && g.role === "co-author" ? "Co-Authors" : ROLE_LABEL[g.role], people: g.people, lead: false })),
  ];

  return (
    <div className="flex flex-wrap items-center gap-y-2 gap-x-4 pt-3 border-t border-[#E5E0D8] font-body-sm text-sm">
      {rows.map((r, i) => (
        <div key={r.label} className="flex items-center gap-x-4">
          {i > 0 && <span className="text-outline-variant">•</span>}
          <div className="flex flex-wrap items-center gap-1.5">
            <span className={ROW_LABEL}>{r.label}:</span>
            {r.people.map((p, j) => (
              <span key={p.name} className="flex items-center gap-1.5">
                {j > 0 && <span className="text-outline-variant">,</span>}
                <span className={r.lead ? "font-semibold text-primary" : r.label === "Editor" || r.label === "Foreword" ? "text-on-surface-variant" : "font-semibold text-on-surface"}>{p.name}</span>
                {p.affiliation && <span className="font-metadata-caps text-[10px] text-outline">({p.affiliation})</span>}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

export default function BookDetailView({ book, related }: { book: Book; related: Book[] }) {
  const d = book.detail;
  const status = book.availabilityStatus;
  const digital = status === "online" || status === "open_access";
  const availableCopies = book.copies.filter((c) => c.status === "available" || c.status === "reserve").length;
  const copiesBadge = d?.copiesBadge ?? (availableCopies ? `${availableCopies} Physical ${availableCopies === 1 ? "Copy" : "Copies"} Available` : book.availabilityLabel);
  const firstCopy = book.copies[0];
  const shelfLine = d?.shelfLine ?? (firstCopy ? `${firstCopy.branchName}, ${firstCopy.location}` : book.availabilityLabel);
  const holdLabel =
    status === "checked_out" ? `Place Hold (Waitlist #${(book.holdsInQueue ?? 0) + 1})` : "Place Hold / Request Copy";
  const stackChip = status === "available" ? "In Stacks" : digital ? "Online" : book.availabilityLabel;

  return (
    <div className="flex flex-col w-full">
      <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-[#E5E0D8]">
        <nav aria-label="Breadcrumbs" className="flex items-center gap-2">
          <Link href="/catalog" className="font-body-sm text-outline hover:text-primary transition-colors flex items-center gap-1.5">
            <Icon name="account_balance" className="text-[17px]" />
            <span>Catalog</span>
          </Link>
          <span className="text-outline-variant text-xs">/</span>
          <Link href="/catalog" className="font-body-sm text-outline hover:text-primary transition-colors">
            Discovery Index
          </Link>
          <span className="text-outline-variant text-xs">/</span>
          <span className="font-label-lg text-primary text-xs uppercase tracking-wider bg-surface-container-high px-2 py-0.5 rounded">Monograph Record</span>
        </nav>
        <div className="flex items-center gap-2.5 bg-surface-container-low px-3 py-1.5 rounded-full border border-[#E5E0D8]">
          <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
          <span className="font-metadata-caps text-on-surface-variant text-[11px]">Central University Library • Indexed &amp; Verified</span>
          <Icon name="verified" className="text-primary text-base" />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 pt-8 items-start">
        {/* LEFT: media + primary actions */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-surface-container-lowest rounded-2xl p-6 border border-[#E5E0D8] flex flex-col items-center">
            <div className="relative w-full max-w-[260px] aspect-[2/3] rounded-lg overflow-hidden shadow-md group">
              <BookCover book={book} className="h-full w-full !rounded-none !border-0 transition-transform duration-300 group-hover:scale-[1.02]" />
              <div className="absolute inset-0 bg-gradient-to-t from-primary/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-4">
                <span className="font-label-lg text-xs text-on-primary flex items-center gap-1.5">
                  <Icon name="zoom_in" className="text-base" />
                  High-Res Archival View
                </span>
              </div>
              <div className="absolute left-0 top-0 bottom-0 w-3 bg-gradient-to-r from-black/25 to-transparent pointer-events-none" />
            </div>

            <div className="w-full mt-6 bg-surface-container-low rounded-xl p-4 border border-[#E5E0D8] flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="font-metadata-caps text-[11px] uppercase tracking-wider text-outline font-semibold">Classification Callout</span>
                <span className="font-metadata-caps text-[10px] bg-secondary-fixed text-on-secondary-fixed px-2 py-0.5 rounded-full font-bold">{stackChip}</span>
              </div>
              <div className="font-title-editorial text-primary font-bold text-base tracking-wide">{book.callNumber}</div>
              <div className="flex items-start gap-1.5 text-on-surface-variant font-body-sm text-xs leading-relaxed">
                <Icon name="location_on" className="text-base text-primary shrink-0 mt-0.5" />
                <span>{shelfLine}</span>
              </div>
            </div>

            <div className="w-full flex flex-col gap-2.5 mt-5">
              <HoldButton
                bookId={book.id}
                label={holdLabel}
                placedLabel="Hold Placed — Cancel"
                icon="bookmark_added"
                className={`w-full h-11 text-on-primary font-label-lg text-sm rounded-xl transition-all flex items-center justify-center gap-2 border border-primary/40 active:scale-[0.99] ${
                  status === "checked_out" ? "bg-tertiary hover:bg-tertiary-container" : "bg-primary hover:bg-[#1e3f2e]"
                }`}
                placedClassName="w-full h-11 bg-surface-container-low text-primary font-label-lg text-sm rounded-xl transition-all flex items-center justify-center gap-2 border border-primary/40"
                iconClassName="text-lg"
              />
              {(d?.fullTextLabel || digital) && (
                <button
                  type="button"
                  className="w-full h-10 bg-surface-container-low hover:bg-surface-container hover:text-primary text-on-surface font-label-lg text-xs rounded-xl border border-[#E5E0D8] transition-colors flex items-center justify-center gap-2"
                >
                  <Icon name="menu_book" className="text-base text-primary" />
                  <span>{d?.fullTextLabel ?? "Read Online (Full-Text PDF)"}</span>
                  <span className="font-metadata-caps text-[9px] uppercase px-1.5 py-0.5 bg-primary/10 text-primary rounded font-bold">{d?.fullTextTag ?? (status === "open_access" ? "Open Access" : "EZProxy")}</span>
                </button>
              )}
            </div>

            <div className="w-full grid grid-cols-2 gap-2 mt-4 pt-4 border-t border-[#E5E0D8]">
              <CiteButton book={book} label="Cite Work" className={SECONDARY_BTN} iconClassName="text-sm text-primary" />
              <AddToListButton bookId={book.id} label="Save to List" icon="playlist_add" align="right" className={SECONDARY_BTN} iconClassName="text-sm text-primary" wrapperClassName="relative w-full" />
              <button type="button" className={SECONDARY_BTN}>
                <Icon name="link" className="text-sm text-primary" />
                <span>Share DOI</span>
              </button>
              <button type="button" className={SECONDARY_BTN}>
                <Icon name="outbound" className="text-sm text-primary" />
                <span>Request ILL</span>
              </button>
            </div>

            {d?.archivalCondition && (
              <div className="w-full mt-5 p-3 rounded-lg bg-[#FAF8F5] border border-[#E5E0D8] flex items-center gap-2.5">
                <Icon name="verified_user" className="text-outline text-lg" />
                <div className="flex flex-col text-left">
                  <span className="font-metadata-caps text-[10px] uppercase text-outline">Archival Condition</span>
                  <span className="font-body-sm text-[12px] text-on-surface-variant leading-tight">{d.archivalCondition}</span>
                </div>
              </div>
            )}
          </div>

          {book.metrics && (
            <div className="bg-surface-container-lowest rounded-2xl p-5 border border-[#E5E0D8] space-y-3">
              <h4 className="font-metadata-caps text-[11px] uppercase tracking-wider text-outline font-bold">Bibliometric Impact</h4>
              <div className="grid grid-cols-3 gap-3 text-center">
                {[
                  { v: String(book.metrics.citations), l: "Citations" },
                  { v: book.metrics.reads, l: "Reads" },
                  { v: book.metrics.fieldIndex, l: "Field Index" },
                ].map((m) => (
                  <div key={m.l} className="p-2.5 rounded-lg bg-surface-container-low border border-[#E5E0D8]">
                    <div className="font-title-editorial text-lg font-bold text-primary">{m.v}</div>
                    <div className="font-metadata-caps text-[10px] text-outline mt-0.5">{m.l}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* RIGHT: header, tabs, cite */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <div className="bg-surface-container-lowest rounded-2xl p-7 border border-[#E5E0D8] space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              {book.peerReviewed && (
                <span className="font-metadata-caps text-[11px] bg-secondary-fixed text-on-secondary-fixed px-2.5 py-1 rounded-full font-bold flex items-center gap-1">
                  <Icon name="verified" className="text-xs" />
                  Peer Reviewed
                </span>
              )}
              {book.formatLabel && (
                <span className="font-metadata-caps text-[11px] bg-surface-container-high text-on-surface-variant px-2.5 py-1 rounded-full border border-[#E5E0D8]">{book.formatLabel}</span>
              )}
              <span className="font-metadata-caps text-[11px] bg-[#E4EEE8] text-primary px-2.5 py-1 rounded-full font-bold border border-primary/20 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-primary" />
                {copiesBadge}
              </span>
              {d?.partnerBadge && (
                <span className="font-metadata-caps text-[11px] bg-surface-container-low text-outline px-2.5 py-1 rounded-full border border-[#E5E0D8]">{d.partnerBadge}</span>
              )}
            </div>
            <div>
              <h1 className="font-headline-lg text-primary font-bold tracking-tight text-2xl sm:text-3xl lg:text-[34px] leading-snug">
                {book.subtitle ? `${book.title}: ${book.subtitle}` : book.title}
              </h1>
              {book.tagline && <p className="font-title-editorial text-on-surface-variant text-base sm:text-lg italic mt-2 leading-relaxed font-normal">{book.tagline}</p>}
            </div>
            <Attribution book={book} />
            <div className="flex flex-wrap items-center gap-3 pt-2 text-xs text-outline font-body-sm">
              <span className="flex items-center gap-1 font-medium text-on-surface">
                <Icon name="apartment" className="text-sm text-primary" />
                {book.publisher}
              </span>
              <span>•</span>
              <span>{book.publishedLabel ?? `Published ${book.publicationYear}`}</span>
              {book.edition && (
                <>
                  <span>•</span>
                  <span>{book.edition}</span>
                </>
              )}
              <span>•</span>
              <span>Language: {book.language}</span>
            </div>
          </div>

          <DetailTabs book={book} />
          <CitePanel book={book} />
        </div>
      </div>

      <RelatedCarousel books={related} />
    </div>
  );
}

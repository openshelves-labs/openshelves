import Link from "next/link";
import type { Book, CatalogCardAction } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import HoldButton from "@/components/HoldButton";
import CiteButton from "@/components/CiteButton";
import SaveButton from "@/components/SaveButton";
import AddToListButton from "@/components/AddToListButton";
import { cardMeta, creatorNames, toneForStatus } from "@/lib/catalog";

const COVER_TONE = {
  primary: "bg-primary/80 text-on-primary",
  secondary: "bg-secondary/85 text-on-secondary",
  tertiary: "bg-tertiary-container/90 text-on-tertiary",
  "primary-container": "bg-primary-container/90 text-on-primary-container",
  "secondary-container": "bg-secondary-container/90 text-on-secondary-container",
} as const;

const TAG_TONE = {
  plain: "text-on-surface-variant bg-surface-container",
  muted: "text-outline bg-surface-container",
  accent: "text-primary bg-secondary-fixed/40",
  secondary: "text-secondary bg-secondary-container/40",
} as const;

const STATUS_TONE = {
  ok: { chip: "bg-secondary-fixed text-primary", dot: "bg-primary" },
  bad: { chip: "bg-error-container/70 text-on-error-container", dot: "bg-error" },
  warn: { chip: "bg-[#FDF4E7] text-[#C27D23]", dot: "bg-[#C27D23]" },
} as const;

const PRIMARY = "h-9 px-4 bg-primary hover:bg-primary-container text-on-primary rounded-lg font-label-lg text-label-md font-semibold flex items-center gap-1.5 transition-colors";
const TERTIARY = "h-9 px-4 bg-tertiary hover:bg-tertiary-container text-on-tertiary rounded-lg font-label-lg text-label-md font-semibold flex items-center gap-1.5 transition-colors";
const SECONDARY = "h-9 px-3 bg-surface-container hover:bg-surface-container-high text-on-surface rounded-lg font-label-lg text-label-md flex items-center gap-1.5 transition-colors";

function ActionButton({ action, book, first }: { action: CatalogCardAction; book: Book; first: boolean }) {
  const cls = first ? (action.tone === "tertiary" ? TERTIARY : PRIMARY) : SECONDARY;
  const iconCls = first ? "text-base" : "text-base text-outline";
  switch (action.kind) {
    case "hold":
      return (
        <HoldButton
          bookId={book.id}
          label={action.label}
          placedLabel={action.placedLabel ?? "Hold Placed"}
          icon={action.icon ?? "bookmark_add"}
          className={cls}
          placedClassName={`${SECONDARY} !text-primary`}
          iconClassName="text-base"
        />
      );
    case "cite":
      return <CiteButton book={book} label={action.label} className={SECONDARY} iconClassName="text-base text-primary" />;
    case "list":
      return <AddToListButton bookId={book.id} label={action.label} icon={action.icon} className={SECONDARY} iconClassName="text-base text-outline" />;
    case "save":
      return (
        <SaveButton
          bookId={book.id}
          label={action.label}
          savedLabel="Saved to Workbench"
          icon={action.icon ?? "dns"}
          className={SECONDARY}
          savedClassName={`${SECONDARY} !text-primary`}
          iconClassName="text-base text-outline"
        />
      );
    default:
      return (
        <button type="button" className={cls}>
          {action.icon && <Icon name={action.icon} className={iconCls} />}
          <span>{action.label}</span>
        </button>
      );
  }
}

export default function CatalogResultCard({ book }: { book: Book }) {
  const m = cardMeta(book);
  const st = STATUS_TONE[toneForStatus(book.availabilityStatus)];
  const names = creatorNames(book);
  return (
    <article className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm hover:shadow transition-shadow flex flex-col md:flex-row gap-space-lg relative group">
      <div className="w-full md:w-44 shrink-0 flex flex-col items-center">
        <div className="w-36 md:w-full aspect-[2/3] rounded-lg overflow-hidden relative shadow-sm bg-surface-container-high flex flex-col justify-between p-3.5 text-center">
          <BookCover book={book} showTitleFallback={false} className="absolute inset-0 h-full w-full !rounded-none !border-0" />
          <div className={`relative z-10 flex flex-col justify-between h-full backdrop-blur-[2px] p-3 rounded ${COVER_TONE[m.cover.tone]}`}>
            <span className="font-metadata-caps text-[9px] uppercase tracking-widest opacity-80">{m.cover.kicker}</span>
            <div className="my-auto">
              <span className="font-title-editorial text-sm font-semibold leading-tight line-clamp-3">{m.cover.title}</span>
              <span className="block text-[11px] font-body-sm mt-1 opacity-90">{m.cover.byline}</span>
            </div>
            <span className="font-metadata-caps text-[8px] uppercase tracking-wider opacity-70">{m.cover.foot}</span>
          </div>
        </div>
        <span className="font-metadata-caps text-metadata-caps text-outline mt-2 tracking-wider">{m.coverRef}</span>
      </div>

      <div className="flex-1 flex flex-col justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-space-xs mb-space-xs">
            <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-lg text-metadata-caps font-semibold ${st.chip}`}>
              {m.statusIcon ? <Icon name={m.statusIcon} className="text-xs" /> : <span className={`w-1.5 h-1.5 rounded-full ${st.dot}`} />}
              {m.statusLabel ?? book.availabilityLabel}
            </span>
            {m.tags.map((t) => (
              <span key={t.label} className={`font-metadata-caps text-metadata-caps uppercase px-2 py-0.5 rounded ${TAG_TONE[t.tone]}`}>
                {t.label}
              </span>
            ))}
          </div>
          <h3 className="font-headline-md text-headline-md text-on-surface hover:text-primary transition-colors leading-snug">
            <Link href={`/catalog/${book.id}`}>{book.subtitle ? `${book.title}: ${book.subtitle}` : book.title}</Link>
          </h3>
          <p className="font-body-md text-body-md text-on-surface-variant mt-1">
            By{" "}
            {names.map((n, i) => (
              <span key={n}>
                {i > 0 && " & "}
                <span className="text-on-surface font-medium hover:underline cursor-pointer">{n}</span>
              </span>
            ))}
          </p>
          <div className="flex flex-wrap items-center gap-x-space-md gap-y-1 font-body-sm text-body-sm text-outline mt-2.5">
            <span>{book.publicationYear}</span>
            <span>•</span>
            <span className="text-on-surface font-medium">{book.publisher}</span>
            {book.pages && (
              <>
                <span>•</span>
                <span>{book.pages} pages</span>
              </>
            )}
            {(book.isbn || book.doi || book.series) && (
              <>
                <span>•</span>
                <span>{book.isbn ? `ISBN: ${book.isbn}` : book.doi ? `DOI: ${book.doi}` : book.series}</span>
              </>
            )}
          </div>
          <div className="mt-3.5 p-3 rounded-lg bg-surface-container-low flex items-start gap-2.5">
            <Icon name={m.note.icon} className={`text-lg mt-0.5 shrink-0 ${m.note.icon === "hourglass_top" ? "text-outline" : "text-primary"}`} />
            <div className="text-body-sm font-body-sm">
              <span className="font-semibold text-on-surface">{m.note.title}</span>
              {m.note.body && <> {m.note.body}</>}
              {m.note.sub && <span className="block text-outline text-xs mt-0.5">{m.note.sub}</span>}
            </div>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-space-xs mt-space-md pt-space-sm">
          {m.actions.map((a, i) => (
            <ActionButton key={`${a.kind}-${a.label}`} action={a} book={book} first={i === 0} />
          ))}
          {m.ghost &&
            (m.ghost.toDetail ? (
              <Link
                href={`/catalog/${book.id}`}
                className="h-9 px-3 text-primary hover:bg-secondary-fixed/30 rounded-lg font-label-lg text-label-md flex items-center gap-1 transition-colors ml-auto"
              >
                <span>{m.ghost.label}</span>
                <Icon name={m.ghost.icon} className="text-base" />
              </Link>
            ) : (
              <button
                type="button"
                className="h-9 px-3 text-on-surface-variant hover:text-on-surface rounded-lg font-label-lg text-label-md flex items-center gap-1 transition-colors ml-auto"
              >
                <Icon name={m.ghost.icon} className="text-base" />
                <span>{m.ghost.label}</span>
              </button>
            ))}
        </div>
      </div>
    </article>
  );
}

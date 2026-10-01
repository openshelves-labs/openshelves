import Link from "next/link";
import type { Book, ReadingList } from "@/types";
import Icon from "@/components/Icon";

const SPINE_TONES = [
  "bg-primary text-on-primary",
  "bg-tertiary-container text-on-tertiary",
  "bg-secondary text-on-secondary",
  "bg-primary-container text-on-primary-container",
  "bg-surface-dim text-on-surface",
];

const CATEGORY_TONE: Record<ReadingList["categoryKind"], string> = {
  course: "bg-[#c6ebd3] text-[#2F5D45] border-primary/20",
  shared: "bg-[#c6ebd3] text-[#2F5D45] border-primary/20",
  grant: "bg-surface-container-high text-on-surface-variant border-[#E5E0D8]",
  study: "bg-surface-container-high text-on-surface-variant border-[#E5E0D8]",
  archive: "bg-surface-container text-on-surface-variant border-[#E5E0D8]",
};

function spineLabels(list: ReadingList, bookOf: (id: string) => Book | undefined): string[] {
  if (list.spines?.length) return list.spines;
  return list.items
    .slice(0, 3)
    .map((i) => bookOf(i.bookId)?.subjects[0]?.replace(/[^A-Za-z]/g, "").slice(0, 4).toUpperCase())
    .filter((x): x is string => !!x);
}

export default function FolioCard({ list, bookOf }: { list: ReadingList; bookOf: (id: string) => Book | undefined }) {
  const spines = spineLabels(list, bookOf);
  const stats = list.statsLabel ?? `${list.pdfCount ?? 0} PDFs • ${list.doiCount ?? 0} DOIs indexed`;
  return (
    <article className="group relative flex flex-col justify-between overflow-hidden rounded-xl border border-[#E5E0D8] bg-white p-5 transition-all duration-200 hover:border-primary/40">
      <div className="space-y-3.5">
        <div className="flex items-center justify-between gap-2">
          <span className={`rounded-full border px-2.5 py-0.5 font-metadata-caps text-[11px] font-semibold uppercase tracking-wider ${CATEGORY_TONE[list.categoryKind]}`}>
            {list.category}
          </span>
          <button type="button" aria-label="Folio options" className="rounded-lg p-1 text-outline transition-colors hover:bg-stone-50 hover:text-on-surface">
            <Icon name="more_horiz" className="text-[20px]" />
          </button>
        </div>
        <div>
          <h2 className="font-title-editorial text-lg font-semibold leading-snug text-on-surface transition-colors group-hover:text-primary">
            <Link href={`/lists/${list.id}`}>{list.title}</Link>
          </h2>
          <p className="mt-1.5 line-clamp-2 font-body-sm text-xs text-on-surface-variant">{list.description || "No description yet."}</p>
        </div>
        <div className="flex items-center justify-between rounded-lg border border-[#E5E0D8]/60 bg-surface-container-low p-3">
          <div className="flex items-center -space-x-3">
            {spines.map((s, i) => (
              <div
                key={`${s}-${i}`}
                className={`flex h-12 w-8 flex-col justify-end overflow-hidden rounded p-1 font-metadata-caps text-[8px] font-bold uppercase leading-none ${SPINE_TONES[i % SPINE_TONES.length]}`}
              >
                {s}
              </div>
            ))}
          </div>
          <div className="ml-auto text-right">
            <span className="block font-label-lg text-xs font-semibold text-on-surface">{list.itemCount} Catalog Items</span>
            <span className="font-body-sm text-[11px] text-outline">{stats}</span>
          </div>
        </div>
        <div className="flex flex-wrap gap-1.5 pt-0.5">
          {list.tags.map((t) => (
            <span key={t} className="rounded border border-[#E5E0D8] bg-surface-container px-2 py-0.5 font-metadata-caps text-[10px] text-on-surface-variant">
              #{t}
            </span>
          ))}
        </div>
      </div>
      <div className="mt-4 space-y-2.5 border-t border-[#E5E0D8] pt-4">
        <div className="flex items-center justify-between text-xs text-outline">
          <div className="flex items-center gap-1.5">
            {list.membersIcon && <Icon name={list.membersIcon} className={`text-[15px] ${list.membersIcon === "verified_user" ? "text-primary" : "text-secondary"}`} />}
            <span className="font-body-sm text-xs text-on-surface-variant">{list.membersLabel ?? "Private Archive (Only you)"}</span>
          </div>
          <span className="font-metadata-caps text-[11px]">{list.updatedLabel}</span>
        </div>
        <div className="flex items-center justify-between pt-1">
          <Link href={`/lists/${list.id}`} className="inline-flex items-center gap-1 font-label-lg text-xs font-semibold text-primary hover:underline">
            <span>Open Folio</span>
            <Icon name="chevron_right" className="text-sm" />
          </Link>
          <button type="button" title="Quick Share" className="p-1 text-outline transition-colors hover:text-primary">
            <Icon name="share" className="text-base" />
          </button>
        </div>
      </div>
    </article>
  );
}

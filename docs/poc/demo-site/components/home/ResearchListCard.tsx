import Link from "next/link";
import type { ReadingList } from "@/types";
import Icon from "@/components/Icon";

/** "Continue Your Research" tile for one reading list. */
export default function ResearchListCard({ list }: { list: ReadingList }) {
  return (
    <div className="flex flex-col justify-between rounded-xl border border-border-warm bg-white p-4 transition-all hover:border-primary/40">
      <div>
        <div className="mb-1.5 flex items-center justify-between font-metadata-caps text-[11px] text-outline">
          <span className="font-semibold uppercase tracking-wider text-secondary">{list.category}</span>
          <span>{list.updatedLabel}</span>
        </div>
        <h3 className="font-title-editorial text-base font-semibold leading-snug text-on-surface">{list.title}</h3>
        <p className="mt-1.5 line-clamp-2 font-body-sm text-xs text-on-surface-variant">{list.description}</p>
      </div>
      <div className="mt-4 flex items-center justify-between border-t border-border-warm/60 pt-3 text-xs">
        <span className="flex items-center gap-1 font-metadata-caps text-[11px] text-outline">
          <Icon name="auto_stories" className="text-sm text-primary" />
          {list.itemCount} Items
        </span>
        <Link href={`/lists/${list.id}`} className="inline-flex items-center font-label-lg text-xs font-medium text-primary hover:underline">
          Open List <Icon name="chevron_right" className="text-sm" />
        </Link>
      </div>
    </div>
  );
}

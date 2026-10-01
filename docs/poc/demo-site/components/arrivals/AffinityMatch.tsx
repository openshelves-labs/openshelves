import Link from "next/link";
import type { Book } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";
import SaveButton from "@/components/SaveButton";
import { authorLine } from "./arrivalUtils";

export default function AffinityMatch({ books }: { books: Book[] }) {
  return (
    <section className="mt-12 bg-surface-container-lowest rounded-xl p-6 shadow-sm space-y-6 border border-border-warm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Icon name="auto_awesome" className="text-primary text-[20px]" />
            <span className="font-metadata-caps uppercase text-primary font-semibold">Scholar Affinity Match</span>
          </div>
          <h2 className="font-headline-sm text-on-surface">Recommended based on your research in Systems Ecology &amp; Biogeochemistry</h2>
        </div>
        <Link href="/catalog" className="inline-flex items-center gap-1 text-primary hover:text-primary-container font-label-md font-semibold transition-colors shrink-0">
          <span>View All Recommendations</span>
          <Icon name="arrow_forward" className="text-[16px]" />
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {books.map((b) => (
          <div key={b.id} className="flex items-start gap-3.5 p-3 rounded-lg bg-surface-container-low hover:bg-surface-container transition-colors group">
            <Link href={`/catalog/${b.id}`} className="shrink-0" aria-label={b.title}>
              <BookCover book={b} className="w-16 h-[88px] !rounded" showTitleFallback={false} />
            </Link>
            <div className="flex-1 min-w-0 flex flex-col justify-between self-stretch">
              <div>
                <span className="font-metadata-caps text-[10px] text-primary uppercase font-semibold">{b.relevanceScore}% Field Relevance</span>
                <Link href={`/catalog/${b.id}`} className="block font-title-editorial text-[14px] leading-tight text-on-surface font-semibold truncate group-hover:text-primary transition-colors">
                  {b.title}
                </Link>
                <p className="font-body-sm text-[12px] text-on-surface-variant truncate">
                  {authorLine(b)} ({b.publicationYear})
                </p>
              </div>
              <div className="flex items-center justify-between pt-2">
                <span className="font-metadata-caps text-[10px] text-outline">{b.callNumber}</span>
                <SaveButton
                  bookId={b.id}
                  iconOnly
                  icon="bookmark_add"
                  label="Save to list"
                  iconClassName="text-[16px]"
                  className="p-1 rounded text-on-surface-variant hover:text-primary hover:bg-white transition-colors"
                  savedClassName="p-1 rounded text-primary bg-white transition-colors"
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="pt-2 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-outline font-metadata-caps">
        <div className="flex items-center gap-2">
          <Icon name="verified" className="text-[16px] text-secondary" />
          <span>Catalog records updated via Cambridge Integrated Library System (Alma/Primo API v4.2)</span>
        </div>
        <div className="flex items-center gap-4">
          <span>RSS Accession Feed</span>
          <span className="text-surface-dim">·</span>
          <span>Export MARC21 / RIS Batch</span>
        </div>
      </div>
    </section>
  );
}

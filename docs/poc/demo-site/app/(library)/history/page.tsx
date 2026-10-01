import Link from "next/link";
import { getHistory, getBooks, getReadingSummary } from "@/lib/api";
import Icon from "@/components/Icon";
import HistoryView from "@/components/history/HistoryView";
import ReadingSummaryPanel from "@/components/history/ReadingSummaryPanel";

export default async function Page() {
  const [entries, books, summary] = await Promise.all([getHistory(), getBooks(), getReadingSummary()]);
  const ids = new Set(entries.map((e) => e.bookId));
  const historyBooks = books.filter((b) => ids.has(b.id));

  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <div className="flex flex-col w-full">
        <div className="flex flex-col space-y-2">
          <nav className="flex items-center space-x-2 font-metadata-caps text-outline uppercase tracking-wider">
            <Link className="hover:text-primary transition-colors" href="/">Home</Link>
            <span className="text-outline-variant">/</span>
            <span>My Library</span>
            <span className="text-outline-variant">/</span>
            <span className="text-primary font-semibold">Reading History</span>
          </nav>
          <div className="flex flex-wrap items-center justify-between gap-4 pt-1">
            <div>
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded bg-surface-container text-on-surface-variant font-metadata-caps uppercase tracking-wider font-semibold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Scholar Activity &amp; Access Archive · Central University Library
              </div>
              <h1 className="font-headline-lg text-on-surface mt-2 tracking-tight">Reading History &amp; Circulation Archive</h1>
              <p className="font-body-md text-on-surface-variant max-w-3xl mt-1 leading-relaxed">
                Complete chronology of physical monographs, digitized rare folios, and inter-library loans accessioned to your scholar account.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button type="button" className="flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-container-lowest text-on-surface hover:bg-surface-container font-label-lg transition-colors shadow-sm">
                <Icon name="download" className="text-[18px] text-primary" />
                Export Archive (CSV)
              </button>
              <button type="button" title="Citation Managers" className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container font-label-md transition-colors shadow-sm">
                <Icon name="share" className="text-[18px]" />
                RIS / BibTeX
              </button>
            </div>
          </div>
        </div>

        <HistoryView entries={entries} books={historyBooks} year={summary.year} aside={<ReadingSummaryPanel summary={summary} />} />
      </div>
    </main>
  );
}

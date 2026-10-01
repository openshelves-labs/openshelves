import Link from "next/link";
import { getNewArrivals, getRecommendedBooks, getCatalogStats } from "@/lib/api";
import Icon from "@/components/Icon";
import ArrivalsView from "@/components/arrivals/ArrivalsView";
import AffinityMatch from "@/components/arrivals/AffinityMatch";

export default async function Page() {
  const [arrivals, recommended, stats] = await Promise.all([getNewArrivals(), getRecommendedBooks(), getCatalogStats()]);
  // Register tiles are the flagged accessions; affinity matches are the top-ranked recommendations that aren't in the register.
  const tiles = arrivals.filter((b) => b.arrivalBadge);
  const tileIds = new Set(tiles.map((b) => b.id));
  const matches = recommended.filter((b) => !tileIds.has(b.id)).slice(0, 4);

  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <div className="flex flex-col w-full">
        <div className="flex flex-col gap-6 pb-2">
          <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-on-surface-variant font-label-md text-sm">
            <Link className="hover:text-primary transition-colors flex items-center gap-1" href="/">
              <Icon name="explore" className="text-[16px]" />
              <span>Home</span>
            </Link>
            <Icon name="chevron_right" className="text-[14px] text-outline" />
            <span>Discover</span>
            <Icon name="chevron_right" className="text-[14px] text-outline" />
            <span className="text-on-surface font-semibold">New Arrivals &amp; Recent Accessions</span>
          </nav>

          <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 bg-surface-container-lowest p-6 rounded-xl shadow-sm border border-border-warm">
            <div className="space-y-2 max-w-2xl">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-[#c6ebd3]/60 text-primary font-metadata-caps uppercase font-semibold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
                <span>Accession Ledger · Cycle 28-B</span>
              </div>
              <h1 className="font-title-editorial text-3xl font-semibold text-on-surface tracking-tight">New Arrivals</h1>
              <p className="font-body-md text-sm text-on-surface-variant leading-relaxed">
                Recently accessioned monographs, critical editions, and serial volumes across the Central University Library System.
              </p>
            </div>
            <div className="flex items-center gap-4 bg-[#F3F1EC] border border-border-warm px-5 py-3.5 rounded-xl shrink-0">
              <div className="w-10 h-10 rounded-lg bg-[#c6ebd3]/60 text-primary flex items-center justify-center shrink-0">
                <Icon name="inventory_2" className="text-[22px]" />
              </div>
              <div className="flex flex-col">
                <div className="flex items-baseline gap-1.5">
                  <span className="font-title-editorial text-xl font-bold text-primary">{stats.newArrivals}</span>
                  <span className="font-label-lg text-xs font-semibold text-on-surface">new acquisitions</span>
                </div>
                <span className="font-metadata-caps text-[10px] text-outline uppercase tracking-wider">Indexed today, 07:45 GMT</span>
              </div>
            </div>
          </div>
        </div>

        <ArrivalsView books={tiles} />
        <AffinityMatch books={matches} />
      </div>
    </main>
  );
}

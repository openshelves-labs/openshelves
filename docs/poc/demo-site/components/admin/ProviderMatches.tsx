import type { CatalogProviderId, CatalogProviderMatch, CatalogRecord } from "@/types";
import BookCover from "@/components/BookCover";
import Icon from "@/components/Icon";

const surnames = (authors: string) =>
  new Set(
    authors
      .split(/[;,]/)
      .map((a) => a.trim().split(/\s+/).pop()?.toLowerCase() ?? "")
      .filter(Boolean),
  );

const subset = (a: Set<string>, b: Set<string>) => [...a].every((x) => b.has(x));

/** Number of authors/date conflicts between a provider's record and the current form values. */
export function conflictCount(p: CatalogProviderMatch, current: CatalogRecord): number {
  let n = 0;
  if (p.record.year.trim() !== current.year.trim()) n++;
  const a = surnames(p.record.authors);
  const b = surnames(current.authors);
  if (!subset(a, b) && !subset(b, a)) n++;
  return n;
}

interface Props {
  providers: CatalogProviderMatch[];
  selectedId: CatalogProviderId;
  current: CatalogRecord;
  loading: boolean;
  querySummary: string;
  onSelect: (id: CatalogProviderId) => void;
}

export default function ProviderMatches({ providers, selectedId, current, loading, querySummary, onSelect }: Props) {
  return (
    <section className="space-y-space-md">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="font-headline-sm text-headline-sm text-on-surface">
            {loading ? "Querying registries…" : `${providers.length} Provider Matches Found`}
          </h2>
          {!loading && querySummary && (
            <span className="rounded-full bg-secondary-container px-2 py-0.5 font-label-md text-label-md font-semibold tabular-nums text-on-secondary-container">
              {querySummary}
            </span>
          )}
        </div>
        <span className="font-metadata-caps text-metadata-caps uppercase text-outline">Auto-Merged • Base schema applied</span>
      </div>

      <div aria-busy={loading} className={`grid grid-cols-1 gap-space-md md:grid-cols-3 ${loading ? "animate-pulse opacity-50" : ""}`}>
        {providers.map((p) => {
          const selected = p.id === selectedId;
          const conflicts = conflictCount(p, current);
          return (
            <article
              key={p.id}
              className={`relative flex flex-col justify-between space-y-space-md overflow-hidden rounded-xl bg-surface-container-lowest p-space-md transition-colors ${
                selected ? "shadow-md" : "shadow-sm hover:bg-surface-container-low"
              }`}
            >
              {selected && <div className="absolute inset-x-0 top-0 h-1 bg-primary" />}
              <div className="space-y-space-sm">
                <div className="flex items-start justify-between gap-2">
                  <span
                    className={`inline-flex items-center gap-1 rounded px-2 py-0.5 font-metadata-caps text-metadata-caps uppercase ${
                      selected ? "bg-primary font-bold text-on-primary" : "bg-surface-container-highest font-medium text-on-surface"
                    }`}
                  >
                    {selected && <Icon name="verified" className="text-[12px]" />}
                    {p.name} • {p.confidence}%
                  </span>
                  <span className={`font-metadata-caps text-metadata-caps ${selected ? "font-semibold text-primary" : "text-outline"}`}>{p.badge}</span>
                </div>
                <div className="flex gap-space-sm pt-space-xs">
                  <BookCover
                    book={{ title: p.card.title, coverUrl: p.coverUrl, coverTone: p.coverTone, contributors: [] }}
                    className="h-24 w-16 shrink-0 rounded shadow-sm"
                    showTitleFallback={false}
                  />
                  <div className="flex min-w-0 flex-col">
                    <h3 className={`line-clamp-2 font-headline-sm text-body-lg leading-tight text-on-surface ${selected ? "font-semibold" : "font-medium"}`}>
                      {p.card.title}
                    </h3>
                    <p className="mt-1 truncate font-body-sm text-body-sm text-on-surface-variant">{p.card.authors}</p>
                    <div className="mt-2 font-label-md text-[12px] tabular-nums text-outline">
                      <span>{p.card.publisher}</span> • <span>{p.card.yearLabel}</span>
                    </div>
                  </div>
                </div>
              </div>
              {selected ? (
                <button
                  type="button"
                  aria-pressed="true"
                  className="flex h-9 w-full cursor-default items-center justify-center gap-1.5 rounded bg-primary font-label-md text-label-md font-semibold text-on-primary shadow-sm"
                >
                  <Icon name="check_circle" className="text-[16px]" />
                  Selected as Base Schema
                </button>
              ) : (
                <button
                  type="button"
                  aria-pressed="false"
                  onClick={() => onSelect(p.id)}
                  className="flex h-9 w-full items-center justify-center gap-1.5 rounded bg-surface-container-lowest font-label-md text-label-md text-on-surface transition-colors hover:bg-surface-container"
                >
                  <Icon name="compare_arrows" className="text-[16px]" />
                  Compare Fields{conflicts > 0 ? ` (${conflicts} Conflict${conflicts > 1 ? "s" : ""})` : ""}
                </button>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}

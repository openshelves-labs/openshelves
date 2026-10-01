import Icon from "@/components/Icon";
import { pageWindow } from "@/lib/catalog";

interface Props {
  page: number;
  totalPages: number;
  from: number;
  to: number;
  total: number;
  onPage: (p: number) => void;
}

export default function Pagination({ page, totalPages, from, to, total, onPage }: Props) {
  return (
    <section className="bg-surface-container-lowest rounded-xl p-space-md shadow-sm mt-space-lg flex flex-col md:flex-row items-center justify-between gap-space-md">
      <div className="text-body-sm font-body-sm text-on-surface-variant text-center md:text-left">
        Showing <span className="font-semibold text-on-surface font-label-lg">{total ? `${from}–${to}` : "0"}</span> of{" "}
        <span className="font-semibold text-on-surface font-label-lg">{total.toLocaleString("en-US")}</span> catalog matches
      </div>
      <nav aria-label="Catalog results pagination" className="flex items-center gap-1">
        <button
          type="button"
          aria-label="Previous page"
          disabled={page <= 1}
          onClick={() => onPage(page - 1)}
          className={`w-9 h-9 rounded-lg flex items-center justify-center transition-colors ${page <= 1 ? "text-outline-variant cursor-not-allowed" : "text-on-surface hover:bg-surface-container"}`}
        >
          <Icon name="chevron_left" className="text-lg" />
        </button>
        {pageWindow(page, totalPages).map((p, i) =>
          p === "…" ? (
            <span key={`gap-${i}`} className="px-1 text-outline font-metadata-caps">
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              aria-current={p === page ? "page" : undefined}
              onClick={() => onPage(p)}
              className={`w-9 h-9 rounded-lg font-label-lg text-label-md flex items-center justify-center transition-colors ${
                p === page ? "bg-primary text-on-primary font-semibold shadow-sm" : "text-on-surface hover:bg-surface-container"
              }`}
            >
              {p}
            </button>
          ),
        )}
        <button
          type="button"
          aria-label="Next page"
          disabled={page >= totalPages}
          onClick={() => onPage(page + 1)}
          className={`w-9 h-9 rounded-lg flex items-center justify-center transition-colors ${page >= totalPages ? "text-outline-variant cursor-not-allowed" : "text-on-surface hover:bg-surface-container"}`}
        >
          <Icon name="chevron_right" className="text-lg" />
        </button>
      </nav>
      <div className="flex items-center gap-space-xs">
        <button
          type="button"
          className="h-9 px-4 bg-surface-container hover:bg-surface-container-high text-on-surface rounded-lg font-label-lg text-label-md font-medium flex items-center gap-2 transition-colors"
        >
          <Icon name="download" className="text-base text-primary" />
          <span>Batch Export: EndNote / Zotero / CSV</span>
          <Icon name="arrow_drop_down" className="text-xs" />
        </button>
      </div>
    </section>
  );
}

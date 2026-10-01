import type { Serial } from "@/types";
import Icon from "@/components/Icon";

const ICON_BTN =
  "h-9 px-3 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface-variant hover:text-on-surface transition-colors";

interface SerialCardProps {
  serial: Serial;
  saved: boolean;
  onToggleSave: () => void;
  onBrowse: () => void;
}

/** One serial record in the directory. */
export default function SerialCard({ serial: s, saved, onToggleSave, onBrowse }: SerialCardProps) {
  return (
    <article className="bg-surface-container-lowest rounded-xl p-6 shadow-sm hover:shadow-md transition-shadow relative group">
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
        <div className="space-y-1.5 flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`px-2.5 py-0.5 rounded-full font-label-md ${
                s.accessTone === "green"
                  ? "bg-secondary-fixed text-primary font-semibold"
                  : "bg-surface-container-highest text-on-surface font-medium"
              }`}
            >
              {s.accessLabel}
            </span>
            <span className="px-2 py-0.5 rounded-full bg-surface-container-high text-on-surface-variant font-metadata-caps uppercase">
              {s.badge}
            </span>
          </div>
          <h2 className="font-headline-sm text-on-surface tracking-tight group-hover:text-primary transition-colors">
            <a className="focus:outline-none" href="#">
              {s.title}
            </a>
          </h2>
          <p className="font-body-sm text-on-surface-variant">
            Published by <span className="font-medium text-on-surface">{s.publisher}</span>
            {s.publisherTail}
            {s.publisherTailItalic && <span className="italic">{s.publisherTailItalic}</span>}
          </p>
        </div>
        <div className="shrink-0 bg-surface-container-low rounded-lg p-3 text-center sm:text-right flex sm:flex-col justify-between items-center sm:items-end gap-1">
          <span className="font-metadata-caps uppercase text-outline">Impact Factor</span>
          <span className="font-headline-sm text-primary leading-none tabular-nums">{s.impactFactor.toFixed(2)}</span>
          <span className="font-label-md text-secondary">{s.sjrLabel}</span>
        </div>
      </div>

      <div className="mt-4 pt-4 bg-surface-container-low/60 rounded-lg p-4 space-y-2">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 font-body-sm text-on-surface-variant">
          <span>
            <strong className="font-medium text-on-surface">ISSN:</strong> <span className="tabular-nums">{s.issn}</span>
          </span>
          <span className="text-outline">•</span>
          <span>
            <strong className="font-medium text-on-surface">E-ISSN:</strong> <span className="tabular-nums">{s.eIssn}</span>
          </span>
          {s.facts.map((f) => (
            <span key={f.label} className="contents">
              <span className="text-outline">•</span>
              <span>
                <strong className="font-medium text-on-surface">{f.label}:</strong> {f.value}
              </span>
            </span>
          ))}
        </div>
        <div className="flex items-start gap-2 pt-1 font-body-sm text-on-surface">
          <Icon name="inventory" className="text-primary text-base mt-0.5 shrink-0" />
          <span>
            <strong className="font-medium">{s.holdingsLabel}:</strong> {s.holdings}
          </span>
        </div>
      </div>

      <div className="mt-4 pt-3 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="font-metadata-caps uppercase text-outline mr-1">Taxonomy:</span>
          {s.taxonomy.map((t) => (
            <span key={t} className="px-2 py-0.5 rounded bg-surface-container-high text-on-surface-variant font-label-md">
              {t}
            </span>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onBrowse}
            className="h-9 px-3.5 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-md transition-colors flex items-center gap-1.5"
          >
            <Icon name="dataset" className="text-[17px]" />
            <span>Browse Issues</span>
          </button>
          {s.stacks && (
            <button
              type="button"
              className="h-9 px-3.5 rounded-lg bg-surface-container-lowest border-0 hover:bg-surface-container-high text-primary font-label-md transition-colors flex items-center gap-1.5"
            >
              <Icon name="pin_drop" className="text-[17px]" />
              <span>Check Shelf</span>
            </button>
          )}
          <button
            type="button"
            title="Bookmark serial"
            aria-pressed={saved}
            onClick={onToggleSave}
            className={`${ICON_BTN} ${saved ? "text-primary" : ""}`}
          >
            <Icon name={saved ? "bookmark" : "bookmark_border"} filled={saved} className="text-lg" />
          </button>
          {!s.stacks && (
            <button type="button" title="Export Citation" className={ICON_BTN}>
              <Icon name="format_quote" className="text-lg" />
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

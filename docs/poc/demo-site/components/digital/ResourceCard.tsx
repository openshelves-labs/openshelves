import Icon from "@/components/Icon";
import type { DigitalResource } from "@/types";

export default function ResourceCard({
  resource: r,
  saved,
  onToggleSave,
  launchLabel,
}: {
  resource: DigitalResource;
  saved: boolean;
  onToggleSave: () => void;
  launchLabel: string;
}) {
  return (
    <article className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow flex flex-col space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="font-metadata-caps text-[11px] uppercase tracking-wider text-outline font-semibold">{r.vendor}</span>
            <span className="w-1.5 h-1.5 rounded-full bg-outline/40" />
            <span
              className={`font-metadata-caps text-[11px] font-semibold ${r.collectionTone === "primary" ? "text-primary" : "text-secondary"}`}
            >
              {r.collection}
            </span>
          </div>
          <h2 className="font-headline-md text-on-surface hover:text-primary transition-colors cursor-pointer">{r.title}</h2>
        </div>
        <div className="flex items-center gap-1.5 shrink-0">
          {r.access === "sso" ? (
            <span className="px-2.5 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-md font-semibold flex items-center gap-1">
              <Icon name="verified_user" className="text-[15px]" />
              Institutional SSO
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded-full bg-surface-variant text-on-surface font-label-md font-semibold flex items-center gap-1">
              <Icon name="lock_open" className="text-primary text-[15px]" />
              Open Access (Public)
            </span>
          )}
          {r.scope && (
            <span className="px-2.5 py-1 rounded-full bg-surface-container text-on-surface-variant font-label-md">{r.scope}</span>
          )}
        </div>
      </div>

      <p className="font-body-md text-on-surface-variant leading-relaxed">{r.description}</p>

      <div className="flex flex-wrap items-center gap-2 pt-1">
        {r.subjects.map((s) => (
          <span
            key={s}
            className="font-metadata-caps text-[10px] uppercase tracking-wide bg-surface-container-low text-on-surface px-2.5 py-1 rounded-md font-semibold"
          >
            {s}
          </span>
        ))}
      </div>

      <div className="bg-surface-container-low/70 rounded-xl p-3 flex flex-wrap items-center justify-between gap-3 font-body-sm text-on-surface-variant">
        <div className="flex items-center gap-4">
          {r.stats.map((s) => (
            <span key={s.label} className="flex items-center gap-1.5">
              <Icon name={s.icon} className="text-outline text-base" />
              {s.label}
            </span>
          ))}
        </div>
        <span className="text-primary font-label-md font-semibold">{r.highlight}</span>
      </div>

      <div className="flex items-center justify-between pt-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 h-10 px-5 rounded-lg bg-primary text-on-primary font-label-lg font-semibold hover:bg-primary/90 transition-colors shadow-sm"
          >
            <span>{launchLabel}</span>
            <Icon name="open_in_new" className="text-sm" />
          </button>
          <button
            type="button"
            onClick={onToggleSave}
            aria-pressed={saved}
            className={`h-10 px-4 rounded-lg font-label-lg transition-colors flex items-center gap-1.5 ${
              saved
                ? "bg-secondary-container text-on-secondary-container"
                : "bg-surface-container text-on-surface hover:bg-surface-container-high"
            }`}
          >
            <Icon name={saved ? "bookmark_added" : "bookmark_add"} filled={saved} className="text-base" />
            <span>{saved ? "Saved to Workspace" : "Save to Workspace"}</span>
          </button>
        </div>
        <button type="button" className="text-outline hover:text-on-surface font-label-md flex items-center gap-1">
          <span>{r.footerLink.label}</span>
          <Icon name={r.footerLink.icon} className="text-base" />
        </button>
      </div>
    </article>
  );
}

import type { ReadingSummary } from "@/types";
import Icon from "@/components/Icon";

export default function ReadingSummaryPanel({ summary }: { summary: ReadingSummary }) {
  const pct = Math.round((summary.benchmark.current / summary.benchmark.target) * 100);
  const cells = [
    { value: summary.physical, label: "Physical Monographs" },
    { value: summary.digital, label: "Digital / ILL Folios" },
    { value: summary.avgDaysLoaned, label: "Avg Days Loaned" },
  ];
  return (
    <div className="lg:col-span-4 space-y-6">
      <div className="sticky top-24 space-y-6">
        <div className="p-6 bg-surface-container-lowest rounded-xl shadow-sm space-y-6">
          <div className="flex items-center justify-between pb-3">
            <div>
              <span className="font-metadata-caps text-outline uppercase tracking-wider block">Scholar Analytics</span>
              <h3 className="font-headline-sm font-title-editorial text-on-surface">Annual Reading Summary</h3>
            </div>
            <span className="px-2.5 py-1 rounded bg-surface-container text-primary font-semibold font-metadata-caps text-[11px]">Year: {summary.year}</span>
          </div>

          <div className="p-4 rounded-lg bg-surface-container-low flex items-baseline justify-between">
            <div>
              <div className="font-display-lg font-title-editorial text-primary leading-none">{summary.worksRead}</div>
              <div className="font-label-md text-on-surface-variant mt-1.5 font-medium">Books &amp; Works Read</div>
            </div>
            <div className="flex items-center gap-1 text-primary font-label-md font-semibold bg-[#E4EEE8] px-2 py-1 rounded">
              <Icon name="trending_up" className="text-[16px]" />
              <span>
                {summary.deltaVsPrevious >= 0 ? "+" : "−"}
                {Math.abs(summary.deltaVsPrevious)} vs. {summary.year - 1}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2.5 text-center">
            {cells.map((c) => (
              <div key={c.label} className="p-3 rounded-lg bg-surface-container-low">
                <div className="font-headline-sm font-title-editorial text-on-surface">{c.value}</div>
                <div className="font-metadata-caps text-[10px] text-outline uppercase tracking-tight mt-0.5">{c.label}</div>
              </div>
            ))}
          </div>

          <div className="space-y-2 pt-2">
            <div className="flex justify-between items-center font-label-md">
              <span className="text-on-surface font-medium">Annual Reading Goal</span>
              <span className="text-primary font-semibold">
                {summary.benchmark.current} / {summary.benchmark.target} Works ({pct}%)
              </span>
            </div>
            <div className="w-full h-2.5 bg-surface-container rounded-full overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: `${Math.min(pct, 100)}%` }} />
            </div>
            <p className="font-body-sm text-xs text-outline leading-tight">On track to surpass your annual reading goal by November.</p>
          </div>

          <div className="pt-2">
            <span className="font-metadata-caps text-outline uppercase tracking-wider block mb-2.5 font-semibold">Primary Academic Subjects</span>
            <div className="flex flex-wrap gap-2">
              {summary.subjects.map((s) => (
                <span key={s.tag} className="px-2.5 py-1 rounded-full bg-surface-container-low text-on-surface font-metadata-caps text-[11px] font-medium">
                  #{s.tag} <strong className="text-primary ml-1">{s.count}</strong>
                </span>
              ))}
            </div>
          </div>

          <div className="space-y-2.5 pt-2">
            <button type="button" className="w-full h-10 px-4 rounded-lg bg-primary text-on-primary hover:bg-[#1E3F2E] font-label-lg flex items-center justify-center gap-2 transition-colors shadow-sm">
              <Icon name="download_for_offline" className="text-[18px]" />
              <span>Export History (CSV)</span>
            </button>
            <button type="button" className="w-full h-9 px-4 rounded-lg bg-surface-container-low text-on-surface hover:bg-surface-container font-label-md flex items-center justify-center gap-2 transition-colors">
              <Icon name="auto_stories" className="text-[16px] text-primary" />
              <span>Sync with Zotero Library</span>
            </button>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-surface-container text-on-surface-variant flex items-start gap-3">
          <Icon name="shield" className="text-primary text-[20px] mt-0.5 shrink-0" />
          <div className="space-y-1">
            <h4 className="font-label-md font-semibold text-on-surface leading-tight">Archival Privacy Notice</h4>
            <p className="font-body-sm text-[12px] leading-relaxed text-outline">
              Borrowing history is strictly confidential to your Cambridge scholar credentials and retained in full compliance with University Library Data Ethics &amp; Preservations policies.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

"use client";

import Icon from "@/components/Icon";
import { useLibrary } from "@/lib/store";

const TEMPLATES = [
  { label: "Course Syllabus Template", icon: "school", tone: "text-primary" },
  { label: "Blank Research Folio", icon: "description", tone: "text-primary" },
  { label: "Import Zotero / RIS File", icon: "cloud_download", tone: "text-secondary" },
];

/** The dashed "Create New Reading List" card at the end of the grid. */
export default function CreateListTile() {
  const { openNewList } = useLibrary();
  return (
    <article className="group relative flex flex-col justify-between rounded-xl border border-[#E5E0D8] bg-white p-5 transition-all duration-200 hover:border-primary/40">
      <button type="button" onClick={openNewList} className="flex flex-col items-center space-y-2.5 pt-1 text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-full border border-primary/20 bg-[#c6ebd3]/70 text-primary transition-transform group-hover:scale-105">
          <Icon name="add" className="text-2xl" />
        </span>
        <span className="block font-title-editorial text-base font-semibold text-on-surface">Create New Reading List</span>
        <span className="block max-w-xs font-body-sm text-xs leading-relaxed text-on-surface-variant">
          Group monographs, journal papers, and research data into a course syllabus, grant bibliography, or personal folio.
        </span>
      </button>
      <div className="mt-4 space-y-2 border-t border-[#E5E0D8] pt-3">
        <span className="block text-center font-metadata-caps text-[10px] font-medium uppercase tracking-wider text-outline">Quick Start Templates</span>
        <div className="flex flex-col gap-1.5">
          {TEMPLATES.map((t) => (
            <button
              key={t.label}
              type="button"
              onClick={openNewList}
              className="flex h-8 w-full items-center justify-between rounded-lg border border-[#E5E0D8] bg-surface-paper px-3 font-label-lg text-xs text-on-surface transition-colors hover:bg-stone-100"
            >
              <span className="flex items-center gap-2">
                <Icon name={t.icon} className={`text-sm ${t.tone}`} />
                <span>{t.label}</span>
              </span>
              <Icon name="chevron_right" className="text-sm text-outline" />
            </button>
          ))}
        </div>
      </div>
    </article>
  );
}

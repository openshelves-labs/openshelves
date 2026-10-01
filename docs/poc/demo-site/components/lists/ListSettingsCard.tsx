"use client";

import { useState } from "react";
import type { ListVisibility, ReadingList } from "@/types";
import Icon from "@/components/Icon";

interface Props {
  list: ReadingList;
  ownerName: string;
}

export default function ListSettingsCard({ list, ownerName }: Props) {
  const [visibility, setVisibility] = useState<ListVisibility>(list.visibility);
  const [autoSync, setAutoSync] = useState(list.settings?.autoSync ?? false);
  const [notify, setNotify] = useState(list.settings?.notifyStudents ?? false);

  const options: { id: ListVisibility; title: string; blurb: string }[] = [
    { id: "private", title: "Private (Only You)", blurb: `Solely accessible to ${ownerName}.` },
    {
      id: "cohort",
      title: "Course / Cohort",
      blurb: `Accessible to ${list.enrolledCount ? `${list.enrolledCount} enrolled students` : "enrolled students"}${
        list.category.startsWith("Course") ? " in Michaelmas Biogeochemistry Tripos" : ""
      } with student comment permissions.`,
    },
    { id: "public", title: "Public Academic", blurb: "Discoverable across Cambridge Library Network and scholarly indexing." },
  ];

  return (
    <section className="space-y-4 rounded-xl border border-[#E5E0D8] bg-surface-container-lowest p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 font-headline-sm text-base font-semibold text-on-surface">
          <Icon name="admin_panel_settings" className="text-lg text-primary" />
          List Settings &amp; Access
        </h3>
        <span className="font-metadata-caps text-[10px] font-bold uppercase tracking-wider text-primary">Sync Active</span>
      </div>

      <div className="space-y-2.5" role="radiogroup" aria-label="Visibility tier">
        <span className="block font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">Visibility Tier</span>
        {options.map((o) => {
          const on = visibility === o.id;
          return (
            <label
              key={o.id}
              className={`flex cursor-pointer items-start gap-2.5 rounded-lg p-2.5 transition-colors ${
                on ? "bg-secondary-container/30" : "border border-transparent hover:bg-surface-container-low"
              }`}
            >
              <input type="radio" name="visibility" checked={on} onChange={() => setVisibility(o.id)} className="mt-0.5 h-4 w-4 text-primary focus:ring-primary" />
              <div className="text-xs">
                <span className={`block font-label-md font-semibold ${on ? "text-primary" : "text-on-surface"}`}>
                  {o.title}
                  {on && o.id !== "private" && o.id !== "public" ? " (Active)" : ""}
                </span>
                <span className="font-body-sm text-[11px] leading-snug text-on-surface-variant">{o.blurb}</span>
              </div>
            </label>
          );
        })}
      </div>

      <div className="space-y-2.5 pt-1">
        <div className="flex items-center justify-between">
          <span className="font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">Teaching Staff</span>
          <button type="button" className="font-label-md text-xs font-semibold text-primary hover:underline">
            + Add
          </button>
        </div>
        <div className="space-y-2">
          {list.collaborators.length === 0 && <p className="text-[11px] text-outline">No collaborators yet.</p>}
          {list.collaborators.map((c) => (
            <div key={c.name} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-surface-container-highest font-metadata-caps text-[10px] font-bold text-on-surface-variant">
                  {c.initials}
                </div>
                <div>
                  <span className="block font-label-md font-semibold leading-tight text-on-surface">{c.name}</span>
                  <span className="text-[10px] text-outline">{c.role}</span>
                </div>
              </div>
              {c.status && <span className="font-metadata-caps text-[10px] text-outline">{c.status}</span>}
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-2.5 pt-2">
        <span className="block font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">Automation &amp; Feeds</span>
        <label className="flex cursor-pointer items-center justify-between text-xs">
          <span className="font-body-sm text-on-surface">Auto-sync with Cambridge Moodle / VLE</span>
          <input type="checkbox" checked={autoSync} onChange={(e) => setAutoSync(e.target.checked)} className="h-4 w-4 rounded text-primary focus:ring-primary" />
        </label>
        <label className="flex cursor-pointer items-center justify-between text-xs">
          <span className="font-body-sm text-on-surface">Notify students when new items added</span>
          <input type="checkbox" checked={notify} onChange={(e) => setNotify(e.target.checked)} className="h-4 w-4 rounded text-primary focus:ring-primary" />
        </label>
      </div>

      <div className="flex items-center justify-between pt-4 font-label-md text-xs text-outline">
        <button type="button" className="underline hover:text-on-surface">
          Archive Syllabus
        </button>
        <button type="button" className="underline hover:text-error">
          Delete Folio
        </button>
      </div>
    </section>
  );
}

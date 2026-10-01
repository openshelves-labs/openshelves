"use client";

import { useState, type ReactNode } from "react";
import type { Book, Copy } from "@/types";
import Icon from "@/components/Icon";
import HoldButton from "@/components/HoldButton";
import { formatDate } from "@/lib/dates";

type TabKey = "overview" | "classification" | "holdings";

const LABEL = "font-metadata-caps text-[11px] uppercase tracking-wider text-outline font-bold";
const CARD = "p-3.5 bg-surface-container-low rounded-xl border border-[#E5E0D8] flex flex-col gap-1";
const CARD_LABEL = "font-metadata-caps text-[10px] text-outline uppercase font-semibold";

function MetaCard({ label, children, note }: { label: string; children: ReactNode; note?: string }) {
  return (
    <div className={CARD}>
      <span className={CARD_LABEL}>{label}</span>
      {children}
      {note && <span className="text-[11px] text-outline">{note}</span>}
    </div>
  );
}
const VALUE = "font-label-lg text-sm text-on-surface font-semibold";

function Overview({ book }: { book: Book }) {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <h3 className={`${LABEL} flex items-center gap-1.5`}>
          <Icon name="subject" className="text-sm text-primary" />
          Scholarly Abstract
        </h3>
        <div className="bg-surface-container-low p-4 rounded-lg">
          <p className="font-body-md text-on-surface text-sm sm:text-base leading-relaxed">{book.abstract ?? "No abstract has been catalogued for this record yet."}</p>
        </div>
      </div>

      <div className="space-y-2.5">
        <h4 className={LABEL}>Controlled Subject Vocabularies</h4>
        <div className="flex flex-wrap gap-2">
          {book.subjects.map((s, i) => (
            <span key={s} className="px-3 py-1 rounded-full bg-[#E4EEE8] text-primary hover:bg-primary hover:text-on-primary transition-colors text-xs font-label-md border border-primary/20 flex items-center gap-1.5 cursor-pointer">
              {i === 0 && <Icon name="label" className="text-xs" />}
              {s}
            </span>
          ))}
          <span className="px-3 py-1 rounded-full bg-surface-container-high text-on-surface-variant text-xs font-label-md border border-[#E5E0D8]">
            {book.publisher} {book.publicationYear}
          </span>
        </div>
      </div>

      {book.tableOfContents && book.tableOfContents.length > 0 && (
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between">
            <h4 className={LABEL}>Archival Table of Contents</h4>
            <button type="button" onClick={() => setCollapsed((c) => !c)} className="font-label-md text-xs text-primary hover:underline flex items-center gap-1">
              <span>{collapsed ? "Expand All" : "Collapse All"}</span>
              <Icon name={collapsed ? "unfold_more" : "unfold_less"} className="text-sm" />
            </button>
          </div>
          {!collapsed && (
            <div className="divide-y divide-[#E5E0D8] border border-[#E5E0D8] rounded-xl overflow-hidden font-body-sm text-xs">
              {book.tableOfContents.map((part) => (
                <div key={part.title} className="p-3.5 bg-surface-container-low hover:bg-surface-container transition-colors">
                  <div className="flex items-center justify-between font-label-lg text-primary text-xs font-semibold">
                    <span>{part.title}</span>
                    {part.pages && <span className="font-metadata-caps text-[11px] text-outline">{part.pages}</span>}
                  </div>
                  <div className="mt-2 space-y-1.5 pl-3 border-l-2 border-[#E5E0D8] text-on-surface-variant">
                    {part.chapters.map((ch) => (
                      <div key={ch.title} className="flex justify-between">
                        <span>{ch.title}</span>
                        {ch.page && <span className="text-outline">{ch.page}</span>}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function Classification({ book }: { book: Book }) {
  const d = book.detail;
  const series = d?.seriesTitle ?? book.series;
  return (
    <div className="space-y-5">
      <h4 className={LABEL}>Cataloging &amp; Bibliographic Schema</h4>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 font-body-sm text-xs">
        {book.isbn && (
          <MetaCard label="Standard Book Number (ISBN-13)" note={d?.binding}>
            <span className={VALUE}>{book.isbn}</span>
          </MetaCard>
        )}
        {book.doi && (
          <MetaCard label="Permanent Object Identifier (DOI)" note={d?.doiRegistry}>
            <a href={`https://doi.org/${book.doi}`} target="_blank" rel="noreferrer" className="font-label-lg text-sm text-primary hover:underline font-semibold flex items-center gap-1">
              {book.doi}
              <Icon name="open_in_new" className="text-xs" />
            </a>
          </MetaCard>
        )}
        <MetaCard label="Library of Congress Classification" note={d?.lccNote}>
          <span className={VALUE}>{d?.lcc ?? book.callNumber}</span>
        </MetaCard>
        {d?.dewey && (
          <MetaCard label="Dewey Decimal System" note={d.deweyNote}>
            <span className={VALUE}>{d.dewey}</span>
          </MetaCard>
        )}
        {(d?.extent || book.pages) && (
          <MetaCard label="Format & Extent" note={d?.extentNote}>
            <span className={VALUE}>{d?.extent ?? `${book.pages} pages`}</span>
          </MetaCard>
        )}
        {series && (
          <MetaCard label="Monograph Series" note={d?.seriesNote}>
            <span className={VALUE}>{series}</span>
          </MetaCard>
        )}
      </div>
      {d?.rightsTitle && (
        <div className="p-4 rounded-xl bg-surface-container-low border border-[#E5E0D8] flex items-center justify-between">
          <div>
            <div className="font-label-lg text-xs font-semibold text-on-surface">{d.rightsTitle}</div>
            <div className="font-body-sm text-xs text-outline mt-0.5">{d.rightsNote}</div>
          </div>
          <Icon name="vpn_key" className="text-primary text-xl" />
        </div>
      )}
    </div>
  );
}

const COPY_CHIP = "inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-metadata-caps text-[10px] font-bold";

function CopyStatus({ copy }: { copy: Copy }) {
  switch (copy.status) {
    case "available":
      return (
        <span className={`${COPY_CHIP} bg-[#E4EEE8] text-primary`}>
          <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Available
        </span>
      );
    case "on_loan":
      return (
        <span className={`${COPY_CHIP} bg-[#FFF4E5] text-[#975A16]`}>
          <Icon name="schedule" className="text-[12px]" /> {copy.dueDate ? `Due ${formatDate(copy.dueDate)}` : "On Loan"}
        </span>
      );
    case "reserve":
      return (
        <span className={`${COPY_CHIP} bg-[#E4EEE8] text-primary`}>
          <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Available (Reserve)
        </span>
      );
    case "in_transit":
      return (
        <span className={`${COPY_CHIP} bg-[#FFF4E5] text-[#975A16]`}>
          <Icon name="local_shipping" className="text-[12px]" /> In Transit
        </span>
      );
    default:
      return <span className={`${COPY_CHIP} bg-surface-container-high text-on-surface-variant`}>Non-circulating</span>;
  }
}

function Holdings({ book }: { book: Book }) {
  const waitlist = `Join Waitlist #${(book.holdsInQueue ?? 0) + 1}`;
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h4 className={LABEL}>Physical Holdings across Central Library Network</h4>
          <p className="font-body-sm text-xs text-on-surface-variant">Real-time status synchronized with Circulation Desk Ledger</p>
        </div>
        <span className="font-metadata-caps text-[11px] text-outline">Updated 4 mins ago</span>
      </div>
      {book.copies.length === 0 ? (
        <div className="rounded-xl border border-[#E5E0D8] bg-surface-container-low p-4 font-body-sm text-xs text-on-surface-variant">
          No physical copies are held — this resource is available digitally ({book.availabilityLabel}).
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-[#E5E0D8]">
          <table className="w-full text-left font-body-sm text-xs">
            <thead className="bg-[#FAF8F5] text-outline font-metadata-caps uppercase text-[10px] border-b border-[#E5E0D8]">
              <tr>
                <th className="px-4 py-3 font-semibold">Barcode / Accession</th>
                <th className="px-4 py-3 font-semibold">Branch Library</th>
                <th className="px-4 py-3 font-semibold">Shelf Location</th>
                <th className="px-4 py-3 font-semibold">Format</th>
                <th className="px-4 py-3 font-semibold">Loan Status</th>
                <th className="px-4 py-3 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E5E0D8] bg-surface-container-lowest">
              {book.copies.map((cp) => {
                const free = cp.status === "available" || cp.status === "reserve";
                return (
                  <tr key={cp.id} className="hover:bg-surface-container-low/60 transition-colors">
                    <td className="px-4 py-3 font-mono font-medium text-primary">{cp.barcode}</td>
                    <td className="px-4 py-3 font-medium text-on-surface">{cp.branchName}</td>
                    <td className="px-4 py-3 text-on-surface-variant">{cp.location}</td>
                    <td className="px-4 py-3 text-outline">{cp.note ?? "Regular 28-day"}</td>
                    <td className="px-4 py-3">
                      <CopyStatus copy={cp} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      {cp.status === "non_circulating" ? (
                        <span className="text-outline">Reading room</span>
                      ) : (
                        <HoldButton
                          bookId={book.id}
                          label={free ? "Hold" : waitlist}
                          className={
                            free
                              ? "px-2.5 py-1 rounded bg-primary text-on-primary font-label-md text-xs hover:bg-[#1e3f2e] transition-colors"
                              : "px-2.5 py-1 rounded bg-surface-container-low border border-[#E5E0D8] text-on-surface-variant font-label-md text-xs hover:bg-surface-container transition-colors"
                          }
                          placedClassName="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-surface-container-low border border-primary/30 text-primary font-label-md text-xs"
                          iconClassName="text-xs"
                        />
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function DetailTabs({ book }: { book: Book }) {
  const [tab, setTab] = useState<TabKey>("overview");
  const tabs: { key: TabKey; label: string; icon: string; badge?: number }[] = [
    { key: "overview", label: "Overview & Abstract", icon: "article" },
    { key: "classification", label: "Classification & Metadata", icon: "category" },
    { key: "holdings", label: "Physical Copies & Locations", icon: "inventory_2", badge: book.copies.length || undefined },
  ];
  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-[#E5E0D8] overflow-hidden">
      <div role="tablist" className="flex border-b border-[#E5E0D8] bg-[#FAF8F5] px-4 overflow-x-auto">
        {tabs.map((t) => {
          const on = tab === t.key;
          return (
            <button
              key={t.key}
              type="button"
              role="tab"
              aria-selected={on}
              onClick={() => setTab(t.key)}
              className={`px-4 py-3.5 border-b-2 font-label-lg text-xs flex items-center gap-2 whitespace-nowrap ${
                on ? "border-primary text-primary font-bold" : "border-transparent text-on-surface-variant hover:text-on-surface"
              }`}
            >
              <Icon name={t.icon} className="text-base" />
              <span>{t.label}</span>
              {t.badge != null && <span className="font-metadata-caps text-[10px] bg-secondary-container text-on-secondary-container px-1.5 rounded-full font-bold">{t.badge}</span>}
            </button>
          );
        })}
      </div>
      <div role="tabpanel" className="p-6 sm:p-7">
        {tab === "overview" && <Overview book={book} />}
        {tab === "classification" && <Classification book={book} />}
        {tab === "holdings" && <Holdings book={book} />}
      </div>
    </div>
  );
}

"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import type { Book, ReadingList, ReadingListItem } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import ItemCard from "./ItemCard";
import ResourcePicker from "./ResourcePicker";
import BibliographyPreview from "./BibliographyPreview";
import ListSettingsCard from "./ListSettingsCard";
import { authorLine, firstFamily, fullTitle } from "./bookText";

type SortMode = "manual" | "author" | "year";
const SORTS: { id: SortMode; label: string }[] = [
  { id: "manual", label: "Manual Order" },
  { id: "author", label: "By Author" },
  { id: "year", label: "By Year" },
];

const VISIBILITY_BADGE = {
  private: { icon: "lock", label: "PRIVATE" },
  cohort: { icon: "lock_open", label: "COHORT ACCESSIBLE" },
  public: { icon: "public", label: "PUBLIC ACADEMIC" },
} as const;

interface Row {
  item: ReadingListItem;
  book: Book;
}

export default function ListDetailView({ id, initialList }: { id: string; initialList: ReadingList | null }) {
  const { lists, getBook, user } = useLibrary();
  const list = lists.find((l) => l.id === id) ?? initialList;
  const [sort, setSort] = useState<SortMode>("manual");
  const [q, setQ] = useState("");

  const rows: Row[] = useMemo(() => {
    if (!list) return [];
    return [...list.items]
      .sort((a, b) => a.position - b.position)
      .flatMap((item) => {
        const book = getBook(item.bookId);
        return book ? [{ item, book }] : [];
      });
  }, [list, getBook]);

  const shown = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const filtered = needle
      ? rows.filter(({ book }) => `${fullTitle(book)} ${authorLine(book)} ${book.subjects.join(" ")}`.toLowerCase().includes(needle))
      : rows;
    if (sort === "author") return [...filtered].sort((a, b) => firstFamily(a.book).localeCompare(firstFamily(b.book)));
    if (sort === "year") return [...filtered].sort((a, b) => b.book.publicationYear - a.book.publicationYear);
    return filtered;
  }, [rows, q, sort]);

  if (!list) {
    return (
      <main className="mx-auto w-full max-w-[1600px] px-8 py-16">
        <div className="mx-auto max-w-md space-y-3 rounded-xl border border-[#E5E0D8] bg-white p-8 text-center">
          <Icon name="folder_off" className="text-4xl text-outline" />
          <h1 className="font-headline-sm text-xl font-semibold text-on-surface">List not found</h1>
          <p className="font-body-sm text-sm text-on-surface-variant">This reading list doesn&apos;t exist or is no longer available in this session.</p>
          <Link href="/lists" className="inline-flex h-9 items-center rounded-lg bg-primary px-4 font-label-lg text-sm text-on-primary hover:bg-primary-container">
            Back to Reading Lists
          </Link>
        </div>
      </main>
    );
  }

  const badge = VISIBILITY_BADGE[list.visibility];
  const hidden = Math.max(0, list.itemCount - list.items.length);

  return (
    <main className="mx-auto w-full max-w-[1600px] space-y-9 px-8 py-8">
      <div className="flex w-full flex-col space-y-8">
        <div className="flex flex-col gap-3 rounded-xl border border-[#E5E0D8] bg-white p-6">
          <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-2 font-label-md text-on-surface-variant">
            <Link href="/" className="flex items-center gap-1 transition-colors hover:text-primary">
              <Icon name="home" className="text-sm" />
              <span>Home</span>
            </Link>
            <span className="text-xs text-outline">/</span>
            <span>My Library</span>
            <span className="text-xs text-outline">/</span>
            <Link href="/lists" className="transition-colors hover:text-primary">
              Reading Lists
            </Link>
            <span className="text-xs text-outline">/</span>
            <span className="max-w-xs truncate font-semibold text-on-surface md:max-w-md">{list.title}</span>
          </nav>

          <div className="flex flex-col justify-between gap-6 pt-1 lg:flex-row lg:items-start">
            <div className="flex-1 space-y-3">
              <div className="flex flex-wrap items-center gap-2.5">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary-container px-2.5 py-0.5 font-metadata-caps text-[11px] font-semibold uppercase tracking-wide text-on-secondary-container">
                  <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                  {list.category}
                </span>
                <span className="inline-flex items-center gap-1 rounded bg-surface-container px-2 py-0.5 font-metadata-caps text-[10px] text-on-surface-variant">
                  <Icon name={badge.icon} className="text-[13px] text-primary" />
                  {badge.label}
                </span>
              </div>
              <div className="group relative flex items-baseline gap-3">
                <h1 className="font-headline-lg tracking-tight text-on-surface">{list.title}</h1>
                <button type="button" aria-label="Edit syllabus title" className="rounded p-1 text-outline opacity-0 transition-opacity hover:bg-surface-container hover:text-primary group-hover:opacity-100">
                  <Icon name="edit" className="text-lg" />
                </button>
              </div>
              {list.description && <p className="max-w-3xl font-body-md leading-relaxed text-on-surface-variant">{list.description}</p>}
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2 pt-1 font-body-sm text-on-surface-variant">
                <div className="flex items-center gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[#2F5D45] font-title-editorial text-[10px] font-semibold text-white ring-1 ring-primary/20">
                    {user.initial}
                  </span>
                  <span className="font-label-md font-semibold text-on-surface">{user.name}</span>
                  <span className="text-xs text-outline">• Curator</span>
                </div>
                <span className="hidden text-outline sm:inline">•</span>
                <div className="flex items-center gap-1.5">
                  <Icon name="history" className="text-[16px] text-outline" />
                  <span>Last {list.updatedLabel.charAt(0).toLowerCase() + list.updatedLabel.slice(1)}</span>
                </div>
                <span className="hidden text-outline sm:inline">•</span>
                <div className="flex items-center gap-1.5">
                  <Icon name="auto_stories" className="text-[16px] text-primary" />
                  <span className="font-medium text-on-surface">{list.itemCount} resources indexed</span>
                </div>
                {!!list.enrolledCount && (
                  <>
                    <span className="hidden text-outline sm:inline">•</span>
                    <div className="flex items-center gap-1.5">
                      <Icon name="group" className="text-[16px] text-outline" />
                      <span>{list.enrolledCount} Enrolled Scholars</span>
                    </div>
                  </>
                )}
              </div>
            </div>
            <div className="flex shrink-0 flex-wrap items-center gap-2.5 sm:flex-nowrap">
              <ResourcePicker listId={list.id} />
              <button type="button" className="flex h-10 items-center gap-1.5 rounded-lg bg-surface-container-lowest px-3.5 font-label-lg text-sm text-on-surface shadow-sm transition-colors hover:bg-surface-container-low">
                <Icon name="file_download" className="text-lg text-primary" />
                <span>Export Bib</span>
                <Icon name="arrow_drop_down" className="text-sm text-outline" />
              </button>
              <button type="button" className="flex h-10 items-center gap-2 rounded-lg bg-surface-container-lowest px-3.5 font-label-lg text-sm text-on-surface shadow-sm transition-colors hover:bg-surface-container-low">
                <Icon name="share" className="text-lg text-outline" />
                <span>Share</span>
                <span className="rounded-full bg-surface-container px-1.5 py-0.5 font-metadata-caps text-[10px] font-semibold text-on-surface-variant">{list.collaborators.length + 1}</span>
              </button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-12">
          <div className="space-y-5 lg:col-span-8">
            <div className="flex flex-col justify-between gap-3 rounded-xl bg-surface-container-lowest p-3.5 shadow-sm md:flex-row md:items-center">
              <div className="flex flex-wrap items-center gap-2">
                <span className="pr-2 font-title-editorial text-sm font-semibold text-on-surface">
                  Curated Works{" "}
                  <span className="font-body-sm font-normal text-outline">
                    ({shown.length} displayed of {list.itemCount})
                  </span>
                </span>
                <div className="hidden h-4 w-px bg-surface-container-highest sm:block" />
                <div className="inline-flex rounded-lg bg-surface-container p-0.5 font-label-md text-xs" role="tablist">
                  {SORTS.map((s) => (
                    <button
                      key={s.id}
                      type="button"
                      role="tab"
                      aria-selected={sort === s.id}
                      onClick={() => setSort(s.id)}
                      className={`rounded-md px-2.5 py-1 ${sort === s.id ? "bg-surface-container-lowest font-semibold text-primary" : "text-on-surface-variant hover:text-on-surface"}`}
                    >
                      {s.label}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <div className="relative flex-1 md:w-48">
                  <Icon name="search" className="pointer-events-none absolute left-2.5 top-2 text-[16px] text-outline" />
                  <input
                    type="text"
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                    placeholder="Filter in list..."
                    className="h-8 w-full rounded-lg bg-surface-container-low pl-8 pr-3 font-body-sm text-xs text-on-surface transition-all placeholder:text-outline focus:bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <button type="button" title="Bulk selection" className="rounded-lg p-1.5 text-outline transition-colors hover:bg-surface-container hover:text-on-surface">
                  <Icon name="select_all" className="text-lg" />
                </button>
                <button type="button" title="Reorder list" className="rounded-lg p-1.5 text-outline transition-colors hover:bg-surface-container hover:text-on-surface">
                  <Icon name="reorder" className="text-lg" />
                </button>
              </div>
            </div>

            <div className="space-y-4">
              {shown.map(({ item, book }) => (
                <ItemCard key={book.id} listId={list.id} item={item} book={book} />
              ))}
              {shown.length === 0 && (
                <p className="rounded-xl border border-dashed border-[#E5E0D8] bg-white p-8 text-center font-body-sm text-sm text-on-surface-variant">
                  {rows.length === 0 ? "No resources in this list yet — use Add Resource to start curating." : "No works match your filter."}
                </p>
              )}
            </div>

            {hidden > 0 && (
              <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-4 text-xs text-on-surface-variant">
                <span className="font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">
                  {hidden} additional reading{hidden === 1 ? "" : "s"} in this {list.categoryKind === "course" ? "syllabus" : "folio"}
                </span>
                <button type="button" className="flex items-center gap-1 font-label-md font-semibold text-primary hover:underline">
                  <span>Expand All {list.itemCount} Readings</span>
                  <Icon name="expand_more" className="text-base" />
                </button>
              </div>
            )}
          </div>

          <div className="space-y-6 lg:sticky lg:top-20 lg:col-span-4">
            <BibliographyPreview books={rows.map((r) => r.book)} total={list.itemCount} />
            <ListSettingsCard key={list.id} list={list} ownerName={user.name} />
          </div>
        </div>
      </div>
    </main>
  );
}

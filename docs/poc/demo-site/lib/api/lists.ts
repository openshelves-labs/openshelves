import type { Book, ID, ReadingList } from "@/types";
import { books, lists } from "@/mock";
import { listsOverview, type ListsOverview } from "@/mock/lists";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getReadingLists(): Promise<ReadingList[]> {
  return clone(lists);
}

export async function getReadingList(id: ID): Promise<ReadingList | null> {
  const l = lists.find((x) => x.id === id);
  return l ? clone(l) : null;
}

export async function getListsOverview(): Promise<ListsOverview> {
  return clone(listsOverview);
}

/** One row of the "Recent Bibliographic Ledger": a work recently added to a folio. */
export interface LedgerEntry {
  id: string;
  book: Book;
  listId: ID;
  listShortTitle: string;
  addedOn: string;
}

/** Most recently added works across all folios, newest first. */
export async function getLedger(limit = 2): Promise<LedgerEntry[]> {
  const rows: LedgerEntry[] = [];
  for (const l of lists) {
    for (const it of l.items) {
      const book = books.find((b) => b.id === it.bookId);
      if (book) rows.push({ id: `${l.id}:${book.id}`, book, listId: l.id, listShortTitle: l.shortTitle ?? l.title, addedOn: it.addedOn });
    }
  }
  rows.sort((a, b) => b.addedOn.localeCompare(a.addedOn));
  return clone(rows.slice(0, limit));
}

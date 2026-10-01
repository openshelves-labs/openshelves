import type { Book, ID } from "@/types";
import { books, catalogStats } from "@/mock";

/**
 * Data layer — the ONLY place components/pages reach for data.
 * Every function is async and returns plain entity shapes so a real REST call
 * (`fetch(`${API}/books/${id}`)`) can replace the body without touching callers.
 */
const clone = <T,>(v: T): T => structuredClone(v);

export async function getBooks(): Promise<Book[]> {
  return clone(books);
}

export async function getBook(id: ID): Promise<Book | null> {
  const b = books.find((x) => x.id === id);
  return b ? clone(b) : null;
}

export async function getRelatedBooks(id: ID): Promise<Book[]> {
  const b = books.find((x) => x.id === id);
  const ids = b?.relatedIds ?? [];
  return clone(ids.map((rid) => books.find((x) => x.id === rid)).filter((x): x is Book => Boolean(x)));
}

/** Recently accessioned works, newest first. */
export async function getNewArrivals(): Promise<Book[]> {
  return clone(
    books
      .filter((b) => b.accessionDate)
      .sort((a, b) => (b.accessionDate ?? "").localeCompare(a.accessionDate ?? "")),
  );
}

/** Affinity-ranked recommendations. */
export async function getRecommendedBooks(): Promise<Book[]> {
  return clone(books.filter((b) => b.relevanceScore != null).sort((a, b) => (b.relevanceScore ?? 0) - (a.relevanceScore ?? 0)));
}

export interface CatalogStats {
  newArrivals: number;
  recommended: number;
  totalHoldings: string;
  catalogMatches: number;
}

export async function getCatalogStats(): Promise<CatalogStats> {
  return { ...catalogStats };
}

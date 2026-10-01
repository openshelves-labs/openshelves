import type { Book, Hold, Loan } from "@/types";
import { books, holds, loans } from "@/mock";
import { homeHighlightIds } from "@/mock/books-home";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getLoans(): Promise<Loan[]> {
  return clone(loans);
}

export async function getHolds(): Promise<Hold[]> {
  return clone(holds);
}

/** Five "New Arrivals" tiles shown on Home (a curated highlight set, distinct from the /new-arrivals feed). */
export async function getHomeHighlights(): Promise<Book[]> {
  return clone(homeHighlightIds.map((id) => books.find((b) => b.id === id)).filter((b): b is Book => Boolean(b)));
}

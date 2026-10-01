import type { Book } from "@/types";
import { coreBooks } from "./books-core";
import { catalogBooks } from "./books-catalog";
import { arrivalsBooks } from "./books-arrivals";
import { historyBooks } from "./books-history";
import { listsBooks } from "./books-lists";
import { homeBooks } from "./books-home";

export const books: Book[] = [...coreBooks, ...catalogBooks, ...arrivalsBooks, ...historyBooks, ...listsBooks, ...homeBooks];

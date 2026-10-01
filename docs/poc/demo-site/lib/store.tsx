"use client";

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import type { AppNotification, Book, Hold, ID, Loan, ReadingList, User } from "@/types";
import { addDays, NOW } from "@/lib/dates";
import CiteModal from "@/components/CiteModal";
import NewListModal from "@/components/NewListModal";
import NotificationsPanel from "@/components/NotificationsPanel";

/** Days a successful renewal extends a loan (matches "Extend to Nov 27" on a loan due Oct 27). */
export const RENEWAL_EXTENSION_DAYS = 31;
/** Days a ready hold is kept at the desk. */
export const HOLD_SHELF_DAYS = 7;

export interface LibraryInit {
  user: User;
  books: Book[];
  loans: Loan[];
  holds: Hold[];
  lists: ReadingList[];
  notifications: AppNotification[];
  outstandingBalance: number;
  newArrivalsCount: number;
  recommendedCount: number;
}

interface LibraryContextValue {
  user: User;
  books: Book[];
  getBook: (id: ID) => Book | undefined;

  loans: Loan[];
  holds: Hold[];
  lists: ReadingList[];
  savedBookIds: ID[];

  outstandingBalance: number;
  newArrivalsCount: number;
  recommendedCount: number;

  /* loans */
  canRenew: (loanId: ID) => boolean;
  renewLoan: (loanId: ID) => void;
  renewAllEligible: () => number;
  /** loan ids renewed during this session (drives "Renewed" button state) */
  renewedLoanIds: ID[];

  /* holds */
  holdFor: (bookId: ID) => Hold | undefined;
  loanFor: (bookId: ID) => Loan | undefined;
  placeHold: (bookId: ID) => void;
  cancelHold: (holdId: ID) => void;

  /* saved */
  isSaved: (bookId: ID) => boolean;
  toggleSave: (bookId: ID) => void;

  /* reading lists */
  isInList: (listId: ID, bookId: ID) => boolean;
  listsContaining: (bookId: ID) => ReadingList[];
  addToList: (listId: ID, bookId: ID) => void;
  removeFromList: (listId: ID, bookId: ID) => void;
  createList: (title: string, bookId?: ID) => ReadingList;

  /* notifications */
  notifications: AppNotification[];
  unreadCount: number;
  markNotificationRead: (id: ID) => void;
  markAllNotificationsRead: () => void;
  notificationsOpen: boolean;
  toggleNotifications: () => void;
  closeNotifications: () => void;

  /* modals */
  openCite: (book: Book) => void;
  openNewList: () => void;
}

const LibraryContext = createContext<LibraryContextValue | null>(null);

export function useLibrary(): LibraryContextValue {
  const ctx = useContext(LibraryContext);
  if (!ctx) throw new Error("useLibrary must be used inside <LibraryProvider>");
  return ctx;
}

export default function LibraryProvider({ init, children }: { init: LibraryInit; children: ReactNode }) {
  const [loans, setLoans] = useState<Loan[]>(init.loans);
  const [holds, setHolds] = useState<Hold[]>(init.holds);
  const [lists, setLists] = useState<ReadingList[]>(init.lists);
  const [savedBookIds, setSaved] = useState<ID[]>([]);
  const [renewedLoanIds, setRenewed] = useState<ID[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>(init.notifications);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [citeBook, setCiteBook] = useState<Book | null>(null);
  const [newListOpen, setNewListOpen] = useState(false);
  const [pendingBookId, setPendingBookId] = useState<ID | undefined>();

  const bookIndex = useMemo(() => new Map(init.books.map((b) => [b.id, b])), [init.books]);
  const getBook = useCallback((id: ID) => bookIndex.get(id), [bookIndex]);

  const canRenew = useCallback(
    (loanId: ID) => {
      const l = loans.find((x) => x.id === loanId);
      return !!l && l.renewalsUsed < l.renewalsMax;
    },
    [loans],
  );

  const renewLoan = useCallback((loanId: ID) => {
    setLoans((prev) =>
      prev.map((l) =>
        l.id === loanId && l.renewalsUsed < l.renewalsMax
          ? { ...l, dueDate: addDays(l.dueDate, RENEWAL_EXTENSION_DAYS), renewalsUsed: l.renewalsUsed + 1 }
          : l,
      ),
    );
    setRenewed((prev) => (prev.includes(loanId) ? prev : [...prev, loanId]));
  }, []);

  const renewAllEligible = useCallback(() => {
    const eligible = loans.filter((l) => l.renewalsUsed < l.renewalsMax);
    setLoans((prev) =>
      prev.map((l) =>
        l.renewalsUsed < l.renewalsMax
          ? { ...l, dueDate: addDays(l.dueDate, RENEWAL_EXTENSION_DAYS), renewalsUsed: l.renewalsUsed + 1 }
          : l,
      ),
    );
    setRenewed((prev) => Array.from(new Set([...prev, ...eligible.map((l) => l.id)])));
    return eligible.length;
  }, [loans]);

  const holdFor = useCallback((bookId: ID) => holds.find((h) => h.bookId === bookId), [holds]);
  const loanFor = useCallback((bookId: ID) => loans.find((l) => l.bookId === bookId), [loans]);

  const placeHold = useCallback(
    (bookId: ID) => {
      if (holds.some((h) => h.bookId === bookId)) return;
      const book = bookIndex.get(bookId);
      if (!book) return;
      const free = book.availabilityStatus === "available" || book.availabilityStatus === "online" || book.availabilityStatus === "open_access";
      const queueLength = (book.holdsInQueue ?? 0) + 1;
      const today = NOW.toISOString().slice(0, 10);
      const hold: Hold = {
        id: `h-${bookId}-${holds.length + 1}`,
        userId: init.user.id,
        bookId,
        status: free ? "ready" : "queued",
        placedOn: today,
        queuePosition: free ? 1 : queueLength,
        queueLength: free ? 1 : queueLength,
        pickupLocation: "Central Library Circulation Desk",
        pickupDetail: "Main Floor, West Wing",
        heldUntil: free ? addDays(today, HOLD_SHELF_DAYS) : undefined,
      };
      setHolds((prev) => [...prev, hold]);
    },
    [holds, bookIndex, init.user.id],
  );

  const cancelHold = useCallback((holdId: ID) => setHolds((prev) => prev.filter((h) => h.id !== holdId)), []);

  const isSaved = useCallback((id: ID) => savedBookIds.includes(id), [savedBookIds]);
  const toggleSave = useCallback(
    (id: ID) => setSaved((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id])),
    [],
  );

  const isInList = useCallback(
    (listId: ID, bookId: ID) => !!lists.find((l) => l.id === listId)?.items.some((i) => i.bookId === bookId),
    [lists],
  );
  const listsContaining = useCallback((bookId: ID) => lists.filter((l) => l.items.some((i) => i.bookId === bookId)), [lists]);

  const addToList = useCallback((listId: ID, bookId: ID) => {
    const today = NOW.toISOString().slice(0, 10);
    setLists((prev) =>
      prev.map((l) =>
        l.id !== listId || l.items.some((i) => i.bookId === bookId)
          ? l
          : {
              ...l,
              items: [...l.items, { bookId, position: l.items.length + 1, addedOn: today }],
              itemCount: l.itemCount + 1,
              updatedLabel: "Edited just now",
            },
      ),
    );
  }, []);

  const removeFromList = useCallback((listId: ID, bookId: ID) => {
    setLists((prev) =>
      prev.map((l) => {
        if (l.id !== listId || !l.items.some((i) => i.bookId === bookId)) return l;
        const items = l.items.filter((i) => i.bookId !== bookId).map((i, idx) => ({ ...i, position: idx + 1 }));
        return { ...l, items, itemCount: Math.max(0, l.itemCount - 1), updatedLabel: "Edited just now" };
      }),
    );
  }, []);

  const createList = useCallback(
    (title: string, bookId?: ID) => {
      const today = NOW.toISOString().slice(0, 10);
      const list: ReadingList = {
        id: `list-new-${lists.length + 1}`,
        ownerId: init.user.id,
        title,
        category: "Personal Archive",
        categoryKind: "archive",
        description: "",
        tags: [],
        visibility: "private",
        items: bookId ? [{ bookId, position: 1, addedOn: today }] : [],
        itemCount: bookId ? 1 : 0,
        collaborators: [],
        updatedAt: today,
        updatedLabel: "Edited just now",
      };
      setLists((prev) => [list, ...prev]);
      return list;
    },
    [lists.length, init.user.id],
  );

  const markNotificationRead = useCallback(
    (id: ID) => setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n))),
    [],
  );
  const markAllNotificationsRead = useCallback(() => setNotifications((prev) => prev.map((n) => ({ ...n, read: true }))), []);
  const toggleNotifications = useCallback(() => setNotificationsOpen((o) => !o), []);
  const closeNotifications = useCallback(() => setNotificationsOpen(false), []);

  const value: LibraryContextValue = {
    user: init.user,
    books: init.books,
    getBook,
    loans,
    holds,
    lists,
    savedBookIds,
    outstandingBalance: init.outstandingBalance,
    newArrivalsCount: init.newArrivalsCount,
    recommendedCount: init.recommendedCount,
    canRenew,
    renewLoan,
    renewAllEligible,
    renewedLoanIds,
    holdFor,
    loanFor,
    placeHold,
    cancelHold,
    isSaved,
    toggleSave,
    isInList,
    listsContaining,
    addToList,
    removeFromList,
    createList,
    notifications,
    unreadCount: notifications.filter((n) => !n.read).length,
    markNotificationRead,
    markAllNotificationsRead,
    notificationsOpen,
    toggleNotifications,
    closeNotifications,
    openCite: setCiteBook,
    openNewList: () => {
      setPendingBookId(undefined);
      setNewListOpen(true);
    },
  };

  return (
    <LibraryContext.Provider value={value}>
      {children}
      <NotificationsPanel />
      <CiteModal book={citeBook} onClose={() => setCiteBook(null)} />
      <NewListModal
        open={newListOpen}
        onClose={() => setNewListOpen(false)}
        onCreate={(title) => createList(title, pendingBookId)}
      />
    </LibraryContext.Provider>
  );
}

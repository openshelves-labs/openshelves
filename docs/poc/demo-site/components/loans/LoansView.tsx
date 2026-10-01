"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Book, Hold, Loan } from "@/types";
import { useLibrary } from "@/lib/store";
import { formatDate } from "@/lib/dates";
import Icon from "@/components/Icon";
import LoanRow from "@/components/LoanRow";
import HoldCard from "./HoldCard";
import LoanSummaryCards from "./LoanSummaryCards";
import Toast, { type ToastState } from "./Toast";

type Tab = "loans" | "holds";
type SortKey = "due" | "title" | "checkout" | "branch";

const SORTS: { key: SortKey; label: string }[] = [
  { key: "due", label: "Due Date (Earliest First)" },
  { key: "title", label: "Title (A-Z)" },
  { key: "checkout", label: "Checkout Date" },
  { key: "branch", label: "Library Branch" },
];

const TAB_BASE = "inline-flex items-center gap-2 rounded-full px-space-md py-2 font-label-lg text-label-md transition-colors";
const TAB_ON = "bg-secondary-fixed font-semibold text-primary";
const TAB_OFF = "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface";

function Tabs({ tab, onTab, loans, holds, historyCount, balance }: { tab: Tab; onTab: (t: Tab) => void; loans: number; holds: number; historyCount: number; balance: number }) {
  return (
    <div className="mb-space-lg flex w-full items-center justify-between gap-space-md overflow-x-auto pb-1">
      <nav aria-label="Library registers" className="flex shrink-0 items-center gap-space-xs">
        <button type="button" onClick={() => onTab("loans")} className={`${TAB_BASE} ${tab === "loans" ? TAB_ON : TAB_OFF}`}>
          <Icon name="book" className="text-lg" />
          <span>Current Loans ({loans})</span>
        </button>
        <button type="button" onClick={() => onTab("holds")} className={`${TAB_BASE} ${tab === "holds" ? TAB_ON : TAB_OFF}`}>
          <Icon name="bookmark" className="text-lg" />
          <span>Holds &amp; Requests ({holds})</span>
        </button>
        <button type="button" className={`${TAB_BASE} ${TAB_OFF}`}>
          <Icon name="devices" className="text-lg" />
          <span>Digital Checkouts (2)</span>
        </button>
        <Link href="/history" className={`${TAB_BASE} ${TAB_OFF}`}>
          <Icon name="history_edu" className="text-lg" />
          <span>Reading History ({historyCount})</span>
        </Link>
        <Link href="/fines" className={`${TAB_BASE} ${TAB_OFF}`}>
          <Icon name="receipt_long" className="text-lg" />
          <span>Fines &amp; Notices (${balance.toFixed(2)})</span>
        </Link>
      </nav>
    </div>
  );
}

/** Current loans ledger + holds. Client-side tabs, sorting, renewals and hold cancellation. */
export default function LoansView({ historyCount }: { historyCount: number }) {
  const { user, loans, holds, getBook, renewLoan, canRenew, renewedLoanIds, cancelHold, outstandingBalance } = useLibrary();
  const [tab, setTab] = useState<Tab>("loans");
  const [sort, setSort] = useState<SortKey>("due");
  const [toast, setToast] = useState<ToastState | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const showToast = useCallback((message: string, icon = "check_circle") => {
    if (timer.current) clearTimeout(timer.current);
    setToast({ id: Date.now(), message, icon });
    timer.current = setTimeout(() => setToast(null), 3200);
  }, []);
  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);

  // "/loans#holds" (sidebar link, Home card) opens the holds tab.
  useEffect(() => {
    const sync = () => setTab(window.location.hash === "#holds" ? "holds" : "loans");
    sync();
    window.addEventListener("hashchange", sync);
    return () => window.removeEventListener("hashchange", sync);
  }, []);

  const rows = useMemo(() => {
    const withBook = loans
      .map((loan) => ({ loan, book: getBook(loan.bookId) }))
      .filter((r): r is { loan: Loan; book: Book } => Boolean(r.book));
    const cmp: Record<SortKey, (a: { loan: Loan; book: Book }, b: { loan: Loan; book: Book }) => number> = {
      due: (a, b) => a.loan.dueDate.localeCompare(b.loan.dueDate),
      title: (a, b) => a.book.title.localeCompare(b.book.title),
      checkout: (a, b) => b.loan.checkedOutOn.localeCompare(a.loan.checkedOutOn),
      branch: (a, b) => a.loan.branchName.localeCompare(b.loan.branchName),
    };
    return withBook.sort(cmp[sort]);
  }, [loans, getBook, sort]);

  const eligible = loans.filter((l) => canRenew(l.id) && !renewedLoanIds.includes(l.id));
  const branchCount = new Set(loans.map((l) => l.branchName)).size;
  const readyHolds = holds.filter((h) => h.status === "ready").length;

  const renewAll = () => {
    if (!eligible.length) return;
    eligible.forEach((l) => renewLoan(l.id));
    showToast(`All ${eligible.length} eligible ${eligible.length === 1 ? "title has" : "titles have"} been extended`, "verified");
  };
  const onRenewed = (_loan: Loan, book: Book, newDue: string) =>
    showToast(`"${book.title}" successfully renewed through ${formatDate(newDue)}.`);
  const onCancel = (hold: Hold, book: Book) => {
    cancelHold(hold.id);
    showToast(`Hold on "${book.title}" cancelled.`, "cancel");
  };

  const showLoans = tab === "loans";

  return (
    <main className="relative mx-auto min-h-screen w-full max-w-[1600px] bg-surface px-gutter-lg py-margin">
      <div className="flex w-full flex-col">
        <div className="mb-space-lg w-full">
          <div className="flex flex-col justify-between gap-space-md pb-space-lg lg:flex-row lg:items-end">
            <div className="space-y-space-xs">
              <div className="mb-1 inline-flex items-center gap-2 rounded-full bg-secondary-fixed px-2.5 py-1 font-metadata-caps text-metadata-caps uppercase tracking-wider text-primary">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-primary" />
                Scholarly Borrowing Ledger
              </div>
              <h1 className="font-headline-lg text-headline-lg tracking-tight text-on-surface">My Library &amp; Academic Loans</h1>
              <p className="flex flex-wrap items-center gap-x-2 gap-y-1 font-body-md text-body-md text-on-surface-variant">
                <span className="font-semibold text-primary">{user.accountType}:</span>
                <span>{user.name}</span>
                <span className="text-outline">/</span>
                <span>Card #{user.cardNumber}</span>
                <span className="text-outline">/</span>
                <span className="italic">Department of {user.department}</span>
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-space-sm self-start lg:self-auto">
              <button
                type="button"
                onClick={renewAll}
                disabled={!eligible.length}
                className="inline-flex h-10 items-center gap-2 rounded-lg bg-primary-container px-space-md font-label-lg text-label-md text-on-primary shadow-sm transition-colors hover:bg-primary disabled:cursor-default disabled:opacity-80 disabled:hover:bg-primary-container"
              >
                <Icon name={eligible.length ? "autorenew" : "done_all"} className="text-lg" />
                <span>{eligible.length ? "Renew All Eligible" : "All Eligible Renewed"}</span>
              </button>
              <button type="button" className="inline-flex h-10 items-center gap-1.5 rounded-lg bg-surface-container-lowest px-space-md font-label-lg text-label-md text-on-surface shadow-sm transition-colors hover:bg-surface-container-high">
                <Icon name="print" className="text-lg text-on-surface-variant" />
                <span className="hidden sm:inline">Receipt</span>
              </button>
            </div>
          </div>
          <LoanSummaryCards />
        </div>

        <Tabs tab={tab} onTab={setTab} loans={loans.length} holds={holds.length} historyCount={historyCount} balance={outstandingBalance} />

        {showLoans && (
          <>
            <div className="mb-space-md flex w-full flex-col justify-between gap-space-md rounded-xl bg-surface-container-low p-space-sm sm:p-space-md md:flex-row md:items-center">
              <div className="flex flex-wrap items-center gap-space-sm font-body-sm text-body-sm text-on-surface-variant">
                <span className="font-metadata-caps text-metadata-caps uppercase text-outline">Sort Catalog:</span>
                <div className="relative inline-flex items-center rounded-lg bg-surface-container-lowest px-3 py-1.5">
                  <Icon name="calendar_today" className="mr-1.5 text-base text-primary" />
                  <select
                    value={sort}
                    onChange={(e) => setSort(e.target.value as SortKey)}
                    className="cursor-pointer bg-transparent pr-4 font-label-lg text-label-md text-on-surface focus:outline-none"
                  >
                    {SORTS.map((s) => (
                      <option key={s.key} value={s.key}>
                        {s.label}
                      </option>
                    ))}
                  </select>
                </div>
                <span className="hidden text-outline lg:inline">•</span>
                <span className="hidden lg:inline">
                  Showing {loans.length} monographs checked out across {branchCount} branches
                </span>
              </div>
              <div className="flex items-center gap-space-sm self-end md:self-auto">
                <button type="button" className="inline-flex items-center gap-1.5 rounded-lg bg-surface-container-lowest px-3 py-1.5 font-label-lg text-label-md text-on-surface transition-colors hover:bg-surface-container-high">
                  <Icon name="terminal" className="text-base text-primary" />
                  <span>Export BibTeX</span>
                </button>
                <button type="button" className="inline-flex items-center gap-1.5 rounded-lg bg-surface-container-lowest px-3 py-1.5 font-label-lg text-label-md text-on-surface transition-colors hover:bg-surface-container-high">
                  <Icon name="download" className="text-base text-on-surface-variant" />
                  <span>CSV Ledger</span>
                </button>
              </div>
            </div>

            <div className="mb-space-xl w-full space-y-space-md">
              {rows.map(({ loan, book }) => (
                <LoanRow key={loan.id} loan={loan} book={book} variant="full" onRenewed={onRenewed} />
              ))}
              {rows.length === 0 && <p className="rounded-xl bg-surface-container-lowest p-space-lg text-on-surface-variant shadow-sm">No items currently checked out.</p>}
            </div>
          </>
        )}

        <section id="holds" className="mb-space-xl w-full scroll-mt-20">
          <div className="mb-space-md flex items-center justify-between">
            <div>
              <h2 className="font-headline-md text-headline-md font-semibold tracking-tight text-on-surface">Holds &amp; Inter-Library Requests</h2>
              <p className="font-body-sm text-body-sm text-on-surface-variant">Reserved materials waiting at campus service points</p>
            </div>
            <span className="rounded-full bg-secondary-fixed px-2.5 py-1 font-metadata-caps text-metadata-caps font-bold text-primary">
              {readyHolds} Ready for Dispatch
            </span>
          </div>
          <div className="space-y-space-md">
            {holds.map((h) => {
              const book = getBook(h.bookId);
              return book ? <HoldCard key={h.id} hold={h} book={book} onCancel={onCancel} /> : null;
            })}
            {holds.length === 0 && <p className="rounded-xl bg-surface-container-lowest p-space-lg text-on-surface-variant shadow-sm">No active holds or requests.</p>}
          </div>
        </section>

        <footer className="mb-space-lg flex w-full flex-col items-start justify-between gap-space-md rounded-xl bg-surface-container-low p-space-md sm:p-space-lg md:flex-row md:items-center">
          <div className="flex max-w-3xl items-start gap-space-sm">
            <Icon name="policy" className="mt-0.5 shrink-0 text-2xl text-primary" />
            <div className="space-y-0.5">
              <h4 className="font-label-lg text-label-md font-semibold text-on-surface">Graduate Borrowing Terms &amp; Grace Policies</h4>
              <p className="font-body-sm text-body-sm leading-relaxed text-on-surface-variant">
                Graduate students receive standard 28-day terms with automated grace renewals unless recalled by another researcher. Inter-library loans comply with the Cambridge Research Federation agreement.
              </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-space-sm self-end md:self-auto">
            <button type="button" className="inline-flex h-10 items-center gap-2 rounded-lg bg-surface-container-lowest px-space-md font-label-lg text-label-md text-primary shadow-sm transition-colors hover:bg-secondary-fixed">
              <Icon name="sync_alt" className="text-lg" />
              <span>Sync with University Zotero</span>
            </button>
          </div>
        </footer>
      </div>
      <Toast toast={toast} />
    </main>
  );
}

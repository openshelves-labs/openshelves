"use client";

import Link from "next/link";
import type { Book, Loan } from "@/types";
import { useLibrary, RENEWAL_EXTENSION_DAYS } from "@/lib/store";
import { addDays, daysUntil, dueLabel, formatDate, formatShortDate } from "@/lib/dates";
import BookCover from "./BookCover";
import CiteButton from "./CiteButton";
import Icon from "./Icon";
import StatusChip, { type ChipTone } from "./StatusChip";

/** A loan counts as "due soon" within this many days. */
export const DUE_SOON_DAYS = 7;

export function dueTone(dueDate: string): ChipTone {
  const n = daysUntil(dueDate);
  if (n <= 3) return "critical";
  if (n <= DUE_SOON_DAYS) return "warning";
  return "success";
}

/** Authors only (no forewords/editors), first two. */
export function loanByline(book: Book): string {
  return book.contributors
    .filter((c) => c.role === "author" || c.role === "co-author")
    .slice(0, 2)
    .map((c) => c.name)
    .join(", ");
}

export interface LoanRowProps {
  loan: Loan;
  book: Book;
  variant: "compact" | "full";
  /** called after a successful renewal (e.g. to show a toast) */
  onRenewed?: (loan: Loan, book: Book, newDue: string) => void;
}

const TONE_BAR = { critical: "bg-error", warning: "bg-[#C27D23]", success: "bg-primary", neutral: "bg-primary", dark: "bg-primary" } as const;
const TONE_FULL = {
  critical: "bg-error-container text-on-error-container",
  warning: "bg-[#FDF4E7] text-[#C27D23]",
  success: "bg-secondary-fixed text-primary",
  neutral: "bg-secondary-fixed text-primary",
  dark: "bg-secondary-fixed text-primary",
} as const;

/** One loan, rendered as a compact Home row or a full Loans-ledger card. Driven by Loan + Book. */
export default function LoanRow({ loan, book, variant, onRenewed }: LoanRowProps) {
  const { canRenew, renewLoan, renewedLoanIds } = useLibrary();
  const tone = dueTone(loan.dueDate);
  const renewed = renewedLoanIds.includes(loan.id) || !canRenew(loan.id);
  const nextDue = addDays(loan.dueDate, RENEWAL_EXTENSION_DAYS);
  const remaining = loan.renewalsMax - loan.renewalsUsed;
  const doRenew = () => {
    if (renewed) return;
    renewLoan(loan.id);
    onRenewed?.(loan, book, nextDue);
  };
  const byline = loanByline(book);

  if (variant === "compact") {
    return (
      <article className="flex flex-col justify-between gap-4 rounded-xl border border-border-warm bg-white p-4 transition-all hover:border-primary/30 md:flex-row md:items-center">
        <div className="flex min-w-0 items-center gap-4">
          <Link href={`/catalog/${book.id}`} className="shrink-0">
            <BookCover book={book} className="h-20 w-14" />
          </Link>
          <div className="min-w-0">
            <div className="mb-1 flex items-center gap-2">
              <StatusChip tone={tone}>{dueLabel(loan.dueDate)}</StatusChip>
              <span className="font-metadata-caps text-[11px] text-outline">{loan.licenseNote ?? `Call No: ${book.callNumber}`}</span>
            </div>
            <h3 className="truncate font-title-editorial text-base font-semibold text-on-surface">
              <Link href={`/catalog/${book.id}`} className="hover:text-primary">
                {loan.compactTitle ?? book.title}
              </Link>
            </h3>
            <p className="font-body-sm text-xs text-on-surface-variant">
              {byline} • <span className="text-outline">{loan.location ?? loan.branchName}</span>
            </p>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2 self-end md:self-center">
          <button
            type="button"
            disabled={renewed}
            onClick={doRenew}
            className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-primary px-3 font-label-lg text-xs text-white transition-colors hover:bg-[#214f38] disabled:cursor-default disabled:bg-secondary-fixed disabled:text-primary disabled:hover:bg-secondary-fixed"
          >
            <Icon name={renewed ? "check" : "autorenew"} className="text-sm" />
            <span>{renewed ? "Renewed" : "Renew"}</span>
          </button>
          <CiteButton book={book} />
        </div>
      </article>
    );
  }

  const urgent = tone === "critical";
  const primaryBtn = "bg-primary text-on-primary hover:bg-primary-container";
  const outlineBtn = "bg-surface-container-lowest text-on-surface hover:bg-surface-container-high";
  const doneBtn = "bg-secondary-container text-on-secondary-container";
  return (
    <article className="relative overflow-hidden rounded-xl bg-surface-container-lowest p-space-md shadow-sm transition-shadow hover:shadow-md sm:p-space-lg">
      <div className={`absolute bottom-0 left-0 top-0 w-1.5 ${TONE_BAR[tone]}`} />
      <div className="flex flex-col gap-space-md sm:gap-space-lg md:flex-row">
        <div className="relative aspect-[2/3] w-28 shrink-0 sm:w-36">
          <Link href={`/catalog/${book.id}`}>
            <BookCover book={book} className="h-full w-full rounded-lg shadow-sm" />
          </Link>
          {urgent && (
            <div className="absolute left-2 top-2 rounded-full bg-error px-2 py-0.5 font-metadata-caps text-[10px] font-bold uppercase tracking-wider text-on-error">
              Due Soon
            </div>
          )}
        </div>
        <div className="flex min-w-0 flex-1 flex-col justify-between">
          <div>
            <div className="mb-1.5 flex flex-wrap items-center justify-between gap-2">
              <span
                className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 font-metadata-caps text-metadata-caps font-semibold uppercase tracking-wider ${TONE_FULL[tone]}`}
              >
                <Icon name={urgent ? "schedule" : "check_circle"} className="text-sm" />
                {dueLabel(loan.dueDate)} ({formatDate(loan.dueDate)})
              </span>
              <span className="font-metadata-caps text-metadata-caps text-outline">Barcode: {loan.barcode}</span>
            </div>
            <h2 className="font-title-editorial text-headline-sm font-semibold leading-snug tracking-tight text-on-surface">
              <Link href={`/catalog/${book.id}`} className="hover:text-primary">
                {loan.ledgerTitle ?? book.title}
              </Link>
            </h2>
            <p className="mt-0.5 font-body-md text-body-md text-on-surface-variant">
              by <span className="font-medium text-on-surface">{byline}</span>
            </p>
            <div className="mt-space-md grid grid-cols-1 gap-x-4 gap-y-2 rounded-lg bg-surface-container-low p-space-sm font-body-sm text-body-sm sm:grid-cols-3">
              <div>
                <span className="block font-metadata-caps text-metadata-caps uppercase text-outline">Call Number</span>
                <span className="font-semibold text-on-surface">{book.callNumber}</span>
              </div>
              <div>
                <span className="block font-metadata-caps text-metadata-caps uppercase text-outline">Branch / Pickup</span>
                <span className="text-on-surface">{loan.branchName}</span>
              </div>
              <div>
                <span className="block font-metadata-caps text-metadata-caps uppercase text-outline">Borrow Date &amp; Renewals</span>
                <span className="text-on-surface">
                  {formatDate(loan.checkedOutOn)} •{" "}
                  <span className="font-medium text-primary">
                    {remaining} of {loan.renewalsMax} remaining
                  </span>
                </span>
              </div>
            </div>
          </div>
          <div className="mt-space-lg flex flex-wrap items-center justify-between gap-space-sm pt-space-sm">
            <div className="flex flex-wrap items-center gap-space-sm">
              <button
                type="button"
                disabled={renewed}
                onClick={doRenew}
                className={`inline-flex h-9 items-center gap-1.5 rounded-lg px-space-md font-label-lg text-label-md transition-colors disabled:cursor-default ${
                  renewed ? doneBtn : urgent ? primaryBtn : outlineBtn
                }`}
              >
                <Icon name={renewed ? "done" : urgent ? "autorenew" : "cached"} className={`text-base ${!renewed && !urgent ? "text-primary" : ""}`} />
                <span>{renewed ? `Renewed (Due ${formatDate(loan.dueDate)})` : urgent ? `Renew Loan (Extend to ${formatShortDate(nextDue)})` : "Renew Early"}</span>
              </button>
              <CiteButton
                book={book}
                label="Cite Book"
                className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-surface-container-lowest px-space-md font-label-lg text-label-md text-on-surface transition-colors hover:bg-surface-container-high"
                iconClassName="text-base text-primary"
              />
              {loan.notesCount !== undefined && (
                <button
                  type="button"
                  className="inline-flex h-9 items-center gap-1 rounded-lg bg-surface-container-lowest px-3 font-label-lg text-label-md text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-on-surface"
                >
                  <Icon name="notes" className="text-base" />
                  <span>Reading Notes ({loan.notesCount})</span>
                </button>
              )}
              {loan.studyGuideUrl && (
                <a
                  href={loan.studyGuideUrl}
                  onClick={(e) => e.preventDefault()}
                  className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-surface-container-low px-space-md font-label-lg text-label-md text-primary transition-colors hover:bg-secondary-fixed"
                >
                  <Icon name="download" className="text-base" />
                  <span>Study Guide (PDF)</span>
                </a>
              )}
            </div>
            {!loan.policyNote ? (
              <button
                type="button"
                className="inline-flex items-center gap-1 font-metadata-caps text-metadata-caps uppercase tracking-wider text-outline transition-colors hover:text-error"
              >
                <Icon name="report_problem" className="text-base" />
                <span>Report Issue</span>
              </button>
            ) : (
              loan.policyNote && (
                <span className="font-metadata-caps text-metadata-caps uppercase tracking-wider text-outline">{loan.policyNote}</span>
              )
            )}
          </div>
        </div>
      </div>
    </article>
  );
}

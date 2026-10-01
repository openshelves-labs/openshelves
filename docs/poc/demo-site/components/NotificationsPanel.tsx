"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { AppNotification, NotificationAction } from "@/types";
import { RENEWAL_EXTENSION_DAYS, useLibrary } from "@/lib/store";
import { daysUntil, formatWeekdayDate } from "@/lib/dates";
import Icon from "./Icon";

type Filter = "all" | "unread" | "circulation" | "acquisitions";

const ICON_TONE: Record<AppNotification["iconTone"], string> = {
  green: "bg-secondary-fixed text-primary",
  amber: "bg-surface-container-high text-amber-700",
  sage: "bg-tertiary-fixed text-tertiary",
  red: "bg-error-container/60 text-error",
  neutral: "bg-surface-container-highest text-on-surface-variant",
};

const BTN: Record<NotificationAction["variant"], string> = {
  primary: "bg-primary text-on-primary hover:bg-primary-dark",
  soft: "bg-secondary-fixed text-primary hover:bg-secondary-fixed-dim",
  secondary: "bg-surface-container-high text-on-surface hover:bg-surface-variant",
  ghost: "text-on-surface-variant hover:text-on-surface",
};

export default function NotificationsPanel() {
  const {
    notifications,
    unreadCount,
    notificationsOpen,
    closeNotifications,
    markNotificationRead,
    markAllNotificationsRead,
    loans,
    renewLoan,
    canRenew,
    renewedLoanIds,
    getBook,
    isSaved,
    toggleSave,
  } = useLibrary();
  const pathname = usePathname();
  const [filter, setFilter] = useState<Filter>("all");

  useEffect(() => closeNotifications(), [pathname, closeNotifications]);
  useEffect(() => {
    if (!notificationsOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && closeNotifications();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [notificationsOpen, closeNotifications]);

  const visible = useMemo(
    () =>
      notifications.filter((n) =>
        filter === "all" ? true : filter === "unread" ? !n.read : n.category === filter,
      ),
    [notifications, filter],
  );

  if (!notificationsOpen) return null;

  const chip = (id: Filter, label: string, extra?: React.ReactNode) => (
    <button
      key={id}
      type="button"
      onClick={() => setFilter(id)}
      className={`flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1 font-label-lg text-xs transition-colors ${
        filter === id ? "bg-primary text-on-primary" : "bg-white text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
      }`}
    >
      {label}
      {extra}
    </button>
  );

  const groups: { key: "today" | "earlier"; label: string; right: string }[] = [
    { key: "today", label: "Today", right: "" },
    { key: "earlier", label: "Earlier This Week", right: "Resolved" },
  ];

  const renderItem = (n: AppNotification) => {
    const loan = n.loanId ? loans.find((l) => l.id === n.loanId) : undefined;
    const book = n.bookId ? getBook(n.bookId) : undefined;
    let title = n.title;
    let bodyAfter = n.bodyAfter;
    let meta = n.meta;
    if (loan) {
      const d = daysUntil(loan.dueDate);
      title = d < 0 ? `Loan overdue by ${-d} day${d === -1 ? "" : "s"}` : d === 0 ? "Loan due today" : `Loan due in ${d} day${d === 1 ? "" : "s"}`;
      const author = book?.contributors[0]?.name;
      bodyAfter = `${author ? ` (${author})` : ""} is due on ${formatWeekdayDate(loan.dueDate)}.`;
      const left = loan.renewalsMax - loan.renewalsUsed;
      meta = [
        { text: `Call No: ${book?.callNumber ?? ""}` },
        { text: "•" },
        { text: `${left} renewal${left === 1 ? "" : "s"} remaining`, highlight: true },
      ];
    }

    const run = (a: NotificationAction) => {
      markNotificationRead(n.id);
      if (a.type === "renew" && loan) renewLoan(loan.id);
      if (a.type === "save" && n.bookId) toggleSave(n.bookId);
    };

    return (
      <div
        key={n.id}
        className={`group relative flex items-start gap-space-md p-space-lg transition-colors hover:bg-surface-container-low ${
          n.read ? "bg-surface-container-low/60 opacity-90" : "bg-white"
        }`}
      >
        {!n.read && <span className="absolute left-2 top-7 h-2 w-2 rounded-full bg-primary" title="Unread notice" />}
        <div className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${ICON_TONE[n.iconTone]}`}>
          <Icon name={n.icon} filled={!n.read} className="text-xl" />
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-space-xs">
          <div className="flex items-center justify-between gap-space-xs">
            <div className="flex min-w-0 items-center gap-space-xs">
              <h4 className={`truncate font-label-lg text-sm text-on-surface ${n.read ? "font-medium" : "font-semibold"}`}>{title}</h4>
              {n.badge && <span className="rounded bg-amber-100 px-1.5 py-0.5 font-metadata-caps text-[11px] font-semibold text-amber-900">{n.badge}</span>}
            </div>
            <span className="shrink-0 font-metadata-caps text-[11px] text-outline">{n.ageLabel}</span>
          </div>
          <p className="font-body-md text-[13px] leading-5 text-on-surface-variant">
            {n.bodyBefore}
            {n.itemTitle && (
              <Link
                href={n.bookId ? `/catalog/${n.bookId}` : "#"}
                onClick={() => markNotificationRead(n.id)}
                className="font-title-editorial text-[15px] italic text-on-surface hover:text-primary"
              >
                {n.itemTitle}
              </Link>
            )}
            {bodyAfter}
          </p>
          {n.card && (
            <div className="mt-space-xs flex flex-col gap-0.5 rounded-lg bg-surface-container-low p-space-sm">
              <div className="flex items-center gap-space-xs text-[13px] font-medium text-on-surface">
                <Icon name={n.card.icon} className="text-base text-primary" />
                <span>{n.card.title}</span>
              </div>
              <span className="font-metadata-caps text-[11px] text-on-surface-variant">{n.card.subtitle}</span>
            </div>
          )}
          {meta && (
            <div className="flex flex-wrap items-center gap-space-sm pt-0.5 font-metadata-caps text-[11px] text-on-surface-variant">
              {meta.map((m, i) => (
                <span key={i} className={m.highlight ? "font-semibold text-primary" : ""}>
                  {m.text}
                </span>
              ))}
            </div>
          )}
          {n.actions.length > 0 && (
            <div className="flex flex-wrap items-center gap-space-sm pt-space-xs">
              {n.actions.map((a) => {
                const cls = `flex h-8 items-center gap-1.5 rounded-lg px-space-md font-label-lg text-xs transition-colors ${BTN[a.variant]}`;
                if (a.type === "renew") {
                  const done = !!loan && (renewedLoanIds.includes(loan.id) || !canRenew(loan.id));
                  return (
                    <button key={a.id} type="button" disabled={done} onClick={() => run(a)} className={`${cls} ${done ? "opacity-60" : ""}`}>
                      <Icon name={done ? "check" : a.icon ?? "autorenew"} className="text-sm" />
                      <span>{done ? "Renewed" : `${a.label} (${RENEWAL_EXTENSION_DAYS} Days)`}</span>
                    </button>
                  );
                }
                if (a.type === "save") {
                  const saved = !!n.bookId && isSaved(n.bookId);
                  return (
                    <button key={a.id} type="button" onClick={() => run(a)} className={cls}>
                      <Icon name={a.icon ?? "bookmark_add"} filled={saved} className="text-sm" />
                      <span>{saved ? "Saved" : a.label}</span>
                    </button>
                  );
                }
                if (a.type === "link" && a.href) {
                  return (
                    <Link key={a.id} href={a.href} onClick={() => markNotificationRead(n.id)} className={cls}>
                      {a.icon && <Icon name={a.icon} className="text-sm" />}
                      <span>{a.label}</span>
                    </Link>
                  );
                }
                return (
                  <button key={a.id} type="button" onClick={() => markNotificationRead(n.id)} className={cls}>
                    {a.icon && <Icon name={a.icon} className="text-sm" />}
                    <span>{a.label}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <>
      <div className="fixed inset-0 left-64 top-16 z-40 bg-on-surface/10 backdrop-blur-[1px]" onClick={closeNotifications} />
      <div
        role="dialog"
        aria-label="Notifications"
        className="fixed bottom-6 right-6 top-[72px] z-50 flex w-full max-w-[460px] flex-col overflow-hidden rounded-xl border border-border-warm bg-white shadow-2xl"
      >
        <div className="flex flex-col gap-space-md bg-surface-container-low p-space-lg">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-space-sm">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-container text-on-primary">
                <Icon name="notifications" filled className="text-lg" />
              </div>
              <div className="flex items-baseline gap-space-xs">
                <h2 className="font-headline-sm text-xl font-semibold text-on-surface">Notifications</h2>
                <span
                  className={`rounded-full px-2 py-0.5 font-metadata-caps text-[11px] font-bold ${
                    unreadCount ? "bg-secondary-fixed text-primary" : "bg-surface-container-high text-on-surface-variant"
                  }`}
                >
                  {unreadCount} new
                </span>
              </div>
            </div>
            <div className="flex items-center gap-space-xs">
              <button type="button" onClick={markAllNotificationsRead} className="px-space-xs py-1 font-label-lg text-xs text-primary hover:underline">
                Mark all read
              </button>
              <button type="button" aria-label="Close notification panel" onClick={closeNotifications} className="rounded-lg p-1.5 text-on-surface-variant hover:bg-surface-container hover:text-on-surface">
                <Icon name="close" className="text-lg" />
              </button>
            </div>
          </div>
          <div className="flex items-center gap-space-xs overflow-x-auto pt-space-xs">
            {chip("all", `All (${notifications.length})`)}
            {chip("unread", "Unread", unreadCount > 0 ? <span className="h-1.5 w-1.5 rounded-full bg-primary" /> : null)}
            {chip("circulation", "Circulation")}
            {chip("acquisitions", "Acquisitions")}
          </div>
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-y-auto bg-white">
          {visible.length === 0 && (
            <div className="my-auto flex flex-col items-center px-space-xl py-space-xl text-center">
              <svg viewBox="0 0 160 140" className="h-36 w-40" role="img" aria-label="Illustration of a tidy bookshelf with a check mark">
                <ellipse cx="80" cy="126" rx="58" ry="7" fill="#E5E0D8" />
                <circle cx="80" cy="66" r="52" fill="#c6ebd3" opacity="0.55" />
                <rect x="32" y="104" width="96" height="6" rx="3" fill="#2F5D45" />
                <rect x="40" y="62" width="14" height="42" rx="2" fill="#2F5D45" />
                <rect x="56" y="72" width="12" height="32" rx="2" fill="#8FBFA2" />
                <rect x="70" y="56" width="16" height="48" rx="2" fill="#E5E0D8" stroke="#2F5D45" strokeWidth="2" />
                <rect x="88" y="68" width="12" height="36" rx="2" fill="#2F5D45" opacity="0.75" />
                <rect x="102" y="60" width="14" height="44" rx="2" fill="#8FBFA2" />
                <line x1="75" y1="66" x2="81" y2="66" stroke="#2F5D45" strokeWidth="2" strokeLinecap="round" />
                <line x1="75" y1="72" x2="81" y2="72" stroke="#2F5D45" strokeWidth="2" strokeLinecap="round" />
                <circle cx="112" cy="36" r="15" fill="#2F5D45" />
                <path d="M105 36.5l5 5 9-10" fill="none" stroke="#fff" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" />
                <circle cx="44" cy="34" r="3" fill="#8FBFA2" />
                <circle cx="132" cy="70" r="2.5" fill="#2F5D45" opacity="0.5" />
                <circle cx="30" cy="80" r="2" fill="#2F5D45" opacity="0.4" />
              </svg>
              <p className="mt-space-sm font-title-editorial text-lg font-semibold text-on-surface">You&apos;re all caught up.</p>
              <p className="mt-1 max-w-[16rem] font-body-sm text-sm text-on-surface-variant">Nothing new here. We&apos;ll let you know when a loan, hold or new arrival needs your attention.</p>
            </div>
          )}
          {groups.map((g) => {
            const items = visible.filter((n) => n.group === g.key);
            if (!items.length) return null;
            return (
              <div key={g.key}>
                <div className="sticky top-0 z-10 flex items-center justify-between bg-white px-space-lg pb-space-xs pt-space-md">
                  <span className="font-metadata-caps text-[11px] uppercase tracking-wider text-outline">{g.label}</span>
                  <span className="font-metadata-caps text-[11px] text-on-surface-variant">{g.right || `${items.length} notice${items.length === 1 ? "" : "s"}`}</span>
                </div>
                {items.map(renderItem)}
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between gap-space-md bg-surface-container-low p-space-md">
          <span className="inline-flex items-center gap-space-xs font-label-lg text-xs text-on-surface-variant">
            <Icon name="tune" className="text-base" />
            Notification Settings
          </span>
          <Link href="/loans" className="inline-flex items-center gap-space-xs font-label-lg text-xs font-semibold text-primary hover:underline">
            Full Archive
            <Icon name="arrow_forward" className="text-base" />
          </Link>
        </div>
      </div>
    </>
  );
}

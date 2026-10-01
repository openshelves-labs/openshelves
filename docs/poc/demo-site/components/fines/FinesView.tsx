"use client";

import { useMemo, useState } from "react";
import type { Fine, Payment, User } from "@/types";
import { NOW } from "@/lib/dates";
import Icon from "@/components/Icon";
import Link from "next/link";
import SummaryCards from "./SummaryCards";
import AssessmentsTable, { type FineFilter } from "./AssessmentsTable";
import CashierPanel, { REMITTANCE_SOURCES } from "./CashierPanel";
import PaymentHistory from "./PaymentHistory";

const isDigital = (f: Fine) => f.reasonKind === "processing_fee" || f.reasonKind === "digital_fee";

export default function FinesView({
  initialFines,
  initialPayments,
  borrowLimit,
  user,
}: {
  initialFines: Fine[];
  initialPayments: Payment[];
  borrowLimit: number;
  user: User;
}) {
  const [fines, setFines] = useState<Fine[]>(initialFines);
  const [payments, setPayments] = useState<Payment[]>(initialPayments);
  const [selected, setSelected] = useState<Set<string>>(
    () => new Set(initialFines.filter((f) => f.status === "unpaid").map((f) => f.id)),
  );
  const [filter, setFilter] = useState<FineFilter>("all");
  const [sourceId, setSourceId] = useState<string>(REMITTANCE_SOURCES[0].id);
  const [zeroPreview, setZeroPreview] = useState(false);

  const unpaid = useMemo(() => fines.filter((f) => f.status === "unpaid"), [fines]);
  const outstanding = unpaid.reduce((s, f) => s + f.amount, 0);
  const settledTotal = payments.reduce((s, p) => s + p.amount, 0);
  const selectedFines = unpaid.filter((f) => selected.has(f.id));
  const selectedTotal = selectedFines.reduce((s, f) => s + f.amount, 0);

  const counts = {
    all: unpaid.length,
    recall: fines.filter((f) => f.reasonKind === "overdue_recall").length,
    digital: fines.filter(isDigital).length,
    waived: fines.filter((f) => f.status === "waived").length,
  };
  const visible = fines.filter((f) => {
    if (filter === "recall") return f.reasonKind === "overdue_recall";
    if (filter === "digital") return isDigital(f);
    if (filter === "waived") return f.status === "waived";
    return true;
  });

  const toggleOne = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  const toggleAll = () =>
    setSelected(selected.size === unpaid.length ? new Set() : new Set(unpaid.map((f) => f.id)));

  const pay = () => {
    if (selectedFines.length === 0) return;
    const source = REMITTANCE_SOURCES.find((s) => s.id === sourceId) ?? REMITTANCE_SOURCES[0];
    const ids = new Set(selectedFines.map((f) => f.id));
    const today = NOW.toISOString().slice(0, 10);
    const seq = payments.length + 1;
    const txn = 98300 + seq * 17;
    const rc = 5100 + seq;
    const title =
      selectedFines.length === 1 ? selectedFines[0].title : `${selectedFines.length} circulation assessments settled`;
    setPayments((prev) => [
      {
        id: `pay-${txn}`,
        userId: user.id,
        transactionRef: `TXN-${txn}`,
        receiptRef: `RC-${rc}`,
        title,
        paidOn: today,
        method: source.historyLabel,
        desk: "Cambridge Central Library",
        amount: selectedTotal,
        taxExempt: true,
        icon: "receipt",
      },
      ...prev,
    ]);
    setFines((prev) => prev.map((f) => (ids.has(f.id) ? { ...f, status: "paid" } : f)));
    setSelected(new Set());
  };

  const displayBalance = zeroPreview ? 0 : outstanding;
  const recallSel = selectedFines.filter((f) => f.reasonKind === "overdue_recall").reduce((s, f) => s + f.amount, 0);
  const illSel = selectedFines.filter(isDigital).reduce((s, f) => s + f.amount, 0);
  const recallCount = unpaid.filter((f) => f.reasonKind === "overdue_recall").length;
  const illCount = unpaid.filter(isDigital).length;

  return (
    <div className="flex flex-col w-full">
      {/* BREADCRUMBS & CONTEXT BAR */}
      <div className="flex items-center justify-between pb-4">
        <nav className="flex items-center gap-2 font-metadata-caps text-outline">
          <Link href="/" className="hover:text-primary transition-colors flex items-center gap-1">
            <Icon name="home" className="text-sm" />
            <span>Home</span>
          </Link>
          <span className="text-outline-variant">/</span>
          <Link href="/loans" className="hover:text-primary transition-colors">
            My Library
          </Link>
          <span className="text-outline-variant">/</span>
          <span className="text-on-surface font-semibold">Fines &amp; Financial Accounts</span>
        </nav>
        <div className="hidden sm:flex items-center gap-2 text-xs font-label-md text-on-surface-variant bg-surface-container-high px-3 py-1 rounded-full">
          <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
          <span>
            Ledger Ref: <span className="font-mono text-on-surface font-medium">CAM-FIN-2024-Q4</span>
          </span>
        </div>
      </div>

      {/* EDITORIAL HEADER */}
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-8 border-b border-surface-variant">
        <div className="space-y-2 max-w-3xl">
          <div className="inline-flex items-center gap-2 text-primary font-metadata-caps tracking-widest uppercase">
            <Icon name="account_balance" className="text-sm" />
            <span>Financial Services &amp; Borrowing Integrity • Circulation Desk</span>
          </div>
          <h1 className="font-headline-lg text-on-surface tracking-tight">Fines, Assessments &amp; Payment Ledger</h1>
          <p className="font-body-md text-on-surface-variant leading-relaxed">
            Manage circulation assessments, inter-library loan processing charges, lost item indemnities, and view
            certified fiscal receipts endorsed by the Cambridge Central University Repository.
          </p>
        </div>
        <div className="flex flex-wrap sm:flex-nowrap items-center gap-3 shrink-0">
          <button
            type="button"
            className="h-10 px-4 inline-flex items-center gap-2 bg-surface-container-lowest text-on-surface font-label-lg rounded-lg shadow-sm hover:bg-surface-container transition-all"
          >
            <Icon name="description" className="text-base text-primary" />
            <span>Statement (PDF)</span>
          </button>
          <button
            type="button"
            className="h-10 px-4 inline-flex items-center gap-2 bg-surface-container-lowest text-primary font-label-lg rounded-lg shadow-sm hover:bg-surface-container transition-all"
          >
            <Icon name="policy" className="text-base" />
            <span>Fee Guidelines &amp; Appeals</span>
          </button>
        </div>
      </div>

      <SummaryCards
        balance={displayBalance}
        settledTotal={settledTotal}
        settledCount={payments.length}
        recallCount={recallCount}
        illCount={illCount}
        borrowLimit={borrowLimit}
      />

      {/* ZERO-BALANCE TOGGLE DEMO BAR */}
      <div className="bg-surface-container p-3 rounded-lg flex items-center justify-between gap-4 mb-6">
        <div className="flex items-center gap-2 text-xs font-body-sm text-on-surface-variant">
          <Icon name="info" className="text-primary text-base" />
          <span>Simulate ledger transition: Test active ledger view vs. settled zero-balance clearance state.</span>
        </div>
        <button
          type="button"
          onClick={() => setZeroPreview((v) => !v)}
          className="px-3 py-1 bg-surface-container-lowest hover:bg-surface-container-high rounded text-xs font-label-md text-primary font-medium transition-colors"
        >
          {zeroPreview ? "Restore Active Assessments" : "Preview Zero-Balance State"}
        </button>
      </div>

      {zeroPreview ? (
        <div className="bg-white border border-[#E5E0D8] p-8 rounded-xl text-center mb-8 space-y-4">
          <div className="w-16 h-16 rounded-full bg-secondary-container/40 text-primary mx-auto flex items-center justify-center">
            <Icon name="auto_stories" className="text-3xl" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="font-headline-md text-on-surface">All Accounts Settled</h3>
            <p className="font-body-md text-on-surface-variant">
              There are no outstanding circulation fines, late recall assessments, or lost item indemnities linked to{" "}
              {user.name}’s {user.accountType.toLowerCase()} account. Privileges are fully unrestricted.
            </p>
          </div>
          <div className="pt-2 inline-flex items-center gap-4 text-xs font-metadata-caps text-outline">
            <span>Access: Cambridge Central Repository</span>
            <span>•</span>
            <span>Tier: Cambridge Research Fellow</span>
            <span>•</span>
            <span>Sanction Index: 0.00</span>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start mb-12">
          <AssessmentsTable
            rows={visible}
            selected={selected}
            filter={filter}
            counts={counts}
            selectableCount={unpaid.length}
            onFilter={setFilter}
            onToggle={toggleOne}
            onToggleAll={toggleAll}
          />
          <CashierPanel
            user={user}
            selectedTotal={selectedTotal}
            selectedCount={selectedFines.length}
            recall={recallSel}
            ill={illSel}
            sourceId={sourceId}
            onSource={setSourceId}
            onPay={pay}
          />
        </div>
      )}

      <PaymentHistory payments={payments} />
    </div>
  );
}

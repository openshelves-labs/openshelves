"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import type { Book } from "@/types";
import { useLibrary } from "@/lib/store";
import BookCard from "@/components/BookCard";
import HoldButton from "@/components/HoldButton";
import SaveButton from "@/components/SaveButton";
import LoanRow from "@/components/LoanRow";
import SectionHeader from "@/components/SectionHeader";
import QuickActions from "./QuickActions";
import HomeHoldCard from "./HomeHoldCard";
import ResearchListCard from "./ResearchListCard";

const TILE_BTN =
  "mt-2.5 w-full rounded-lg border border-border-warm bg-surface-paper px-2 py-1.5 font-label-lg text-[11px] text-on-surface transition-colors hover:bg-secondary-fixed/50";

function greetingFor(h: number): string {
  return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
}

/** Home / scholar workspace. All counts read from the client library store. */
export default function HomeView({ highlights }: { highlights: Book[] }) {
  const { user, loans, holds, lists, getBook } = useLibrary();
  const [greeting, setGreeting] = useState("Good morning");
  useEffect(() => setGreeting(greetingFor(new Date().getHours())), []);
  const readyCount = holds.filter((h) => h.status === "ready").length;
  const firstQueued = holds[0];

  return (
    <main className="mx-auto w-full max-w-[1600px] space-y-9 px-8 py-8">
      <section className="space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-secondary">
            <span className="font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-secondary">Central University Library</span>
            <span className="inline-block h-1 w-1 rounded-full bg-secondary" />
            <span className="font-metadata-caps text-[11px] tracking-wider text-outline">Academic Portal</span>
          </div>
          <h1 className="font-title-editorial text-3xl font-semibold tracking-tight text-on-surface">
            {greeting}, {user.firstName}
          </h1>
          <p className="max-w-2xl font-body-md text-sm leading-relaxed text-on-surface-variant">
            Welcome to your scholarly workbench. You have{" "}
            <span className="font-semibold text-primary">
              {loans.length} {loans.length === 1 ? "loan" : "loans"} active
            </span>{" "}
            and{" "}
            <span className="font-semibold text-primary">
              {holds.length} requested {holds.length === 1 ? "hold" : "holds"}
            </span>{" "}
            {readyCount > 0 ? "awaiting pickup at the circulation desk." : "in the queue."}
          </p>
        </div>
        <QuickActions />
      </section>

      <section className="space-y-4" id="current-loans">
        <SectionHeader
          title="Current Loans"
          meta={
            <span className="rounded-full border border-border-warm bg-white px-2 py-0.5 font-metadata-caps text-[11px] text-on-surface-variant">
              {loans.length} Items Active
            </span>
          }
          linkLabel="Complete Circulation Ledger"
          href="/loans"
        />
        <div className="space-y-3">
          {loans.map((loan) => {
            const book = getBook(loan.bookId);
            return book ? <LoanRow key={loan.id} loan={loan} book={book} variant="compact" /> : null;
          })}
        </div>
      </section>

      <section className="space-y-4">
        <SectionHeader
          title="Continue Your Research"
          meta={<span className="font-metadata-caps text-[11px] text-outline">Curated Reading Lists &amp; Folios</span>}
          linkLabel={`All Lists (${lists.length})`}
          href="/lists"
        />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {lists.slice(0, 3).map((l) => (
            <ResearchListCard key={l.id} list={l} />
          ))}
        </div>
      </section>

      <section className="space-y-4">
        <SectionHeader
          title="New Arrivals"
          meta={<span className="font-metadata-caps text-[11px] text-outline">Recently accessioned monographs</span>}
          linkLabel="View Catalog Additions"
          href="/new-arrivals"
        />
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
          {highlights.map((book, i) => (
            <BookCard
              key={book.id}
              book={book}
              actions={
                i % 2 === 0 ? (
                  <HoldButton bookId={book.id} className={TILE_BTN} iconClassName="text-sm" />
                ) : (
                  <SaveButton bookId={book.id} className={TILE_BTN} iconClassName="text-sm" />
                )
              }
            />
          ))}
        </div>
      </section>

      <section className="space-y-4" id="your-holds">
        <SectionHeader
          title="Your Holds"
          meta={
            <span className="rounded-full bg-[#c6ebd3] px-2 py-0.5 font-metadata-caps text-[11px] font-semibold text-[#2F5D45]">
              {readyCount} Ready for Pickup
            </span>
          }
          right={
            firstQueued && (
              <Link href="/loans#holds" className="font-metadata-caps text-[11px] text-outline hover:text-primary">
                Queue Status: Position #{firstQueued.queuePosition}
              </Link>
            )
          }
        />
        {holds.length === 0 && (
          <p className="rounded-xl border border-border-warm bg-white p-5 font-body-sm text-xs text-on-surface-variant">No active holds.</p>
        )}
        {holds.map((h) => {
          const book = getBook(h.bookId);
          return book ? <HomeHoldCard key={h.id} hold={h} book={book} /> : null;
        })}
      </section>
    </main>
  );
}

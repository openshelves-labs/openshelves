"use client";

import { useEffect, useRef, useState } from "react";
import { useLibrary } from "@/lib/store";
import { useChat } from "@/lib/chat";
import { daysUntil } from "@/lib/dates";
import Icon from "./Icon";

interface Msg {
  id: number;
  from: "user" | "librarian";
  /** Librarian who wrote the reply (undefined for the automatic greeting and for the user's own messages) */
  by?: string;
  text: string;
  time: string;
}

/** Any available librarian can pick up a chat; the replying librarian's name is shown on each reply. */
const LIBRARIANS = ["Priya Nair", "Daniel Okafor", "Meera Iyer", "Tom Fischer"];

const SUGGESTIONS = ["Renew my loans", "Help finding sources", "Library opening hours", "Request an inter-library loan"];

const clock = () => new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });

export default function LibrarianChat() {
  const { open, close } = useChat();
  const { user, loans, holds, outstandingBalance } = useLibrary();
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [draft, setDraft] = useState("");
  const [typing, setTyping] = useState(false);
  const nextId = useRef(1);
  const replies = useRef(0);
  const scroller = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Seed the greeting once, on the client (keeps server/client markup identical).
  useEffect(() => {
    setMsgs([
      {
        id: 0,
        from: "librarian",
        text: `Hi ${user.firstName}! Ask us anything about finding sources, renewals, fines or citations. The next available librarian will reply here.`,
        time: clock(),
      },
    ]);
  }, [user.firstName]);

  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);

  useEffect(() => {
    if (!open) return;
    inputRef.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && close();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, close]);

  useEffect(() => {
    scroller.current?.scrollTo({ top: scroller.current.scrollHeight });
  }, [msgs, typing, open]);

  const reply = (q: string): string => {
    const t = q.toLowerCase();
    if (/renew|due|extend/.test(t)) {
      const soon = loans.filter((l) => daysUntil(l.dueDate) <= 7).length;
      return `You have ${loans.length} active loan${loans.length === 1 ? "" : "s"}${soon ? `, and ${soon} ${soon === 1 ? "is" : "are"} due within a week` : ""}. You can renew eligible items from Current Loans, and I can extend any that show as ineligible if there's no waiting hold.`;
    }
    if (/fine|fee|pay|overdue/.test(t)) {
      return outstandingBalance > 0
        ? `Your current balance is $${outstandingBalance.toFixed(2)}. You can pay it under Fines & Fees, and if a charge looks wrong, tell me the item and I'll review it with circulation.`
        : "You have no outstanding fines right now.";
    }
    if (/hold|reserve|pickup|pick up/.test(t)) {
      return holds.length
        ? `You have ${holds.length} hold${holds.length === 1 ? "" : "s"} in the queue; ready items wait at the Main Circulation desk for 5 days.`
        : "You have no active holds. Open any book in the catalog and choose Place Hold to reserve it.";
    }
    if (/hour|open|close|when/.test(t)) {
      return "The Main Library is open Mon–Fri 8:00–22:00, Sat–Sun 10:00–18:00. The circulation desk closes an hour earlier.";
    }
    if (/inter.?library|ill\b|loan request|not in the catalog/.test(t)) {
      return "For items we don't hold, use Inter-Library Loan: send me the title, author and ISBN or DOI and I'll submit the request. Articles usually arrive in 2–3 days and books in about a week.";
    }
    if (/cite|citation|apa|mla|bibtex|reference/.test(t)) {
      return "Every book page has a Cite button that generates APA, MLA, Chicago and BibTeX. For articles, send me the DOI and I'll check the formatting.";
    }
    if (/source|paper|research|thesis|literature|find|journal|database/.test(t)) {
      return "For computer science, start with the ACM Digital Library and IEEE Xplore under Digital Resources. Tell me your topic and I'll suggest search terms and a few databases to try.";
    }
    return "Thanks for your question. I've noted it and will look into this for you. Could you share a little more detail, such as the title or topic involved?";
  };

  const send = (text: string) => {
    const q = text.trim();
    if (!q || typing) return;
    setMsgs((m) => [...m, { id: nextId.current++, from: "user", text: q, time: clock() }]);
    setDraft("");
    setTyping(true);
    timer.current = setTimeout(() => {
      const by = LIBRARIANS[replies.current++ % LIBRARIANS.length];
      setMsgs((m) => [...m, { id: nextId.current++, from: "librarian", by, text: reply(q), time: clock() }]);
      setTyping(false);
    }, 1300);
  };

  if (!open) return null;

  const showSuggestions = msgs.length <= 1 && !typing;

  return (
    <div
      role="dialog"
      aria-label="Ask a Librarian"
      className="fixed bottom-6 right-6 z-50 flex h-[36rem] max-h-[calc(100%-6rem)] w-[24rem] flex-col overflow-hidden rounded-xl border border-border-warm bg-white shadow-xl"
    >
      <div className="flex shrink-0 items-center gap-3 border-b border-border-warm bg-[#F3F1EC]/70 px-4 py-3">
        <div className="relative">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#2F5D45] text-white">
            <Icon name="support_agent" className="text-[22px]" />
          </div>
          <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white bg-emerald-500" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate font-title-editorial text-base font-semibold text-on-surface">Ask a Librarian</div>
          <div className="truncate font-body-sm text-xs text-on-surface-variant">Library help desk · a librarian will reply</div>
        </div>
        <button type="button" onClick={close} aria-label="Close chat" className="rounded-lg p-1.5 text-on-surface-variant transition-colors hover:bg-white hover:text-on-surface">
          <Icon name="close" className="text-xl" />
        </button>
      </div>

      <div ref={scroller} className="flex min-h-0 flex-1 flex-col overflow-y-auto px-4 py-4">
        <div className="mt-auto space-y-3">
        {msgs.map((m) => (
          <div key={m.id} className={`flex flex-col ${m.from === "user" ? "items-end" : "items-start"}`}>
            <div
              className={`max-w-[85%] whitespace-pre-wrap break-words rounded-2xl px-3.5 py-2.5 font-body-md text-sm leading-relaxed ${
                m.from === "user" ? "rounded-br-md bg-[#2F5D45] text-white" : "rounded-bl-md border border-border-warm bg-[#F3F1EC]/70 text-on-surface"
              }`}
            >
              {m.text}
            </div>
            <span className="mt-1 px-1 font-metadata-caps text-[10px] tabular-nums text-outline">
              {m.from === "user" ? m.time : m.by ? `${m.by} · Librarian · ${m.time}` : `Library Help Desk · ${m.time}`}
            </span>
          </div>
        ))}
        {typing && (
          <div className="flex items-start" aria-live="polite" aria-label="A librarian is typing">
            <div className="flex items-center gap-1 rounded-2xl rounded-bl-md border border-border-warm bg-[#F3F1EC]/70 px-3.5 py-3">
              {[0, 150, 300].map((d) => (
                <span key={d} className="h-1.5 w-1.5 animate-bounce rounded-full bg-outline" style={{ animationDelay: `${d}ms` }} />
              ))}
            </div>
          </div>
        )}
        {showSuggestions && (
          <div className="flex flex-wrap gap-2 pt-1">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => send(s)}
                className="rounded-full border border-border-warm bg-white px-3 py-1.5 font-label-md text-xs font-medium text-[#2F5D45] transition-colors hover:bg-[#c6ebd3]/50"
              >
                {s}
              </button>
            ))}
          </div>
        )}
        </div>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          send(draft);
        }}
        className="flex shrink-0 items-end gap-2 border-t border-border-warm bg-white p-3"
      >
        <textarea
          ref={inputRef}
          rows={1}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              send(draft);
            }
          }}
          placeholder="Ask the librarian a question…"
          className="max-h-24 min-h-[40px] flex-1 resize-none rounded-lg border border-border-warm bg-[#F3F1EC]/50 px-3 py-2.5 font-body-md text-sm text-on-surface placeholder:text-outline focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
        />
        <button
          type="submit"
          disabled={!draft.trim() || typing}
          aria-label="Send message"
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#2F5D45] text-white transition-colors hover:bg-[#274d3a] disabled:cursor-not-allowed disabled:opacity-40"
        >
          <Icon name="send" className="text-[18px]" />
        </button>
      </form>
    </div>
  );
}

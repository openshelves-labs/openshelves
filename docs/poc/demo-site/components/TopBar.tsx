"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";
import ProfileMenu from "./ProfileMenu";
import { useChat } from "@/lib/chat";

export default function TopBar() {
  const router = useRouter();
  const { unreadCount, toggleNotifications, closeNotifications, notificationsOpen } = useLibrary();
  const { open: chatOpen, toggle: toggleChat, close: closeChat } = useChat();
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = inputRef.current?.value.trim() ?? "";
    router.push(q ? `/catalog?q=${encodeURIComponent(q)}` : "/catalog");
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-end gap-6 border-b border-border-warm bg-[#F3F1EC]/90 px-8 backdrop-blur-sm">
      {/* Centred on the *viewport* (not the header): the header starts after the 16rem sidebar, so shift left by half of it. */}
      <form onSubmit={onSubmit} role="search" className="absolute left-1/2 top-1/2 -ml-32 w-[min(36rem,calc(100%-34rem))] -translate-x-1/2 -translate-y-1/2">
        <div className="relative flex items-center">
          <Icon name="search" className="pointer-events-none absolute left-3.5 text-lg text-outline" />
          <input
            ref={inputRef}
            type="search"
            placeholder="Search titles, authors, subjects, ISBN, DOI…"
            className="h-10 w-full rounded-xl border border-border-warm bg-white pl-10 pr-12 font-body-md text-sm text-on-surface transition-colors placeholder:text-outline focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
          <div className="absolute right-3 flex items-center">
            <kbd className="rounded border border-border-warm bg-[#eae8e3] px-1.5 py-0.5 font-metadata-caps text-[10px] font-medium text-on-surface-variant">⌘K</kbd>
          </div>
        </div>
      </form>
      <div className="flex shrink-0 items-center justify-end gap-1.5">
        <Link href="/lists" aria-label="Reading lists" title="Reading lists" className="rounded-lg p-2 text-on-surface-variant transition-colors hover:bg-white/60 hover:text-on-surface">
          <Icon name="folder" className="text-xl" />
        </Link>
        <button
          type="button"
          aria-label="Ask a librarian"
          title="Ask a librarian"
          aria-expanded={chatOpen}
          onClick={() => {
            closeNotifications();
            toggleChat();
          }}
          className={`rounded-lg p-2 transition-colors hover:bg-white/60 hover:text-on-surface ${chatOpen ? "bg-white/60 text-on-surface" : "text-on-surface-variant"}`}
        >
          <Icon name="support_agent" filled={chatOpen} className="text-xl" />
        </button>
        <button type="button" aria-label="Help" title="Help" className="rounded-lg p-2 text-on-surface-variant transition-colors hover:bg-white/60 hover:text-on-surface">
          <Icon name="help" className="text-xl" />
        </button>
        <button
          type="button"
          aria-label="Notifications"
          aria-expanded={notificationsOpen}
          onClick={() => {
            closeChat();
            toggleNotifications();
          }}
          className={`relative rounded-lg p-2 transition-colors hover:bg-white/60 hover:text-on-surface ${notificationsOpen ? "bg-white/60 text-on-surface" : "text-on-surface-variant"}`}
        >
          <Icon name="notifications" filled={notificationsOpen} className="text-xl" />
          {unreadCount > 0 && <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-primary ring-2 ring-[#F3F1EC]" />}
        </button>
        <div className="mx-2 h-5 w-px bg-border-warm" />
        <ProfileMenu />
      </div>
    </header>
  );
}

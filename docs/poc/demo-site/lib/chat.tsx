"use client";

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";

interface ChatCtx {
  open: boolean;
  toggle: () => void;
  close: () => void;
}

const Ctx = createContext<ChatCtx | null>(null);

/** UI open/close state for the "Ask a Librarian" chat (the conversation itself lives in the panel). */
export function ChatProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const toggle = useCallback(() => setOpen((v) => !v), []);
  const close = useCallback(() => setOpen(false), []);
  const value = useMemo(() => ({ open, toggle, close }), [open, toggle, close]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useChat(): ChatCtx {
  const c = useContext(Ctx);
  if (!c) throw new Error("useChat must be used inside <ChatProvider>");
  return c;
}

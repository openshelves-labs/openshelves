import type { ReactNode } from "react";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import LibrarianChat from "./LibrarianChat";
import { ChatProvider } from "@/lib/chat";

/** App chrome for signed-in screens: sidebar, top bar and the Ask-a-Librarian chat. Used by the route-group layouts. */
export default function AppShell({ children }: { children: ReactNode }) {
  return (
    <ChatProvider>
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar />
        {children}
      </div>
      <LibrarianChat />
    </ChatProvider>
  );
}

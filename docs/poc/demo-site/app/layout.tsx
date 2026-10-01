import type { Metadata } from "next";
import type { ReactNode } from "react";
import "@fontsource-variable/dm-sans";
import "@fontsource-variable/literata";
import "material-symbols/outlined.css";
import "./globals.css";
import * as api from "@/lib/api";
import LibraryProvider from "@/lib/store";

export const metadata: Metadata = {
  title: "OpenShelves — Central University Library",
  description: "Academic discovery platform: catalog, loans, reading lists and citations.",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const [user, books, loans, holds, lists, fines, stats, notifications] = await Promise.all([
    api.getCurrentUser(),
    api.getBooks(),
    api.getLoans(),
    api.getHolds(),
    api.getReadingLists(),
    api.getFineSummary(),
    api.getCatalogStats(),
    api.getNotifications(),
  ]);

  return (
    <html lang="en">
      <body className="flex min-h-screen bg-[#F3F1EC] font-body-md text-on-surface antialiased">
        <LibraryProvider
          init={{
            user,
            books,
            loans,
            holds,
            lists,
            notifications,
            outstandingBalance: fines.outstanding,
            newArrivalsCount: stats.newArrivals,
            recommendedCount: stats.recommended,
          }}
        >
          {children}
        </LibraryProvider>
      </body>
    </html>
  );
}

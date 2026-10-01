import { Suspense } from "react";
import * as api from "@/lib/api";
import CatalogView from "@/components/catalog/CatalogView";

export const metadata = { title: "Catalog — OpenShelves" };

export default async function CatalogPage() {
  const books = await api.getBooks();
  return (
    <main className="relative mx-auto min-h-screen w-full max-w-[1600px] px-gutter-lg py-margin">
      <Suspense fallback={null}>
        <CatalogView books={books} />
      </Suspense>
    </main>
  );
}

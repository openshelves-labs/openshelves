import { notFound } from "next/navigation";
import * as api from "@/lib/api";
import BookDetailView from "@/components/catalog/BookDetailView";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const book = await api.getBook(id);
  return { title: book ? `${book.title} — OpenShelves` : "Record not found — OpenShelves" };
}

export default async function ResourceDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [book, related] = await Promise.all([api.getBook(id), api.getRelatedBooks(id)]);
  if (!book) notFound();
  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <BookDetailView book={book} related={related} />
    </main>
  );
}

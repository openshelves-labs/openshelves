import { notFound } from "next/navigation";
import * as api from "@/lib/api";
import ListDetailView from "@/components/lists/ListDetailView";

export default async function ReadingListDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const list = await api.getReadingList(id);
  // Lists created in the browser (store) are unknown to the server; let the client resolve those.
  if (!list && !id.startsWith("list-new-")) notFound();
  return <ListDetailView id={id} initialList={list} />;
}

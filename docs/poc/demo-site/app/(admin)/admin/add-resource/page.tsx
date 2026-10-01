import { getCatalogingDraft } from "@/lib/api";
import AddResourceView from "@/components/admin/AddResourceView";

export default async function Page() {
  const draft = await getCatalogingDraft();
  return <AddResourceView draft={draft} />;
}

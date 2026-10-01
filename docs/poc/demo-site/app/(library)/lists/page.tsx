import * as api from "@/lib/api";
import ListsIndexView from "@/components/lists/ListsIndexView";

export default async function ReadingListsPage() {
  const [overview, ledger] = await Promise.all([api.getListsOverview(), api.getLedger(2)]);
  return <ListsIndexView overview={overview} ledger={ledger} />;
}

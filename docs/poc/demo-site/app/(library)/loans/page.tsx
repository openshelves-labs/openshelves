import { getHistory } from "@/lib/api";
import LoansView from "@/components/loans/LoansView";

export default async function Page() {
  const history = await getHistory();
  return <LoansView historyCount={history.length} />;
}

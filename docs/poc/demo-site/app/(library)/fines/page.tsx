import * as api from "@/lib/api";
import FinesView from "@/components/fines/FinesView";

export default async function Page() {
  const [fines, payments, summary, user] = await Promise.all([
    api.getFines(),
    api.getPayments(),
    api.getFineSummary(),
    api.getCurrentUser(),
  ]);
  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <FinesView initialFines={fines} initialPayments={payments} borrowLimit={summary.borrowLimit} user={user} />
    </main>
  );
}

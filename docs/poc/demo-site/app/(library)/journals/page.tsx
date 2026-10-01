import { getRecentSerials, getSerials } from "@/lib/api";
import JournalsView from "@/components/journals/JournalsView";

export default async function Page() {
  const [serials, recent] = await Promise.all([getSerials(), getRecentSerials()]);
  return <JournalsView serials={serials} recent={recent} />;
}

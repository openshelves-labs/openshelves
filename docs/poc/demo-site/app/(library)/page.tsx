import { getHomeHighlights } from "@/lib/api";
import HomeView from "@/components/home/HomeView";

export default async function Page() {
  const highlights = await getHomeHighlights();
  return <HomeView highlights={highlights} />;
}

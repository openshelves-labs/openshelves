import * as api from "@/lib/api";
import DigitalResourcesView from "@/components/digital/DigitalResourcesView";

export const metadata = { title: "Digital Resources — OpenShelves" };

export default async function DigitalResourcesPage() {
  const [resources, tabs] = await Promise.all([api.getDigitalResources(), api.getDigitalResourceTabs()]);
  return (
    <main className="w-full max-w-[1600px] mx-auto px-8 py-8 space-y-9">
      <DigitalResourcesView resources={resources} tabs={tabs} />
    </main>
  );
}

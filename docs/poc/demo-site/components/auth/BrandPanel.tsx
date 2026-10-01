import Icon from "@/components/Icon";
import Logo from "@/components/Logo";

const PILLARS = [
  { numeral: "I", title: "Federated Discovery", icon: "travel_explore", body: "Search simultaneously across university special collections, illuminated folios, incunabula, and modern indexed serials." },
  { numeral: "II", title: "Critical Apparatus & Citation", icon: "format_quote", body: "Direct export to BibTeX, Zotero, and CSL-JSON with canonical persistent identifiers (DOI, ARK, and URN)." },
  { numeral: "III", title: "Shelf Reserve & Folios", icon: "bookmarks", body: "Curate syllabus course reserves, request closed-stack retrieval slips, and follow real-time physical shelf locations." },
];

export default function BrandPanel() {
  return (
    <div className="relative flex flex-col justify-between overflow-hidden bg-surface-container-low p-8 lg:w-5/12 lg:p-12">
      <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-secondary-container/15 blur-3xl" />
      <div className="relative z-10">
        <div className="mb-8 flex items-center gap-3.5">
          <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-surface-container-lowest p-1 shadow-sm">
            <Logo size={36} />
          </span>
          <div className="flex flex-col">
            <span className="font-headline-sm text-headline-sm tracking-tight text-primary">OpenShelves</span>
            <span className="font-metadata-caps text-metadata-caps uppercase tracking-wider text-outline">Central University Library</span>
          </div>
        </div>

        <div className="mb-10 space-y-3">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary-container/30 px-2.5 py-1 font-label-md text-label-md text-primary">
            <Icon name="assured_workload" filled className="text-[15px]" />
            Federated Academic Gateway
          </span>
          <h1 className="font-headline-lg text-headline-lg leading-tight text-on-surface">Open discovery for academic libraries.</h1>
          <p className="font-body-md text-body-md leading-relaxed text-on-surface-variant">
            The contemplative nexus connecting rare books collections, scholarly monograph archives, and active faculty research across global consortiums.
          </p>
        </div>

        <div className="space-y-6 pt-2">
          {PILLARS.map((p) => (
            <div key={p.numeral} className="flex items-start gap-4">
              <div className="mt-0.5 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-md bg-surface-container-lowest text-primary shadow-sm">
                <span className="font-label-md text-label-md font-semibold">{p.numeral}</span>
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-1.5 font-label-lg text-label-lg text-on-surface">
                  <span>{p.title}</span>
                  <Icon name={p.icon} className="text-[16px] text-outline" />
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant">{p.body}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="relative z-10 mt-8 pt-10">
        <div className="flex items-center gap-3 rounded-lg bg-surface-container-lowest/80 p-3.5 shadow-sm backdrop-blur-sm">
          <Icon name="verified" filled className="flex-shrink-0 text-xl text-primary" />
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Connecting <span className="font-semibold text-primary">420+ institutional repositories</span>, bodily archives, and university consortia worldwide.
          </p>
        </div>
      </div>
    </div>
  );
}

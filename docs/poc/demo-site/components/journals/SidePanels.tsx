import Icon from "@/components/Icon";

/** Citation matcher, document delivery and preservation note (static, inert controls). */
export default function SidePanels() {
  return (
    <>
      <section className="bg-surface-container-lowest rounded-xl p-5 shadow-sm space-y-3">
        <div className="flex items-center gap-2">
          <Icon name="travel_explore" className="text-primary text-lg" />
          <h3 className="font-headline-sm text-on-surface font-semibold">Citation &amp; DOI Matcher</h3>
        </div>
        <p className="font-body-sm text-on-surface-variant">
          Have an exact paper citation, DOI link, or ISSN code? Jump straight into specific volume and issue archives.
        </p>
        <div className="space-y-2 pt-1">
          <input
            type="text"
            placeholder="e.g. 10.1038/s41558-023-01824 or 0028-0836"
            className="w-full h-10 px-3 font-body-sm bg-surface-container-low text-on-surface rounded-lg placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest transition-colors"
          />
          <button
            type="button"
            className="w-full h-10 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-md font-semibold transition-colors flex items-center justify-center gap-1.5"
          >
            <Icon name="arrow_forward" className="text-[17px] text-primary" />
            <span>Resolve Citation Reference</span>
          </button>
        </div>
      </section>

      <section className="bg-primary text-on-primary rounded-xl p-5 shadow-sm relative overflow-hidden space-y-3">
        <div className="flex items-center gap-2 text-secondary-fixed">
          <Icon name="local_shipping" className="text-lg" />
          <span className="font-metadata-caps uppercase tracking-wider">Document Delivery</span>
        </div>
        <h3 className="font-headline-sm text-on-primary font-semibold leading-tight">Unlisted Volume or Rare Pre-1970 Microform?</h3>
        <p className="font-body-sm text-on-primary-container leading-relaxed">
          Through the Central University Library Inter-Library Loan (ILL) compact, our librarians can retrieve physical off-site rolls,
          high-resolution scans, or international consortium issues within 48 business hours.
        </p>
        <div className="pt-2">
          <button
            type="button"
            className="w-full h-10 rounded-lg bg-secondary-fixed text-on-secondary-fixed hover:bg-primary-fixed-dim transition-colors font-label-lg font-semibold flex items-center justify-center gap-2 shadow-sm"
          >
            <Icon name="outgoing_mail" className="text-[18px]" />
            <span>Request Document Delivery (ILL)</span>
          </button>
        </div>
      </section>

      <div className="p-4 rounded-xl bg-surface-container-low text-on-surface-variant space-y-1.5">
        <div className="flex items-center gap-1.5 text-secondary font-label-md font-semibold">
          <Icon name="verified_user" className="text-[16px]" />
          <span>Preservation Guarantee</span>
        </div>
        <p className="font-body-sm text-on-surface-variant leading-relaxed">
          All digital subscriptions are safeguarded under Portico and CLOCKSS digital escrow networks, maintaining persistent researcher
          access indefinitely.
        </p>
      </div>
    </>
  );
}

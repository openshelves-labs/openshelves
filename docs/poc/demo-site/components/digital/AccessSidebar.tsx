import Icon from "@/components/Icon";

const STEPS = [
  { title: "Institutional SSO (Shibboleth)", body: "Look for “Log in via your institution” on publisher landing pages and enter your University ID." },
  { title: "Central University VPN", body: "For IP-restricted internal terminals, raw datasets, and historical archives, activate the campus VPN tunnel." },
  { title: "EZproxy Browser Bookmarklet", body: "Drag our proxy bookmarklet to your favorites bar to instantly reload paywalled articles through university access." },
];

export default function AccessSidebar() {
  return (
    <aside className="lg:col-span-4 flex flex-col space-y-6 lg:sticky lg:top-20">
      <section className="bg-surface-container-low rounded-2xl p-6 shadow-sm flex flex-col space-y-5">
        <div className="flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center shrink-0">
            <Icon name="key" className="text-2xl" />
          </div>
          <div className="space-y-0.5">
            <span className="font-metadata-caps text-[10px] uppercase tracking-wider text-primary font-semibold">Researcher Guidance</span>
            <h3 className="font-headline-sm text-on-surface">Off-Campus &amp; Remote Access</h3>
          </div>
        </div>
        <p className="font-body-sm text-on-surface-variant leading-relaxed">
          Accessing university-licensed digital databases from home, fieldwork, or collaborative partner institutions is seamless through our single sign-on system.
        </p>
        <div className="space-y-4">
          {STEPS.map((s, i) => (
            <div key={s.title} className="flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-surface-container-highest text-on-surface flex items-center justify-center font-metadata-caps text-[11px] font-bold shrink-0 mt-0.5 tabular-nums">
                {i + 1}
              </span>
              <div className="space-y-1">
                <h4 className="font-label-md text-on-surface font-semibold">{s.title}</h4>
                <p className="font-body-sm text-on-surface-variant">{s.body}</p>
              </div>
            </div>
          ))}
        </div>
        <div className="pt-2 flex flex-col space-y-2">
          <button type="button" className="inline-flex items-center justify-center gap-2 w-full h-10 px-4 rounded-xl bg-surface-container-lowest text-primary font-label-lg font-semibold hover:bg-surface-container transition-colors">
            <Icon name="bookmark" className="text-base" />
            <span>Install EZproxy Bookmarklet</span>
          </button>
          <button type="button" className="inline-flex items-center justify-center gap-2 w-full h-10 px-4 rounded-xl bg-surface-container-lowest text-on-surface font-label-lg hover:bg-surface-container transition-colors">
            <Icon name="router" className="text-base" />
            <span>VPN Configuration Guides</span>
          </button>
          <button type="button" className="font-label-md text-outline hover:text-error transition-colors pt-1 flex items-center gap-1.5 justify-center">
            <Icon name="report_problem" className="text-sm" />
            <span>Report broken proxy or vendor link</span>
          </button>
        </div>
      </section>

      <section className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm flex flex-col space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-surface-container text-primary flex items-center justify-center shrink-0">
            <Icon name="analytics" className="text-xl" />
          </div>
          <div>
            <h3 className="font-headline-sm text-on-surface">Specialized Datasets</h3>
            <span className="font-metadata-caps text-[11px] text-outline">Research Data Management Desk</span>
          </div>
        </div>
        <p className="font-body-sm text-on-surface-variant leading-relaxed">
          Need access to Bloomberg Terminals, Wharton Research Data Services (WRDS), ICPSR archives, or restricted GIS geospatial boundaries?
        </p>
        <div className="bg-surface-container-low p-3.5 rounded-xl space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-label-md text-on-surface font-semibold">Office Hours:</span>
            <span className="font-body-sm text-on-surface-variant">Mon &amp; Thu, 14:00–17:00</span>
          </div>
          <div className="flex items-center justify-between text-xs">
            <span className="font-label-md text-on-surface font-semibold">Location:</span>
            <span className="font-body-sm text-on-surface-variant">Central Library, Seminar Room 3B</span>
          </div>
        </div>
        <button type="button" className="w-full h-10 px-4 rounded-xl bg-primary text-on-primary font-label-lg font-semibold hover:bg-primary/90 transition-colors shadow-sm flex items-center justify-center gap-2">
          <Icon name="event" className="text-base" />
          <span>Book Data Consultation</span>
        </button>
      </section>

      <section className="bg-surface-container-lowest rounded-2xl p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-secondary opacity-75" />
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-primary" />
            </span>
            <span className="font-label-md font-semibold text-on-surface">Systems Operational</span>
          </div>
          <span className="font-metadata-caps text-[10px] uppercase text-outline">Live Check</span>
        </div>
        {[
          { label: "EZproxy Gateway (Primary)", value: "99.98%" },
          { label: "Shibboleth Federated IdP", value: "Nominal" },
        ].map((row, i) => (
          <div key={row.label} className={`space-y-2 ${i === 0 ? "pt-1" : ""}`}>
            <div className="flex items-center justify-between font-body-sm">
              <span className="text-on-surface-variant">{row.label}</span>
              <span className="font-metadata-caps text-[11px] text-primary font-semibold tabular-nums">{row.value}</span>
            </div>
            <div className="w-full bg-surface-container h-1.5 rounded-full overflow-hidden">
              <div className="bg-primary h-full rounded-full w-full" />
            </div>
          </div>
        ))}
        <p className="font-metadata-caps text-[10px] text-outline text-center pt-2">All 32 core vendor connections verified • Last updated 4m ago</p>
      </section>
    </aside>
  );
}

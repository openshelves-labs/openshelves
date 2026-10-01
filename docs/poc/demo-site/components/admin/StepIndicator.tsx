import Icon from "@/components/Icon";

export default function StepIndicator() {
  return (
    <div className="rounded-xl bg-surface-container-lowest p-space-md shadow-sm">
      <ol className="grid grid-cols-1 gap-space-md text-left md:grid-cols-3">
        <li className="flex items-center gap-space-sm">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary font-label-md text-label-md text-on-primary">
            <Icon name="check" className="text-[16px]" />
          </span>
          <div className="flex min-w-0 flex-col">
            <span className="font-metadata-caps text-metadata-caps font-bold uppercase text-primary">Step 1 • Query</span>
            <span className="truncate font-label-lg text-label-lg font-medium text-on-surface">Lookup &amp; Identifiers</span>
          </div>
        </li>
        <li className="-m-2 flex items-center gap-space-sm rounded-lg bg-surface-container-low p-2">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-container font-label-md text-label-md text-on-primary">2</span>
          <div className="flex min-w-0 flex-col">
            <div className="flex items-center gap-1.5">
              <span className="font-metadata-caps text-metadata-caps font-bold uppercase text-primary-container">Step 2 • Active</span>
              <span className="h-2 w-2 rounded-full bg-error" />
            </div>
            <span className="truncate font-label-lg text-label-lg font-semibold text-on-surface">Review &amp; Resolve Conflicts</span>
          </div>
        </li>
        <li className="flex items-center gap-space-sm opacity-60">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container-high font-label-md text-label-md text-on-surface-variant">3</span>
          <div className="flex min-w-0 flex-col">
            <span className="font-metadata-caps text-metadata-caps font-semibold uppercase text-outline">Step 3 • Finalize</span>
            <span className="truncate font-label-lg text-label-lg font-medium text-on-surface-variant">Holdings &amp; Classification</span>
          </div>
        </li>
      </ol>
    </div>
  );
}

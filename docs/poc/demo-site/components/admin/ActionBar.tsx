import Icon from "@/components/Icon";

export type SaveState = "auto" | "dirty" | "saved";

interface Props {
  saveState: SaveState;
  onSave: () => void;
  onReset: () => void;
}

export default function ActionBar({ saveState, onSave, onReset }: Props) {
  const saved = saveState === "saved";
  return (
    <div className="sticky bottom-4 z-20 flex flex-col items-center justify-between gap-space-md rounded-xl bg-surface-container-lowest/95 p-space-md shadow-xl backdrop-blur sm:flex-row">
      <div className="flex items-center gap-2 font-label-md text-label-md text-on-surface-variant" aria-live="polite">
        <span className={`h-2 w-2 rounded-full bg-primary ${saveState === "auto" ? "animate-pulse" : ""}`} />
        {saveState === "auto" && (
          <span>
            Draft saved automatically <time className="font-medium text-on-surface">2 minutes ago</time>
          </span>
        )}
        {saveState === "dirty" && <span>Unsaved changes since last save</span>}
        {saved && (
          <span>
            Saved to catalog <time className="font-medium text-on-surface">just now</time>
          </span>
        )}
      </div>
      <div className="flex w-full flex-wrap items-center justify-end gap-space-sm sm:w-auto">
        <button type="button" onClick={onReset} className="h-10 rounded-lg bg-surface-container px-4 font-label-lg text-label-lg text-on-surface-variant transition-colors hover:bg-surface-container-high">
          Discard &amp; Reset
        </button>
        <button type="button" className="flex h-10 items-center gap-1.5 rounded-lg bg-surface-container-lowest px-4 font-label-lg text-label-lg text-on-surface transition-colors hover:bg-surface-container">
          <Icon name="inventory_2" className="text-[18px]" />
          Add Physical Copies &amp; Barcodes...
        </button>
        <button
          type="button"
          onClick={onSave}
          disabled={saved}
          className="flex h-10 items-center gap-2 rounded-lg bg-primary px-6 font-label-lg text-label-lg text-on-primary shadow-md transition-all hover:bg-primary-container active:scale-[0.98] disabled:cursor-default disabled:opacity-90"
        >
          <Icon name={saved ? "check_circle" : "check"} className="text-[20px]" />
          {saved ? "Saved to catalog" : "Save Resource to Catalog"}
        </button>
      </div>
    </div>
  );
}

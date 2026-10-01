"use client";

import Icon from "@/components/Icon";

/** ISSN Quick Lookup dialog (query button is inert). */
export default function IssnModal({ onClose }: { onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div role="dialog" aria-modal="true" aria-label="Institutional ISSN Lookup" className="bg-surface-container-lowest rounded-xl max-w-lg w-full p-6 shadow-xl space-y-4">
        <div className="flex items-center justify-between pb-2">
          <div className="flex items-center gap-2">
            <Icon name="search_check" className="text-primary text-xl" />
            <h3 className="font-headline-sm text-on-surface">Institutional ISSN Lookup</h3>
          </div>
          <button type="button" aria-label="Close" onClick={onClose} className="p-1 rounded-lg text-outline hover:text-on-surface transition-colors">
            <Icon name="close" className="text-xl" />
          </button>
        </div>
        <p className="font-body-md text-on-surface-variant">
          Query the global ISSN International Centre registry synchronized with the Central University Library archival database.
        </p>
        <div className="space-y-3 pt-2">
          <div>
            <label className="font-label-md text-on-surface block mb-1">Standard Serial Number (ISSN or E-ISSN)</label>
            <input
              type="text"
              placeholder="XXXX-XXXX (e.g. 1469-185X)"
              className="w-full h-11 px-3.5 bg-surface-container-low text-on-surface rounded-lg font-body-md placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest transition-colors"
            />
          </div>
          <div className="flex items-center gap-2 pt-2">
            <button type="button" className="flex-1 h-11 rounded-lg bg-primary text-on-primary hover:bg-on-primary-fixed-variant font-label-lg transition-colors flex items-center justify-center gap-2">
              <Icon name="search" className="text-lg" />
              <span>Query Serial Record</span>
            </button>
            <button type="button" onClick={onClose} className="h-11 px-4 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-lg transition-colors">
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

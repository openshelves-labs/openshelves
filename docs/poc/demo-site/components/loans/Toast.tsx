"use client";

import Icon from "@/components/Icon";

export interface ToastState {
  id: number;
  message: string;
  icon: string;
}

/** Bottom-right notification toast (visible while `toast` is set). */
export default function Toast({ toast }: { toast: ToastState | null }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`pointer-events-none fixed bottom-6 right-6 z-50 flex max-w-md items-center gap-3 rounded-xl bg-on-surface px-4 py-3 text-inverse-on-surface shadow-xl transition-all duration-300 ${
        toast ? "translate-y-0 opacity-100" : "translate-y-24 opacity-0"
      }`}
    >
      <Icon name={toast?.icon ?? "check_circle"} className="text-secondary-fixed" />
      <span className="font-body-sm text-body-sm">{toast?.message ?? ""}</span>
    </div>
  );
}

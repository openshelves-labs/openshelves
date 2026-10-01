import type { ReactNode } from "react";
import type { AvailabilityStatus } from "@/types";
import Icon from "./Icon";

export type ChipTone = "success" | "warning" | "critical" | "neutral" | "dark";

const TONES: Record<ChipTone, string> = {
  success: "bg-[#c6ebd3] text-[#2F5D45]",
  warning: "bg-[#FDF4E7] text-[#C27D23]",
  critical: "bg-[#ffdad6] text-[#93000a]",
  neutral: "bg-[#f0eee9] text-on-surface-variant",
  dark: "bg-[#1E3F2E] text-white",
};

const DOT: Record<ChipTone, string> = {
  success: "bg-[#2F5D45]",
  warning: "bg-[#C27D23]",
  critical: "bg-[#B83A30]",
  neutral: "bg-outline",
  dark: "bg-white",
};

/** Availability status → chip tone. */
export const availabilityTone: Record<AvailabilityStatus, ChipTone> = {
  available: "success",
  online: "success",
  open_access: "success",
  checked_out: "critical",
  reserve: "warning",
  restricted: "neutral",
  in_transit: "warning",
  on_order: "warning",
};

interface Props {
  tone?: ChipTone;
  children: ReactNode;
  /** leading coloured dot */
  dot?: boolean;
  /** leading Material Symbols glyph */
  icon?: string;
  /** uppercase tracked metadata style */
  caps?: boolean;
  className?: string;
}

/** Pill/badge used for availability, due dates, peer-review flags, etc. */
export default function StatusChip({ tone = "neutral", children, dot, icon, caps, className = "" }: Props) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded px-2 py-0.5 text-[11px] font-medium leading-4 ${
        caps ? "font-metadata-caps uppercase tracking-wider font-semibold" : ""
      } ${TONES[tone]} ${className}`}
    >
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${DOT[tone]}`} />}
      {icon && <Icon name={icon} className="text-[13px]" />}
      {children}
    </span>
  );
}

/** Convenience: chip driven straight from a Book's availability. */
export function AvailabilityChip({ status, label }: { status: AvailabilityStatus; label: string }) {
  return (
    <StatusChip tone={availabilityTone[status]} dot>
      {label}
    </StatusChip>
  );
}

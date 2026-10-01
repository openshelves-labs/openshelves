import type { Book } from "@/types";
import Icon from "@/components/Icon";
import { CHIP } from "./arrivalUtils";

export default function AvailabilityPill({ book }: { book: Book }) {
  const c = CHIP[book.availabilityStatus];
  return (
    <div className={`inline-flex max-w-full items-center gap-1.5 px-2 py-1 rounded font-label-md text-[11px] font-medium ${c.cls}`}>
      {c.dot && <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${c.dot}`} />}
      {c.icon && <Icon name={c.icon} className={`text-[13px] ${book.availabilityStatus === "restricted" ? "" : "text-primary"}`} />}
      <span className="truncate">{book.availabilityLabel}</span>
    </div>
  );
}

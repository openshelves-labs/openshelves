"use client";

import type { ID } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";

interface Props {
  bookId: ID;
  label?: string; // idle label
  placedLabel?: string; // after the hold exists
  icon?: string;
  className: string; // idle style
  placedClassName?: string; // style once hold placed (defaults to idle style, muted)
  iconClassName?: string;
}

/** Place Hold → creates a Hold in client state; clicking again while placed cancels it. */
export default function HoldButton({
  bookId,
  label = "Place Hold",
  placedLabel = "Hold Placed",
  icon,
  className,
  placedClassName,
  iconClassName = "text-sm",
}: Props) {
  const { holdFor, placeHold, cancelHold } = useLibrary();
  const hold = holdFor(bookId);
  const placed = !!hold;
  return (
    <button
      type="button"
      aria-pressed={placed}
      title={placed ? "Click to cancel this hold" : undefined}
      className={placed ? placedClassName ?? `${className} opacity-70` : className}
      onClick={() => (hold ? cancelHold(hold.id) : placeHold(bookId))}
    >
      {(placed ? "check" : icon) && <Icon name={placed ? "check" : icon!} className={iconClassName} />}
      <span>{placed ? placedLabel : label}</span>
    </button>
  );
}

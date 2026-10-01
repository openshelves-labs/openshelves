"use client";

import type { ID } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "./Icon";

interface Props {
  bookId: ID;
  label?: string;
  savedLabel?: string;
  className: string;
  savedClassName?: string;
  /** glyph shown when idle; saved state uses the filled variant of the same glyph */
  icon?: string;
  iconClassName?: string;
  /** hide the text (icon-only button) */
  iconOnly?: boolean;
}

/** Save / Bookmark toggle backed by client state. */
export default function SaveButton({
  bookId,
  label = "Bookmark",
  savedLabel = "Saved",
  className,
  savedClassName,
  icon = "bookmark",
  iconClassName = "text-sm",
  iconOnly,
}: Props) {
  const { isSaved, toggleSave } = useLibrary();
  const saved = isSaved(bookId);
  return (
    <button
      type="button"
      aria-pressed={saved}
      aria-label={iconOnly ? (saved ? savedLabel : label) : undefined}
      className={saved ? savedClassName ?? `${className} !border-primary !text-primary` : className}
      onClick={() => toggleSave(bookId)}
    >
      <Icon name={icon} filled={saved} className={iconClassName} />
      {!iconOnly && <span>{saved ? savedLabel : label}</span>}
    </button>
  );
}

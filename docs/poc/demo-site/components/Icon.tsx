import type { CSSProperties } from "react";

/** Material Symbols Outlined glyph. Size via className (e.g. "text-[20px]"). */
export default function Icon({
  name,
  className = "",
  filled = false,
  style,
}: {
  name: string;
  className?: string;
  filled?: boolean;
  style?: CSSProperties;
}) {
  return (
    <span aria-hidden="true" className={`material-symbols-outlined${filled ? " fill" : ""} ${className}`} style={style}>
      {name}
    </span>
  );
}

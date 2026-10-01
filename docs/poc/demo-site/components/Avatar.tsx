"use client";

import { useState } from "react";

/** Round profile photo; falls back to the initial on a forest-green disc if the image can't load. */
export default function Avatar({ name, initial, src, className = "h-8 w-8 text-xs" }: { name: string; initial: string; src?: string; className?: string }) {
  const [failed, setFailed] = useState(false);
  if (src && !failed) {
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={src} alt={name} referrerPolicy="no-referrer" onError={() => setFailed(true)} className={`rounded-full border border-border-warm object-cover ${className}`} />;
  }
  return (
    <div className={`flex items-center justify-center rounded-full border border-[#2F5D45]/30 bg-[#2F5D45] font-title-editorial font-semibold text-white ${className}`}>{initial}</div>
  );
}

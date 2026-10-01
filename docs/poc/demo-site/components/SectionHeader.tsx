import Link from "next/link";
import type { ReactNode } from "react";
import Icon from "./Icon";

/** Ruled section heading used on Home: title + meta on the left, "view all" link on the right. */
export default function SectionHeader({
  title,
  meta,
  linkLabel,
  href,
  right,
}: {
  title: string;
  meta?: ReactNode;
  linkLabel?: string;
  href?: string;
  right?: ReactNode;
}) {
  return (
    <div className="flex items-baseline justify-between border-b border-border-warm pb-2">
      <div className="flex items-baseline gap-2.5">
        <h2 className="font-title-editorial text-xl font-semibold text-on-surface">{title}</h2>
        {meta}
      </div>
      {right ??
        (linkLabel && href && (
          <Link href={href} className="inline-flex items-center gap-1 font-label-lg text-xs font-medium text-primary hover:underline">
            <span>{linkLabel}</span>
            <Icon name="chevron_right" className="text-sm" />
          </Link>
        ))}
    </div>
  );
}

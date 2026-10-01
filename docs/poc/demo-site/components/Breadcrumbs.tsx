import Link from "next/link";
import Icon from "./Icon";

export interface Crumb {
  label: string;
  href?: string;
}

/** "Home › My Library › Reading Lists" — last crumb is the current page. */
export default function Breadcrumbs({ items, homeIcon = false }: { items: Crumb[]; homeIcon?: boolean }) {
  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-2 font-body-sm text-sm text-on-surface-variant">
      {items.map((c, i) => {
        const last = i === items.length - 1;
        return (
          <span key={`${c.label}-${i}`} className="flex items-center gap-2">
            {i === 0 && homeIcon && <Icon name="home" className="text-base" />}
            {c.href && !last ? (
              <Link href={c.href} className="hover:text-primary">
                {c.label}
              </Link>
            ) : (
              <span className={last ? "font-medium text-primary" : ""}>{c.label}</span>
            )}
            {!last && <Icon name="chevron_right" className="text-sm text-outline" />}
          </span>
        );
      })}
    </nav>
  );
}

import Icon from "@/components/Icon";

function pageWindow(page: number, last: number): (number | "gap")[] {
  if (last <= 5) return Array.from({ length: last }, (_, i) => i + 1);
  const start = Math.min(Math.max(page - 1, 1), last - 3);
  const nums = [start, start + 1, start + 2];
  const out: (number | "gap")[] = [];
  if (start > 1) out.push(1, ...(start > 2 ? (["gap"] as const) : []));
  out.push(...nums.filter((n) => n < last));
  if (nums[2] < last - 1) out.push("gap");
  out.push(last);
  return out;
}

export default function Pagination({
  page,
  last,
  onChange,
}: {
  page: number;
  last: number;
  onChange: (p: number) => void;
}) {
  const btn =
    "inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-container text-on-surface font-label-md hover:bg-surface-container-high transition-colors disabled:opacity-50 disabled:pointer-events-none";
  return (
    <nav className="pt-4 flex items-center justify-between border-t-0 bg-surface-container-lowest p-4 rounded-2xl shadow-sm">
      <button type="button" className={btn} disabled={page <= 1} onClick={() => onChange(page - 1)}>
        <Icon name="arrow_back" className="text-sm" />
        <span>Previous</span>
      </button>
      <div className="flex items-center gap-1">
        {pageWindow(page, last).map((p, i) =>
          p === "gap" ? (
            <span key={`g${i}`} className="px-2 text-outline">
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              onClick={() => onChange(p)}
              aria-current={p === page ? "page" : undefined}
              className={`w-9 h-9 rounded-lg font-label-md tabular-nums ${
                p === page ? "bg-primary text-on-primary font-semibold" : "hover:bg-surface-container text-on-surface"
              }`}
            >
              {p}
            </button>
          ),
        )}
      </div>
      <button type="button" className={btn} disabled={page >= last} onClick={() => onChange(page + 1)}>
        <span>Next</span>
        <Icon name="arrow_forward" className="text-sm" />
      </button>
    </nav>
  );
}

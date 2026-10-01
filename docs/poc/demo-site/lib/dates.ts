/**
 * The mock data is anchored to the reference screens (late Oct 2024).
 * `NOW` is a fixed "today" so relative labels ("Due in 3 days") match the designs.
 * Swap for `new Date()` when real data arrives.
 */
export const NOW = new Date("2024-10-24T09:00:00Z");

const DAY = 86_400_000;

export function toDate(iso: string): Date {
  return new Date(iso.length === 10 ? `${iso}T12:00:00Z` : iso);
}

export function daysUntil(iso: string, from: Date = NOW): number {
  const a = Date.UTC(toDate(iso).getUTCFullYear(), toDate(iso).getUTCMonth(), toDate(iso).getUTCDate());
  const b = Date.UTC(from.getUTCFullYear(), from.getUTCMonth(), from.getUTCDate());
  return Math.round((a - b) / DAY);
}

export function addDays(iso: string, days: number): string {
  const d = toDate(iso);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** "Oct 27, 2024" */
export function formatDate(iso: string): string {
  return toDate(iso).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" });
}
/** "Oct 27" */
export function formatShortDate(iso: string): string {
  return toDate(iso).toLocaleDateString("en-US", { month: "short", day: "numeric", timeZone: "UTC" });
}
/** "Tuesday, Oct 29" */
export function formatWeekdayDate(iso: string): string {
  return toDate(iso).toLocaleDateString("en-US", { weekday: "long", month: "short", day: "numeric", timeZone: "UTC" });
}

export function dueLabel(iso: string): string {
  const n = daysUntil(iso);
  if (n < 0) return `Overdue by ${-n} day${n === -1 ? "" : "s"}`;
  if (n === 0) return "Due today";
  return `Due in ${n} day${n === 1 ? "" : "s"}`;
}

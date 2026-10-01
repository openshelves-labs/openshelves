import type { HistoryEntry, ReadingSummary } from "@/types";
import { history, readingSummary } from "@/mock";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getHistory(): Promise<HistoryEntry[]> {
  return clone(history);
}

export async function getReadingSummary(): Promise<ReadingSummary> {
  return clone(readingSummary);
}

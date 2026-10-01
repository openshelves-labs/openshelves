import type { RecentSerial, Serial } from "@/types";
import { recentSerials, serials } from "@/mock/journals";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getSerials(): Promise<Serial[]> {
  return clone(serials);
}

export async function getRecentSerials(): Promise<RecentSerial[]> {
  return clone(recentSerials);
}

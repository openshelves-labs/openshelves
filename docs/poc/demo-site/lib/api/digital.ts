import type { DigitalResource, DigitalResourceTab } from "@/types";
import { digitalResources, digitalTabs } from "@/mock/digital-resources";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getDigitalResources(): Promise<DigitalResource[]> {
  return clone(digitalResources);
}

export async function getDigitalResourceTabs(): Promise<DigitalResourceTab[]> {
  return clone(digitalTabs);
}

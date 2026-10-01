import type { CatalogingDraft } from "@/types";
import { catalogingDraft } from "@/mock/cataloging";

export async function getCatalogingDraft(): Promise<CatalogingDraft> {
  return structuredClone(catalogingDraft);
}

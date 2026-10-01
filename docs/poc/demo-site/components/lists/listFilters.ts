import type { ReadingList } from "@/types";

export type FolioFilter = "all" | "course" | "grant" | "archive" | "shared";

export const FOLIO_FILTERS: { id: FolioFilter; label: string; test: (l: ReadingList) => boolean }[] = [
  { id: "all", label: "All", test: () => true },
  { id: "course", label: "Course Syllabi", test: (l) => l.categoryKind === "course" || l.categoryKind === "study" },
  { id: "grant", label: "Research Grants", test: (l) => l.categoryKind === "grant" || l.categoryKind === "shared" },
  { id: "archive", label: "Personal Archive", test: (l) => l.categoryKind === "archive" },
  { id: "shared", label: "Shared / Collaborative", test: (l) => l.collaborators.length > 0 || !!l.enrolledCount || l.categoryKind === "shared" },
];

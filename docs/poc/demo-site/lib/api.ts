/**
 * Data-layer barrel. Import from "@/lib/api" only — never from "@/mock" —
 * so real REST calls can replace these modules later.
 */
export * from "./api/books";
export * from "./api/circulation";
export * from "./api/lists";
export * from "./api/fines";
export * from "./api/history";
export * from "./api/user";
export * from "./api/notifications";
export * from "./api/digital";
export * from "./api/journals";
export * from "./api/cataloging";

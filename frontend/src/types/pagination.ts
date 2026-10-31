/**
 * Paged list response returned by the backend.
 *
 * Mirrors Spring Data's `PagedModel` (the stable JSON shape for `Page<T>`):
 *
 *   { "content": [ ... ], "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 } }
 */
export interface Page<T> {

  /** Items on the current page. */
  content: T[];

  /** Position of this page within the full result set. */
  page: PageMetadata;
}


/** Page position and totals; mirrors `PagedModel.PageMetadata`. */
export interface PageMetadata {

  /** Requested page size (maximum items per page). */
  size: number;

  /** Current page index. Zero-based: the first page is 0. */
  number: number;

  /** Total number of items across all pages. */
  totalElements: number;

  /** Total number of pages. */
  totalPages: number;
}


/**
 * Query parameters for a paged request, as read by Spring's `Pageable` resolver.
 * All fields are optional; the backend applies its own defaults.
 */
export interface PageRequest {

  /** Page index to fetch. Zero-based: the first page is 0. */
  page?: number;

  /** Number of items per page. */
  size?: number;

  /** One or more sort orders, e.g. `"title,asc"`. Sent as repeated `sort` params. */
  sort?: SortParam | SortParam[];
}


/** A single sort order in Spring's `property,direction` format, e.g. `"createdAt,desc"`. */
export type SortParam = `${string},${"asc" | "desc"}`;

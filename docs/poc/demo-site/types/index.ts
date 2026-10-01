/**
 * Domain entities. Shapes mirror what a REST backend would return:
 * camelCase, string ids, ISO-8601 date strings (YYYY-MM-DD or full ISO).
 * UI-only state (selected tabs, filters, modal open flags) does NOT live here.
 */

export type ID = string;
export type ISODate = string;

/* ───────────── People & places ───────────── */

export interface User {
  id: ID;
  name: string; // display name, e.g. "Arpan"
  initial: string;
  accountType: string; // "Scholar Account"
  email: string;
  cardNumber: string;
  department: string;
  firstName: string;
  studentId?: string;
  program?: string;
  avatarUrl?: string;
}

export interface Branch {
  id: ID;
  name: string;
  address?: string;
}

/* ───────────── Catalog ───────────── */

export type ResourceType =
  | "monograph"
  | "ebook"
  | "journal"
  | "journal-article"
  | "dissertation"
  | "archival"
  | "digital-resource"
  | "special-collection"
  | "manuscript-guide"
  | "folio-edition";

export type AvailabilityStatus =
  | "available" // physical copy on shelf
  | "online" // instant online / full-text access
  | "open_access"
  | "checked_out"
  | "reserve" // course reserve / reserve stacks
  | "restricted" // rare books room / non-circulating
  | "in_transit"
  | "on_order";

export interface Contributor {
  /** Display string exactly as shown, e.g. "Dr. Marion Vance" */
  name: string;
  family: string;
  given: string;
  role: "author" | "co-author" | "editor" | "foreword" | "translator" | "curator";
  affiliation?: string;
}

export interface Copy {
  id: ID;
  bookId: ID;
  barcode: string;
  branchId: ID;
  branchName: string;
  location: string; // "3rd Floor, Section East, Shelf 148"
  callNumber: string;
  status: "available" | "on_loan" | "reserve" | "in_transit" | "non_circulating";
  dueDate?: ISODate;
  note?: string;
}

export interface TocEntry {
  title: string;
  page?: string;
}
export interface TocPart {
  title: string; // "Part I: Foundations of Ecological Stability & Equilibrium"
  pages?: string; // "pp. 1–98"
  chapters: TocEntry[];
}

export interface Book {
  id: ID;
  title: string;
  subtitle?: string;
  contributors: Contributor[];
  publisher: string;
  publicationYear: number;
  publishedLabel?: string; // "Published October 2023"
  edition?: string; // "3rd Revised Edition"
  series?: string;
  isbn?: string;
  doi?: string;
  callNumber: string;
  language: string;
  pages?: number;
  resourceType: ResourceType;
  formatLabel?: string; // "Hardcover 3rd Imp.", "E-Book Monograph"
  subjects: string[];
  abstract?: string;
  tableOfContents?: TocPart[];
  coverUrl?: string; // remote image; UI falls back to generated cover
  coverTone?: "forest" | "sage" | "parchment" | "charcoal"; // generated-cover palette
  peerReviewed?: boolean;
  isNewRelease?: boolean;
  accessionDate?: ISODate; // used by New Arrivals ("accession date")
  arrivalBadge?: string; // New Arrivals tile flag: "NEW", "JUST IN", "E-EDITION", "ARCHIVAL", "FACULTY AUTHOR"
  discipline?: string; // New Arrivals discipline facet
  availabilityStatus: AvailabilityStatus;
  availabilityLabel: string; // "Available on Shelf (2 copies)"
  availabilityNote?: string; // long-form circulation note
  holdsInQueue?: number;
  copies: Copy[];
  relevanceScore?: number; // 0-100, recommendations
  relatedIds?: ID[];
  metrics?: { citations: number; reads: string; fieldIndex: string };
  /** Italic descriptive line under the title on the resource-detail page. */
  tagline?: string;
  /** Optional catalog-result-card presentation overrides (falls back to values derived from availability). */
  catalogCard?: CatalogCardMeta;
  /** Optional extra record-detail fields for the resource-detail page. */
  detail?: BookDetail;
}

export interface CatalogCardAction {
  kind: "hold" | "cite" | "list" | "save" | "inert";
  label: string;
  icon?: string;
  placedLabel?: string; // hold: label once the hold exists
  tone?: "primary" | "tertiary"; // primary (first) button colour
}

export interface CatalogCardMeta {
  statusIcon?: string; // glyph instead of the status dot
  statusLabel?: string; // overrides availabilityLabel in the chip
  tags: { label: string; tone: "plain" | "muted" | "accent" | "secondary" }[];
  cover: {
    kicker: string;
    title: string;
    byline: string;
    foot: string;
    tone: "primary" | "secondary" | "tertiary" | "primary-container" | "secondary-container";
  };
  coverRef: string; // "LC: QH541 .E28" | "DOI: 10.4159/harvard"
  note: { icon: string; title: string; body: string; sub?: string };
  actions: CatalogCardAction[];
  ghost?: { label: string; icon: string; toDetail?: boolean };
}

export interface BookDetail {
  copiesBadge?: string; // "2 Physical Copies Available"
  partnerBadge?: string; // "Central Library Open Access Partner"
  shelfLine?: string; // classification callout location
  fullTextLabel?: string; // "Read Online (Full-Text PDF)"
  fullTextTag?: string; // "EZProxy"
  archivalCondition?: string;
  binding?: string;
  doiRegistry?: string;
  lcc?: string;
  lccNote?: string;
  dewey?: string;
  deweyNote?: string;
  extent?: string;
  extentNote?: string;
  seriesTitle?: string;
  seriesNote?: string;
  rightsTitle?: string;
  rightsNote?: string;
}

/* ───────────── Circulation ───────────── */

export interface Loan {
  id: ID;
  userId: ID;
  bookId: ID;
  barcode: string;
  branchName: string;
  location?: string;
  checkedOutOn: ISODate;
  dueDate: ISODate;
  renewalsUsed: number;
  renewalsMax: number;
  loanKind: "physical" | "digital";
  licenseNote?: string; // "Online E-Book • Institutional License"
  policyNote?: string; // "Standard Loan Term", "Auto-grace eligible"
  notesCount?: number;
  studyGuideUrl?: string;
  /** Title as printed on the full ledger card (defaults to the book title) */
  ledgerTitle?: string;
  /** Title as printed on compact Home rows (defaults to the book title) */
  compactTitle?: string;
}

export type HoldStatus = "ready" | "queued" | "in_transit";

export interface Hold {
  id: ID;
  userId: ID;
  bookId: ID;
  status: HoldStatus;
  placedOn: ISODate;
  queuePosition: number;
  queueLength: number;
  pickupLocation: string;
  pickupDetail?: string; // "Main Floor, West Wing"
  heldUntil?: ISODate;
  pickupCode?: string;
}

/* ───────────── Reading lists ───────────── */

export type ListVisibility = "private" | "cohort" | "public";

export interface ReadingListItem {
  bookId: ID;
  position: number;
  week?: string; // "WEEK 02"
  note?: string; // scholar note
  addedOn: ISODate;
  tag?: string; // "Assigned • Core"
}

export interface ReadingList {
  id: ID;
  ownerId: ID;
  title: string;
  subtitle?: string;
  category: string; // "Course Syllabus • 2024–25"
  categoryKind: "course" | "grant" | "study" | "archive" | "shared";
  description: string;
  tags: string[];
  visibility: ListVisibility;
  items: ReadingListItem[];
  itemCount: number; // total works in the folio (may exceed items[].length)
  pdfCount?: number;
  doiCount?: number;
  collaborators: { name: string; role: string; initials: string; status?: string }[];
  enrolledCount?: number;
  membersLabel?: string; // "18 Enrolled • Thorne (Co-instr.)"
  updatedAt: ISODate | string;
  updatedLabel: string; // "Edited 2h ago"
  settings?: { autoSync: boolean; notifyStudents: boolean };
  /** short name used in cross-references ("Foundational Texts") */
  shortTitle?: string;
  /** labels for the little cover spines on the index card */
  spines?: string[];
  /** overrides the derived "8 PDFs • 14 DOIs indexed" line */
  statsLabel?: string;
  /** Material Symbols glyph next to membersLabel */
  membersIcon?: string;
}

/* ───────────── Fines ───────────── */

export type FineStatus = "unpaid" | "waived" | "paid";

export interface Fine {
  id: ID;
  userId: ID;
  bookId?: ID;
  title: string;
  detail?: string;
  reasonKind: "overdue_recall" | "processing_fee" | "binding_assessment" | "lost_item" | "digital_fee";
  reasonLabel: string;
  reasonNote: string;
  barcode?: string;
  assessedOn: ISODate;
  daysOverdue?: number;
  accrualLabel: string;
  amount: number;
  originalAmount?: number;
  /** Small mono reference chips under the title (call no., barcode, doc id…). */
  catalogRefs?: string[];
  /** Inert row action links (first is the primary link). */
  actions?: string[];
  status: FineStatus;
}

export interface Payment {
  id: ID;
  userId: ID;
  transactionRef: string; // "TXN-98214"
  receiptRef: string; // "RC-4491"
  title: string;
  paidOn: ISODate;
  method: string;
  desk: string;
  amount: number;
  taxExempt: boolean;
  icon?: string;
}

export interface FineSummary {
  outstanding: number;
  settledTotal: number;
  currency: "USD";
  standing: "good" | "restricted";
  borrowLimit: number;
}

/* ───────────── History ───────────── */

export type HistoryFormat = "physical" | "digital" | "ill";

export interface HistoryEntry {
  id: ID;
  userId: ID;
  bookId: ID;
  format: HistoryFormat;
  formatLabel: string; // "PHYSICAL MONOGRAPH"
  borrowedOn: ISODate;
  returnedOn: ISODate;
  loanDays: number;
  branchNote: string;
  barcode?: string;
  imprint?: string; // "Oxford University Press, 2013" — shown after the authors; falls back to publisher
  sourceNote?: string; // third metadata chip, e.g. "Bodleian Libraries / Oxford Accession"
  noteIcon?: string; // Material Symbols glyph beside branchNote
}

export interface ReadingSummary {
  year: number;
  worksRead: number;
  deltaVsPrevious: number;
  physical: number;
  digital: number;
  avgDaysLoaned: number;
  benchmark: { target: number; current: number };
  subjects: { tag: string; count: number }[];
}

/* ───────────── Notifications ───────────── */

export type NotificationCategory = "circulation" | "acquisitions";
export type NotificationKind = "hold_ready" | "loan_due" | "new_arrival" | "overdue" | "fee";

export interface NotificationAction {
  id: string;
  label: string;
  icon?: string;
  variant: "primary" | "soft" | "secondary" | "ghost";
  /** renew = renew the linked loan; link = navigate to href; save = toggle save on linked book; none = inert */
  type: "renew" | "link" | "save" | "none";
  href?: string;
}

export interface AppNotification {
  id: ID;
  userId: ID;
  kind: NotificationKind;
  category: NotificationCategory;
  group: "today" | "earlier";
  title: string;
  badge?: string; // "Action Needed"
  ageLabel: string; // "25m ago"
  read: boolean;
  icon: string;
  iconTone: "green" | "amber" | "sage" | "red" | "neutral";
  bookId?: ID;
  loanId?: ID;
  itemTitle?: string; // rendered in italic serif inside the message
  bodyBefore?: string;
  bodyAfter?: string;
  card?: { icon: string; title: string; subtitle: string };
  meta?: { text: string; highlight?: boolean }[];
  actions: NotificationAction[];
}

/* ---------- Digital Resources ---------- */
export type DigitalResourceType = "ebooks" | "databases" | "theses" | "datasets";

export interface DigitalResource {
  id: ID;
  type: DigitalResourceType;
  vendor: string; // "ITHAKA Press • Database"
  collection: string; // "Core Collection"
  collectionTone: "primary" | "secondary";
  title: string;
  description: string;
  access: "sso" | "open";
  scope?: string; // "Campus & Remote"
  subjects: string[];
  stats: { icon: string; label: string }[];
  highlight: string;
  footerLink: { label: string; icon: string };
  peerReviewed: boolean;
  apiAccess: boolean;
  addedOn: string; // ISO date, used for "Recent" sort
}

export interface DigitalResourceTab {
  type: DigitalResourceType;
  label: string;
  icon: string;
  countLabel: string; // "412 Active"
  total: number;
  noun: string; // "databases"
}

/* ---------- Journals & Serials ---------- */
export interface Serial {
  id: ID;
  title: string;
  publisher: string;
  /** Plain text after the publisher name, e.g. " on behalf of the " */
  publisherTail?: string;
  /** Italic text after publisherTail, e.g. "British Ecological Society" */
  publisherTailItalic?: string;
  accessLabel: string;
  accessTone: "green" | "neutral";
  badge: string;
  impactFactor: number;
  sjrLabel: string;
  issn: string;
  eIssn: string;
  /** Extra ledger facts shown after ISSN / E-ISSN */
  facts: { label: string; value: string }[];
  holdingsLabel: string;
  holdings: string;
  /** First year of holdings (archival depth sort) */
  holdingsFrom: number;
  /** Primary discipline used by the subject filter */
  category: string;
  taxonomy: string[];
  /** Shelf location; when set the card offers "Check Shelf" */
  stacks?: string;
  peerReviewed: boolean;
  fullText: boolean;
  openAccess: boolean;
  print: boolean;
  indexed: boolean;
}

export interface RecentSerial {
  serialId: ID;
  viewedLabel: string;
}

/* ───────────── Librarian cataloging (Add Resource) ───────────── */

export type CatalogProviderId = "open-library" | "google-books" | "hardcover";

/** Editable bibliographic fields of the cataloging form. */
export interface CatalogRecord {
  title: string;
  /** Semicolon-separated display names */
  authors: string;
  year: string;
  publisher: string;
  edition: string;
  callNumber: string;
  ddc: string;
  subjects: string[];
  abstract: string;
}

export interface CatalogProviderMatch {
  id: CatalogProviderId;
  name: string;
  confidence: number;
  badge: string;
  coverUrl?: string;
  coverTone?: "forest" | "sage" | "parchment" | "charcoal";
  /** Display strings on the match card */
  card: { title: string; authors: string; publisher: string; yearLabel: string };
  /** Field values applied when this match is chosen as base schema */
  record: CatalogRecord;
  /** Labels for the conflict-picker chips */
  pickers: { authors: string; year: string };
}

export interface CatalogingDraft {
  identifier: string;
  defaultProviderId: CatalogProviderId;
  providers: CatalogProviderMatch[];
}

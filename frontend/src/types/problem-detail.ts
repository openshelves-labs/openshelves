/**
 * Error response body returned by the backend for every non-2xx response,
 * served as `application/problem+json` (RFC 9457 – Problem Details for HTTP APIs).
 *
 * Mirrors `com.openshelves.common.model.ApiErrorResponse`.
 * The backend omits empty members, so `detail` and `instance` may be absent.
 */
export interface ProblemDetail {

  // ---------------------------------------------------------------------------
  // Standard members (RFC 9457)
  // ---------------------------------------------------------------------------

  /** Short, human-readable summary; defaults to the HTTP reason phrase (e.g. "Unauthorized"). */
  title: string;

  /** HTTP status code of this response. */
  status: number;

  /** Human-readable explanation of this specific occurrence. Safe to show to users. */
  detail?: string;

  /** Request path the error relates to (e.g. "/api/v1/me"). */
  instance?: string;


  // ---------------------------------------------------------------------------
  // Extension members
  // ---------------------------------------------------------------------------

  /**
   * Stable, machine-readable error code to branch on.
   * Defaults to the HTTP status name (e.g. "UNAUTHORIZED"); see {@link ProblemCode}.
   */
  code: ProblemCode;

  /** Time the error was created, as an ISO-8601 UTC string. */
  timestamp: string;

  /** Additional members the backend may add via `withProperty(...)`. */
  [extension: string]: unknown;
}


/**
 * Error codes the frontend explicitly handles.
 *
 * `(string & {})` keeps editor autocomplete for the known codes while still
 * accepting any other value, since every HTTP status name is also a valid code.
 */
export type ProblemCode =
  // Authentication (401)
  | "INVALID_CREDENTIALS"
  | "ACCOUNT_LOCKED"
  | "ACCOUNT_DISABLED"
  | "PASSWORD_EXPIRED"

  // Status-based defaults
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "BAD_REQUEST"
  | "INTERNAL_SERVER_ERROR"
  | "BAD_GATEWAY"

  | (string & {});

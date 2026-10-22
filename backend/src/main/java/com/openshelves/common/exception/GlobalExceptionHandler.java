package com.openshelves.common.exception;

import com.openshelves.common.model.ApiErrorResponse;
import com.openshelves.security.filter.BearerTokenFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.function.Predicate;

/// Global exception handler for mapping application exceptions to HTTP responses.
///
/// Annotated with `@RestControllerAdvice` so it applies across all controllers
/// in the application without requiring per-controller error handling.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /*
     * NOTE: Convention for error responses
     *
     * - Controllers return only success responses (2xx). For any error, throw an
     *   exception instead of returning an error ResponseEntity directly.
     *
     * - Each exception type is mapped to its HTTP status here, once. This keeps the
     *   problem+json body and default headers (e.g. WWW-Authenticate) consistent.
     *
     * - Add a dedicated @ExceptionHandler for every new exception type; anything
     *   unmapped falls through to the catch-all and is returned as a 500.
     *
     * - Spring MVC exceptions (400, 405, 415, ...) are already mapped by the
     *   handleExceptionInternal override; don't add handlers for them.
     */

    /// Handles [ThirdPartyClientException] thrown during external API calls.
    ///
    /// @return a 502 Bad Gateway response with a generic user-facing error message
    @ExceptionHandler(ThirdPartyClientException.class)
    public ResponseEntity<ApiErrorResponse> handleThirdPartyClientException(ThirdPartyClientException e, HttpServletRequest req) {
        log.error("Third party client exception", e);

        String detail = "Unable to process your request due to an external service error. Please try again later.";
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, null, detail, req.getRequestURI(), null);
    }

    /// Handles [AuthenticationException] thrown by services, e.g. bad credentials on login.
    ///
    /// @return a 401 Unauthorized response with the exception's message and a code per failure type
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException e, HttpServletRequest req) {
        log.debug("Authentication failed: {}", e.getMessage());

        String code = switch (e) {
            case BadCredentialsException _ -> "INVALID_CREDENTIALS";
            case LockedException _ -> "ACCOUNT_LOCKED";
            case DisabledException _ -> "ACCOUNT_DISABLED";
            case CredentialsExpiredException _ -> "PASSWORD_EXPIRED";
            default -> null;    // Falls back to UNAUTHORIZED
        };

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, code, e.getMessage(), req.getRequestURI(), null);
    }

    /// Handles [AccessDeniedException] thrown by method security, e.g. `@PreAuthorize`.
    ///
    /// @return a 403 Forbidden response with a generic user-facing error message
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest req) {
        log.debug("Access denied: {}", e.getMessage());

        String detail = "You do not have permission to access this resource.";
        return buildErrorResponse(HttpStatus.FORBIDDEN, null, detail, req.getRequestURI(), null);
    }

    /// Catch-all for any exception without a dedicated handler.
    ///
    /// @return a 500 Internal Server Error response with a generic user-facing error message
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception", e);

        String detail = "An unexpected error occurred. Please try again later.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, detail, req.getRequestURI(), null);
    }

    /// Renders Spring MVC exceptions as [ApiErrorResponse], keeping Spring's status and headers.
    ///
    /// @return the error response with Spring's status (e.g. 400, 405, 415)
    @Override
    @SuppressWarnings("JvmTaintAnalysis")
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        log.debug("Spring MVC exception", ex);

        // Most Spring MVC exceptions pass a null body and expect it to be resolved here
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }

        String detail = body instanceof ProblemDetail pd ? pd.getDetail() : null;
        String path = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;

        ResponseEntity<ApiErrorResponse> response = buildErrorResponse(statusCode, null, detail, path, headers);
        return new ResponseEntity<>(response.getBody(), response.getHeaders(), response.getStatusCode());
    }

    /// Replaces Spring's "No static resource" message with an API-friendly one.
    @Override
    protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(@NonNull NoResourceFoundException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "The requested resource was not found.");
        return handleExceptionInternal(ex, body, headers, status, request);
    }


    // -----------------------------------------------------------------------
    // Static utility methods for building error responses
    // -----------------------------------------------------------------------

    /// Builds the standard error response used across the application.
    ///
    /// The body is an [ApiErrorResponse] serialized as `application/problem+json`.
    /// Default headers required for the given status are applied if not already
    /// present on the response or in `headers` (see [#withDefaultHeaders]).
    ///
    /// @param status  the HTTP status of the response
    /// @param code    the machine-readable error code; may be `null` to use the default for the status
    /// @param detail  the client-facing explanation of this occurrence
    /// @param path    the request path the error relates to; may be `null`
    /// @param headers additional response headers; may be `null`
    ///
    /// @return the error response
    @SuppressWarnings("JvmTaintAnalysis")
    static ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatusCode status, String code, String detail, String path, HttpHeaders headers) {
        HttpHeaders responseHeaders = withDefaultHeaders(status, headers);

        ApiErrorResponse body = ApiErrorResponse.forStatus(status.value())
            .withDetail(detail)
            .withInstance(createInstanceUri(path));

        // Keep the status-based default code unless the caller provides one
        if (code != null) {
            body.withCode(code);
        }

        return ResponseEntity.status(status)
            .headers(responseHeaders)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    /// Returns a copy of `headers` with the defaults required for the given status added.
    ///
    /// A default is added only if the header is absent both on the current
    /// [HttpServletResponse] (e.g. set before `sendError`) and in `headers`,
    /// so caller-provided values always take precedence and no duplicates are produced.
    /// The caller's `headers` are never modified.
    ///
    /// Outside a servlet request thread, only the copy of `headers` is returned.
    ///
    /// @param status  the HTTP status of the response
    /// @param headers caller-provided headers; may be `null`
    ///
    /// @return the headers to send with the response; never `null`
    private static HttpHeaders withDefaultHeaders(HttpStatusCode status, HttpHeaders headers) {
        HttpHeaders result = new HttpHeaders();
        if (headers != null) result.addAll(headers);

        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return result;
        }

        HttpServletRequest req = attrs.getRequest();
        HttpServletResponse res = attrs.getResponse();

        // Already set on the response (e.g. before sendError) or by the caller
        Predicate<String> containsHeader = headerName -> result.containsHeader(headerName)
                || (res != null && res.containsHeader(headerName));

        // WWW-Authenticate challenge (RFC 6750 §3.1)
        if (!containsHeader.test(HttpHeaders.WWW_AUTHENTICATE)) {
            boolean hadToken = BearerTokenFilter.extractBearerToken(req).isPresent();

            // 401 Unauthorized: Token missing or invalid
            if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                result.set(HttpHeaders.WWW_AUTHENTICATE, hadToken ? "Bearer error=\"invalid_token\"" : "Bearer");
            }

            // 403 Forbidden: Token valid but lacks the required privileges
            if (status.isSameCodeAs(HttpStatus.FORBIDDEN) && hadToken) {
                result.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"");
            }
        }

        return result;
    }

    /// Creates a URI from the given path string, or returns `null` if the path is `null`.
    private static URI createInstanceUri(String path) {
        return path != null ? URI.create(path) : null;
    }
}

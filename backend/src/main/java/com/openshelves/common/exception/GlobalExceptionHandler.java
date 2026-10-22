package com.openshelves.common.exception;

import com.openshelves.common.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.function.Predicate;

/// Global exception handler for mapping application exceptions to HTTP responses.
///
/// Annotated with `@RestControllerAdvice` so it applies across all controllers
/// in the application without requiring per-controller error handling.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
     */

    /// Handles [ThirdPartyClientException] thrown during external API calls.
    ///
    /// @param e the exception
    /// @return a 502 Bad Gateway response with a generic user-facing error message
    @ExceptionHandler(ThirdPartyClientException.class)
    public ResponseEntity<ApiErrorResponse> handleThirdPartyClientException(ThirdPartyClientException e, HttpServletRequest req) {
        log.error("Third party client exception", e);

        String detail = "Unable to process your request due to an external service error. Please try again later.";
        return handleExceptionInternal(HttpStatus.BAD_GATEWAY, detail, req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception", e);

        String detail = "An unexpected error occurred. Please try again later.";
        return handleExceptionInternal(HttpStatus.INTERNAL_SERVER_ERROR, detail, req.getRequestURI(), null);
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
    /// @param detail  the client-facing explanation of this occurrence
    /// @param path    the request path the error relates to; may be `null`
    /// @param headers additional response headers; may be `null`
    ///
    /// @return the error response
    @SuppressWarnings("JvmTaintAnalysis")
    static ResponseEntity<ApiErrorResponse> handleExceptionInternal(HttpStatusCode status, String detail, String path, HttpHeaders headers) {
        HttpHeaders responseHeaders = withDefaultHeaders(status, headers);

        ApiErrorResponse body = ApiErrorResponse.forStatus(status.value())
            .withDetail(detail)
            .withInstance(createInstanceUri(path));

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
            String authToken = req.getHeader(HttpHeaders.AUTHORIZATION);
            boolean hadToken = StringUtils.startsWithIgnoreCase(authToken, "Bearer ");

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

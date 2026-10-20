package com.openshelves.common.exception;

import com.openshelves.common.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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
    /// present on the response or in `headers` (see [#applyDefaultHeaders]).
    ///
    /// @param status  the HTTP status of the response
    /// @param detail  the client-facing explanation of this occurrence
    /// @param path    the request path the error relates to; may be `null`
    /// @param headers additional response headers; may be `null`
    /// @return the error response
    @SuppressWarnings("JvmTaintAnalysis")
    static ResponseEntity<ApiErrorResponse> handleExceptionInternal(HttpStatusCode status, String detail, String path, HttpHeaders headers) {
        applyDefaultHeaders(status, headers);

        ApiErrorResponse body = ApiErrorResponse.forStatus(status.value())
            .withDetail(detail)
            .withInstance(createInstanceUri(path));

        return ResponseEntity.status(status)
            .headers(headers)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    /// Applies default headers to the current response when the status requires them.
    ///
    /// A default is written only if the header is absent both on the current
    /// [HttpServletResponse] (e.g. set before `sendError`) and in `headers`,
    /// so caller-provided values always take precedence and no duplicates are produced.
    ///
    /// Does nothing outside a servlet request thread.
    ///
    /// @param status  the HTTP status of the response
    /// @param headers caller-provided headers to check against; may be `null`
    private static void applyDefaultHeaders(HttpStatusCode status, HttpHeaders headers) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }

        HttpServletRequest req = attrs.getRequest();
        HttpServletResponse res = attrs.getResponse();
        if (res == null) return;

        Predicate<String> containsHeader = headerName -> res.containsHeader(headerName)
                || (headers != null && headers.containsHeader(headerName));


        // For 401 Unauthorized responses, ensure the WWW-Authenticate header is present.
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) && !containsHeader.test(HttpHeaders.WWW_AUTHENTICATE)) {
            boolean hadToken = req.getHeader(HttpHeaders.AUTHORIZATION) != null;
            res.setHeader(HttpHeaders.WWW_AUTHENTICATE, hadToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        }
    }

    /// Creates a URI from the given path string, or returns `null` if the path is `null`.
    private static URI createInstanceUri(String path) {
        return path != null ? URI.create(path) : null;
    }
}

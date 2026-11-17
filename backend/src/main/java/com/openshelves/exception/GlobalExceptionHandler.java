package com.openshelves.exception;

import com.openshelves.shared.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.function.Predicate;

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

    // -----------------------------------------------------------------------
    // Exception handlers for application-specific exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception", e);

        String detail = "An unexpected error occurred. Please try again later.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, detail, req.getRequestURI(), null);
    }


    // -----------------------------------------------------------------------
    // Spring MVC exception handling overrides
    // -----------------------------------------------------------------------

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

    @Override
    protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(@NonNull NoResourceFoundException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "The requested resource was not found.");
        return handleExceptionInternal(ex, body, headers, status, request);
    }


    // -----------------------------------------------------------------------
    // Static utility methods for building error responses
    // -----------------------------------------------------------------------

    protected static ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatusCode status, String code, String detail, String path, HttpHeaders headers) {
        HttpHeaders responseHeaders = withDefaultHeaders(status, headers);

        ApiErrorResponse body = ApiErrorResponse.forStatus(status.value())
            .withDetail(detail)
            .withInstance(toInstanceUri(path));

        // Keep the status-based default code unless the caller provides one
        if (code != null) {
            body.withCode(code);
        }

        // Return the response entity with the problem+json content type
        return ResponseEntity.status(status)
            .headers(responseHeaders)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

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

    private static URI toInstanceUri(String path) {
        return path != null ? URI.create(path) : null;
    }
}

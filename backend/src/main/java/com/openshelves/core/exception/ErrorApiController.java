package com.openshelves.core.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.openshelves.core.exception.GlobalExceptionHandler.buildErrorResponse;

/**
 * Controller to handle errors that occur outside the normal request handling flow,
 * such as 404 Not Found or 500 Internal Server Error.
 */
@Slf4j
@RestController
public class ErrorApiController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> handleError(HttpServletRequest req) {
        // Retrieve error attributes from the request
        Integer code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Throwable ex = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Determine the HTTP status code and error detail
        HttpStatusCode status = HttpStatusCode.valueOf(code != null ? code : 500);
        String detail = (msg != null && !msg.toString().isEmpty() && !status.is5xxServerError())
            ? msg.toString()
            : null;     // Don't expose internal error messages for 5xx errors

        if (status.is5xxServerError()) {
            log.error("Error occurred at path: {} with status: {} and message: {}", path, status.value(), msg, ex);
        }

        // Build and return the error response
        return buildErrorResponse(status, null, detail, path != null ? path.toString() : null, null);
    }
}

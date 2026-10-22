package com.openshelves.common.exception;

import com.openshelves.common.model.ApiErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.openshelves.common.exception.GlobalExceptionHandler.handleExceptionInternal;

@Slf4j
@RestController
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> handleError(HttpServletRequest req) {
        Integer code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Throwable ex = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        HttpStatusCode status = HttpStatusCode.valueOf(code != null ? code : 500);
        String detail = (msg != null && !msg.toString().isEmpty() && !status.is5xxServerError())
            ? msg.toString()
            : null;     // Don't expose internal error messages for 5xx errors

        if (status.is5xxServerError()) {
            log.error("Error occurred at path: {} with status: {} and message: {}", path, status.value(), msg, ex);
        }

        return handleExceptionInternal(status, null, detail, path != null ? path.toString() : null, null);
    }
}

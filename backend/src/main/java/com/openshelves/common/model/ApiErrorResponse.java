package com.openshelves.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@ToString
@JsonInclude(Include.NON_EMPTY)
public class ApiErrorResponse {

    private String title;
    private String detail;
    private final int status;
    private URI instance;
    private Instant timestamp;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    private ApiErrorResponse(int status) {
        this.status = status;
        this.timestamp = Instant.now();

        HttpStatus httpStatus = HttpStatus.resolve(status);
        this.title = httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
    }

    public static ApiErrorResponse forStatus(int status) {
        return new ApiErrorResponse(status);
    }

    public ApiErrorResponse withTitle(String title) {
        this.title = title;
        return this;
    }

    public ApiErrorResponse withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public ApiErrorResponse withInstance(URI instance) {
        this.instance = instance;
        return this;
    }

    public ApiErrorResponse withTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ApiErrorResponse withProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }
}

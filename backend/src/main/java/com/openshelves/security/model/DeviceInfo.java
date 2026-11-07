package com.openshelves.security.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

/// The device a login comes from, as recorded on its session.
@Value
@Builder(setterPrefix = "with")
public class DeviceInfo {

    /// The client IP address.
    String ipAddress;

    /// The raw `User-Agent` header.
    String userAgent;

    /// A readable name derived from the user agent, e.g. "Chrome on macOS"; `null` if unrecognised.
    String deviceName;

    /// Reads the device details from the login request.
    ///
    /// The IP is the request's remote address. Behind a proxy, it reflects `X-Forwarded-For`
    /// only when `server.forward-headers-strategy` is configured, so the header can't be spoofed otherwise.
    public static DeviceInfo from(HttpServletRequest request) {
        String userAgent = StringUtils.trimToNull(request.getHeader(HttpHeaders.USER_AGENT));

        return DeviceInfo.builder()
            .withIpAddress(request.getRemoteAddr())
            .withUserAgent(userAgent)
            .withDeviceName(describe(userAgent))
            .build();
    }


    // -----------------------------------------------------------------------
    // Device Name
    // -----------------------------------------------------------------------

    /// Builds a readable device name from a user agent, e.g. "Chrome on macOS".
    ///
    /// A best-effort match on well-known tokens, enough for the devices list;
    /// falls back to just the browser or just the OS when only one is recognised.
    ///
    /// @param userAgent the raw `User-Agent` header; may be `null`
    /// @return the device name, or `null` if neither browser nor OS is recognised
    private static String describe(String userAgent) {
        if (userAgent == null) {
            return null;
        }

        String browser = browserOf(userAgent);
        String os = osOf(userAgent);

        if (browser != null && os != null) return browser + " on " + os;
        if (browser != null) return browser;
        return os;
    }

    /// Order matters: Edge and Opera also send `Chrome/`, and Chrome sends `Safari/`.
    private static String browserOf(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) return "Edge";
        if (ua.contains("OPR/")) return "Opera";
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) return "Chrome";
        if (ua.contains("Safari/") && ua.contains("Version/")) return "Safari";
        return null;
    }

    /// Order matters: iOS sends `Mac OS X`, and Android sends `Linux`.
    private static String osOf(String ua) {
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Macintosh") || ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }
}

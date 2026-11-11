package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/// One entry of `GET /api/v1/auth/devices`: a device the user is signed in on.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class DeviceResponse {

    /// ID of the session behind this device.
    Long id;

    /// Whether this is the device making the request.
    boolean current;

    /// Readable name, e.g. "Chrome on macOS".
    String deviceName;

    String userAgent;

    String ipAddress;

    String deviceLocation;

    /// Identity provider the device signed in with; absent for a password sign-in.
    String authProvider;

    Instant createdAt;

    Instant lastActiveAt;

    Instant expiresAt;
}

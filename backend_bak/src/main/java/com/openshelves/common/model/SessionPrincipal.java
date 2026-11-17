package com.openshelves.common.model;

import lombok.Builder;
import lombok.Value;

/// Principal stored in the SecurityContext for a request authenticated by a session token.
@Value
@Builder(setterPrefix = "with")
public class SessionPrincipal {

    /// ID of the authenticated user.
    Long userId;

    /// ID of the session the request was authenticated with.
    Long sessionId;
}

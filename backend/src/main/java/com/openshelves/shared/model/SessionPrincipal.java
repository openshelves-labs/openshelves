package com.openshelves.shared.model;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * Principal stored in the SecurityContext for a request authenticated by a session token.
 */
@Value
@Builder(setterPrefix = "with")
public class SessionPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** ID of the authenticated user. */
    Long userId;

    /** ID of the session the request was authenticated with. */
    Long sessionId;
}

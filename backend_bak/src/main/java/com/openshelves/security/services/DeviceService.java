package com.openshelves.security.services;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.UserSessionEntity;
import com.openshelves.security.model.DeviceResponse;
import com.openshelves.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/// Service for the devices a user is signed in on.
///
/// Every signed-in device is one active session; signing a device out revokes its session.
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final AuthService authService;

    private final UserSessionRepository sessionRepository;


    // -----------------------------------------------------------------------
    // Listing
    // -----------------------------------------------------------------------

    /// Lists the devices the caller is signed in on, most recently active first.
    ///
    /// @param principal the caller
    /// @return the caller's active sessions, with the current one flagged
    public List<DeviceResponse> listDevices(SessionPrincipal principal) {
        return sessionRepository.findActiveByUserId(principal.getUserId(), Instant.now()).stream()
            .map(session -> toDeviceResponse(session, principal.getSessionId()))
            .toList();
    }


    // -----------------------------------------------------------------------
    // Signing Out
    // -----------------------------------------------------------------------

    /// Signs one of the caller's devices out. Signing out the current device is the same as logging out.
    ///
    /// @param principal the caller
    /// @param sessionId the session behind the device
    /// @throws ResponseStatusException `404` if the session isn't the caller's, so other users' IDs aren't revealed
    public void signOutDevice(SessionPrincipal principal, Long sessionId) {

        // Only the caller's own sessions; anything else looks like it doesn't exist
        sessionRepository.findByIdAndUserProfileId(sessionId, principal.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));

        authService.revokeSession(sessionId);
    }

    /// Signs out every device of the caller except the one making the request.
    ///
    /// @param principal the caller
    public void signOutOtherDevices(SessionPrincipal principal) {
        int revoked = sessionRepository.revokeAllExcept(principal.getUserId(), principal.getSessionId(), Instant.now());

        log.debug("Other sessions revoked [userId={}, count={}]", principal.getUserId(), revoked);
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Maps a session to its devices-list entry.
    ///
    /// @param session the session
    /// @param currentSessionId the session making the request
    /// @return the device entry
    private DeviceResponse toDeviceResponse(UserSessionEntity session, Long currentSessionId) {
        return DeviceResponse.builder()
            .withId(session.getId())
            .withCurrent(session.getId().equals(currentSessionId))
            .withDeviceName(session.getDeviceName())
            .withUserAgent(session.getUserAgent())
            .withIpAddress(session.getIpAddress())
            .withDeviceLocation(session.getDeviceLocation())
            .withAuthProvider(session.getAuthProvider())
            .withCreatedAt(session.getCreatedAt())
            .withLastActiveAt(session.getLastActiveAt())
            .withExpiresAt(session.getExpiresAt())
            .build();
    }
}

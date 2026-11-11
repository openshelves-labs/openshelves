package com.openshelves.security.controller;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.SessionResponse;
import com.openshelves.security.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/// REST controller for signing in and out with session tokens.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /// Authenticates the user and returns a session token.
    @PostMapping("/login")
    public LoginResponse loginUser(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.createSession(request, DeviceInfo.from(httpRequest));
    }

    /// Returns the current session and basic details of its user.
    @GetMapping("/session")
    public SessionResponse getSessionDetails(@AuthenticationPrincipal SessionPrincipal principal) {
        return authService.getSessionDetails(principal.getSessionId());
    }

    /// Revokes the current session, if the request has one.
    ///
    /// Public and always `204`: a missing, invalid, expired or already revoked token
    /// leaves nothing to revoke, so it's safe to call any number of times.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutUser(@AuthenticationPrincipal SessionPrincipal principal) {
        // No principal when the token was missing or no longer valid
        if (principal != null) {
            authService.revokeSession(principal.getSessionId());
        }
    }
}

package com.openshelves.security.controller;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.common.util.FrontendUrlValidator;
import com.openshelves.security.model.ChangePasswordRequest;
import com.openshelves.security.model.ResetLinkRequest;
import com.openshelves.security.model.ResetPasswordRequest;
import com.openshelves.security.services.UserPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/// REST controller for changing a password, and setting one through an emailed link.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password")
public class PasswordController {

    private final UserPasswordService userPasswordService;
    private final FrontendUrlValidator frontendUrlValidator;

    /// Changes the signed-in user's password; signs out every other device.
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal SessionPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
        userPasswordService.changePassword(principal, request.getCurrentPassword(), request.getNewPassword());
    }

    /// Emails a link to set a new password. Always `202`, so it can't be used to find accounts.
    @PostMapping("/reset-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestResetLink(@Valid @RequestBody ResetLinkRequest request, HttpServletRequest httpRequest) {
        frontendUrlValidator.check(request.getFrontendResetUrl(), "frontendResetUrl", httpRequest);
        userPasswordService.requestResetLink(request.getIdentifier(), request.getFrontendResetUrl());
    }

    /// Sets a new password with the token from the emailed link; signs out every device.
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userPasswordService.resetPassword(request.getToken(), request.getNewPassword());
    }
}

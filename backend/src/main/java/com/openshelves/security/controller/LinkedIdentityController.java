package com.openshelves.security.controller;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.model.IdentityResponse;
import com.openshelves.security.model.LinkResponse;
import com.openshelves.security.services.LinkedIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// REST controller for a user's sign-in methods: the identity providers they've connected.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/identities")
public class LinkedIdentityController {

    private final LinkedIdentityService linkedIdentityService;

    /// Lists the providers the caller has connected.
    @GetMapping
    public List<IdentityResponse> listIdentities(@AuthenticationPrincipal SessionPrincipal principal) {
        return linkedIdentityService.listIdentities(principal);
    }

    /// Starts connecting a provider; returns where to send the browser.
    @PostMapping("/{provider}")
    public LinkResponse startLink(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable String provider) {
        return linkedIdentityService.startLink(principal, provider);
    }

    /// Disconnects a provider.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        linkedIdentityService.unlink(principal, id);
    }
}

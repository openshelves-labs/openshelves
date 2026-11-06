package com.openshelves.security.controller;

import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.ExchangeRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.ProviderResponse;
import com.openshelves.security.services.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// REST controller for signing in with an SSO identity provider.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SsoController {

    private final SsoService ssoService;

    /// Lists the providers for the sign-in page's dropdown.
    @GetMapping("/providers")
    public List<ProviderResponse> listProviders() {
        return ssoService.listProviders();
    }

    /// Swaps the one-time code from the SSO callback for a session token.
    @PostMapping("/sso/exchange")
    public LoginResponse exchangeCode(@Valid @RequestBody ExchangeRequest request, HttpServletRequest httpRequest) {
        return ssoService.exchangeCode(request.getCode(), DeviceInfo.from(httpRequest));
    }
}

package com.openshelves.security.controller;

import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /// Authenticates the user and returns a session token.
    @PostMapping("/login")
    public LoginResponse loginUser(@RequestBody LoginRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /// Revokes the current session.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutUser() {
        // TODO: implement
    }
}

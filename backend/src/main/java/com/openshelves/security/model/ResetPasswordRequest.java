package com.openshelves.security.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `POST /api/v1/auth/password/reset`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class ResetPasswordRequest {

    /// Token from the emailed link; never logged.
    @NotBlank
    @ToString.Exclude
    String token;

    /// The new password: 12 to 128 characters, any characters; never logged.
    @NotNull
    @Size(min = 12, max = 128)
    @ToString.Exclude
    String newPassword;
}

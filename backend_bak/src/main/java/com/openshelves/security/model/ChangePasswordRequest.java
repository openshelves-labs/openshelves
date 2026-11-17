package com.openshelves.security.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `PUT /api/v1/auth/password`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class ChangePasswordRequest {

    /// The password the user signs in with today; never logged.
    @NotNull
    @ToString.Exclude
    String currentPassword;

    /// The new password: 12 to 128 characters, any characters; never logged.
    @NotNull
    @Size(min = 12, max = 128)
    @ToString.Exclude
    String newPassword;
}

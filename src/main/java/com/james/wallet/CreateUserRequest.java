package com.james.wallet;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code, uppercase")
        String currency
) { }

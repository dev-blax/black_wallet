package com.james.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransferRequest(

        @NotNull(message = "sourceAccountId is required")
        Long sourceAccountId,

        @NotNull(message = "destinationAccountId is required")
        Long destinationAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0001", inclusive = true, message = "amount must be > 0")
        @Digits(integer = 15, fraction = 4, message = "amount must fit NUMERIC(19,4)")
        BigDecimal amount
) { }

package ai.ownfinance.backend.dto;

import ai.ownfinance.backend.entity.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank
        String description,

        @NotNull @DecimalMin(value = "0.01", message = "The amount must be greater than zero")
        BigDecimal amount,

        @NotNull
        TransactionType type,

        @NotNull
        LocalDate transactionDate,

        Integer categoryId
) {}
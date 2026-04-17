package ai.ownfinance.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ChatRequest(
        @NotBlank
        String question,

        @NotNull
        Integer year,

        @NotNull @Min(1) @Max(12)
        Integer month
) {}
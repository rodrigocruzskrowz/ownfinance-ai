package ai.ownfinance.backend.dto;

public record ChatResponse(
        String answer,
        int year,
        int month
) {}
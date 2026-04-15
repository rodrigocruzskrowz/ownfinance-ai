package ai.ownfinance.backend.dto;

import ai.ownfinance.backend.entity.Transaction;
import ai.ownfinance.backend.entity.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        TransactionType type,
        LocalDate transactionDate,
        String categoryName,
        String categoryColor,
        String source,
        OffsetDateTime createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getDescription(),
                t.getAmount(),
                t.getType(),
                t.getTransactionDate(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getCategory() != null ? t.getCategory().getColor() : null,
                t.getSource(),
                t.getCreatedAt()
        );
    }
}
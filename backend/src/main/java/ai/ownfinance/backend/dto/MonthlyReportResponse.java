package ai.ownfinance.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance,
        BigDecimal previousMonthDebit,
        BigDecimal debitVariationPercent,
        String topCategory,
        List<CategorySummary> categoryBreakdown,
        String aiInsights
) {
    public record CategorySummary(
            String categoryName,
            String categoryColor,
            BigDecimal total,
            int transactionCount,
            BigDecimal percentageOfTotal
    ) {}
}
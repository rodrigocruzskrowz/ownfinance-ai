package ai.ownfinance.backend.service;

import ai.ownfinance.backend.dto.MonthlyReportResponse;
import ai.ownfinance.backend.dto.MonthlyReportResponse.CategorySummary;
import ai.ownfinance.backend.entity.Transaction;
import ai.ownfinance.backend.entity.Transaction.TransactionType;
import ai.ownfinance.backend.entity.User;
import ai.ownfinance.backend.repository.TransactionRepository;
import ai.ownfinance.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        User user = getCurrentUser();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        var transactions = transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, start, end);

        // Current month totals
        BigDecimal totalDebit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalCredit.subtract(totalDebit);

        // Last month totals
        LocalDate prevStart = start.minusMonths(1);
        LocalDate prevEnd = prevStart.withDayOfMonth(prevStart.lengthOfMonth());

        var prevTransactions = transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, prevStart, prevEnd);

        BigDecimal previousMonthDebit = prevTransactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Variation percentage
        BigDecimal debitVariation = BigDecimal.ZERO;
        if (previousMonthDebit.compareTo(BigDecimal.ZERO) > 0) {
            debitVariation = totalDebit.subtract(previousMonthDebit)
                    .divide(previousMonthDebit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Categories breakdown
        Map<String, List<Transaction>> byCategory = transactions.stream()
                        .filter(t -> t.getType() == TransactionType.DEBIT && t.getCategory() != null)
                        .collect(Collectors.groupingBy(t -> t.getCategory().getName()));

        List<CategorySummary> categoryBreakdown = byCategory.entrySet().stream()
                .map(entry -> {
                    String catName = entry.getKey();
                    List<Transaction> catTransactions = entry.getValue();
                    BigDecimal catTotal = catTransactions.stream()
                            .map(t -> t.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal percentage = totalDebit.compareTo(BigDecimal.ZERO) > 0
                            ? catTotal.divide(totalDebit, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    String color = catTransactions.getFirst().getCategory().getColor();
                    return new CategorySummary(catName, color, catTotal,catTransactions.size(), percentage);
                })
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();

        String topCategory = categoryBreakdown.isEmpty() ? null : categoryBreakdown.getFirst().categoryName();

        return new MonthlyReportResponse(
                year, month,
                totalDebit, totalCredit, balance,
                previousMonthDebit, debitVariation,
                topCategory, categoryBreakdown,
                null // aiInsights
        );
    }

    private User getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }
}
package ai.ownfinance.backend.repository;

import ai.ownfinance.backend.entity.Transaction;
import ai.ownfinance.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN FETCH t.category
        WHERE t.user = :user
        ORDER BY t.transactionDate DESC
        """)
    Page<Transaction> findByUserWithCategory(
            @Param("user") User user,
            Pageable pageable);

    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user = :user
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findByUserAndYearAndMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.user = :user
            AND t.type = ai.ownfinance.backend.entity.Transaction$TransactionType.DEBIT
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            """)
    BigDecimal sumDebitByUserAndYearAndMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
            SELECT c.name, SUM(t.amount) as total
            FROM Transaction t
            JOIN t.category c
            WHERE t.user = :user
            AND t.type = ai.ownfinance.backend.entity.Transaction$TransactionType.DEBIT
            AND YEAR(t.transactionDate) = :year
            AND MONTH(t.transactionDate) = :month
            GROUP BY c.name
            ORDER BY total DESC
            """)
    List<Object[]> findTopCategoriesByUserAndYearAndMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );
}
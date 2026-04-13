package ai.ownfinance.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "total_debit", nullable = false)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false)
    private BigDecimal totalCredit;

    @Column(name = "top_category")
    private String topCategory;

    @Column(name = "ai_insights", columnDefinition = "TEXT")
    private String aiInsights;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;
}
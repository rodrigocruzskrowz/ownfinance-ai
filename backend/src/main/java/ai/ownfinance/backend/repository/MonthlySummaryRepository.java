package ai.ownfinance.backend.repository;

import ai.ownfinance.backend.entity.MonthlySummary;
import ai.ownfinance.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, UUID> {

    Optional<MonthlySummary> findByUserAndYearAndMonth(User user, int year, int month);

    List<MonthlySummary> findByUserOrderByYearDescMonthDesc(User user);
}
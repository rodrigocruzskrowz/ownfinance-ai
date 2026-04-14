package ai.ownfinance.backend.repository;

import ai.ownfinance.backend.entity.AiChatHistory;
import ai.ownfinance.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, UUID> {

    Page<AiChatHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
package ai.ownfinance.backend.service;

import ai.ownfinance.backend.dto.PageResponse;
import ai.ownfinance.backend.dto.TransactionRequest;
import ai.ownfinance.backend.dto.TransactionResponse;
import ai.ownfinance.backend.entity.Category;
import ai.ownfinance.backend.entity.Transaction;
import ai.ownfinance.backend.entity.User;
import ai.ownfinance.backend.repository.CategoryRepository;
import ai.ownfinance.backend.repository.TransactionRepository;
import ai.ownfinance.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("transactionDate").descending());
        Page<TransactionResponse> result = transactionRepository
                .findByUserWithCategory(user, pageable)
                .map(TransactionResponse::from);
        return PageResponse.from(result);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getCurrentUser();

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .description(request.description())
                .amount(request.amount())
                .type(request.type())
                .transactionDate(request.transactionDate())
                .source("MANUAL")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(transactionRepository.findById(saved.getId()).orElseThrow());
    }

    @Transactional
    public void deleteTransaction(UUID id) {
        User user = getCurrentUser();
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access Denied");
        }

        transactionRepository.delete(transaction);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }
}
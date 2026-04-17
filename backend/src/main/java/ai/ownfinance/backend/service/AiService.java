package ai.ownfinance.backend.service;

import ai.ownfinance.backend.config.AiProperties;
import ai.ownfinance.backend.dto.MonthlyReportResponse;
import ai.ownfinance.backend.entity.AiChatHistory;
import ai.ownfinance.backend.entity.User;
import ai.ownfinance.backend.repository.AiChatHistoryRepository;
import ai.ownfinance.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final ReportService reportService;
    private final AiChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String chat(String question, int year, int month, String locale) {
        User user = getCurrentUser();

        // 1. Get financial context from the report service
        MonthlyReportResponse report = reportService.getMonthlyReport(year, month);

        // 2. Build a structured financial context string to feed into the prompt
        String financialContext = buildFinancialContext(report);

        // 3. Define the system and user prompts for the OpenAI API
        String systemPrompt = """
                És um assistente financeiro pessoal chamado Finas.
                %s
                Baseia as tuas respostas APENAS nos dados financeiros fornecidos.
                Não inventes valores ou categorias que não estejam nos dados.
                Quando relevante, dá sugestões práticas de poupança.
                """.formatted(languageInstruction(locale));

        String userPrompt = """
                Dados financeiros do utilizador para %d/%d:
                
                %s
                
                Pergunta do utilizador: %s
                """.formatted(month, year, financialContext, question);

        // 4. Call OpenRouter API with the defined prompts
        String answer = callOpenRouter(systemPrompt, userPrompt);

        // 5. Persists data in the database
        AiChatHistory history = AiChatHistory.builder()
                .user(user)
                .question(question)
                .answer(answer)
                .context(Map.of(
                        "year", year,
                        "month", month,
                        "locale", locale != null ? locale : "pt-PT",
                        "totalDebit", report.totalDebit().toString(),
                        "topCategory", report.topCategory() != null
                                ? report.topCategory() : "N/A"
                ))
                .build();

        chatHistoryRepository.save(history);

        return answer;
    }

    private String buildFinancialContext(MonthlyReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("- Total gasto (débitos): %.2f€\n".formatted(report.totalDebit()));
        sb.append("- Total recebido (créditos): %.2f€\n".formatted(report.totalCredit()));
        sb.append("- Saldo do mês: %.2f€\n".formatted(report.balance()));

        if (report.previousMonthDebit().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("- Gastos no mês anterior: %.2f€\n"
                    .formatted(report.previousMonthDebit()));
            sb.append("- Variação em relação ao mês anterior: %s%.2f%%\n"
                    .formatted(report.debitVariationPercent().compareTo(
                                    java.math.BigDecimal.ZERO) >= 0 ? "+" : "",
                            report.debitVariationPercent()));
        }

        if (report.topCategory() != null) {
            sb.append("- Categoria onde mais gastou: %s\n"
                    .formatted(report.topCategory()));
        }

        if (!report.categoryBreakdown().isEmpty()) {
            sb.append("- Distribuição de gastos por categoria:\n");
            report.categoryBreakdown().forEach(cat ->
                    sb.append("  * %s: %.2f€ (%s%%) — %d transacções\n"
                            .formatted(cat.categoryName(), cat.total(),
                                    cat.percentageOfTotal(), cat.transactionCount()))
            );
        }

        return sb.toString();
    }

    private String callOpenRouter(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 1000,
                "temperature", 0.7
        );

        int maxRetries = 3;
        int delayMs = 2000;

        for(int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                RestClient restClient = RestClient.builder()
                        .baseUrl(aiProperties.getApiUrl())
                        .build();

                String response = restClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .header("HTTP-Referer", "https://ownfinance.ai")
                        .header("X-Title", "OwnFinance AI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(response);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    throw new IllegalStateException("AI response does not contain choices");
                }
                return choices.get(0).path("message").path("content").asText();

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Rate limit reached, attempt {}/{}, waiting {}ms", attempt, maxRetries, delayMs);
                if (attempt == maxRetries) {
                    throw new RuntimeException("AI Service is temporarily unavailable due to rate limits. " +
                            "Please try again later.");
                }
                try {
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                log.error("Error calling OpenRouter: {}", e.getMessage());
                throw new RuntimeException("Error communicating with AI service");
            }
        }

        throw new RuntimeException("AI service unavailable after " + maxRetries + " attempts");
    }

    private String languageInstruction(String locale) {
        if (locale == null || locale.isBlank()) {
            return "Responde em português europeu de forma clara e concisa.";
        }

        String normalized = locale.toLowerCase().replace('_', '-');
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }

        if (normalized.startsWith("en")) {
            return "Respond in clear and concise English.";
        }
        if (normalized.equals("pt-br")) {
            return "Responda em português do Brasil de forma clara e concisa.";
        }
        if (normalized.startsWith("pt")) {
            return "Responde em português europeu de forma clara e concisa.";
        }
        if (normalized.startsWith("es")) {
            return "Responde en español de forma clara y concisa.";
        }
        if (normalized.startsWith("fr")) {
            return "Réponds en français de façon claire et concise.";
        }
        if (normalized.startsWith("de")) {
            return "Antworte auf klare und prägnante Weise auf Deutsch.";
        }

        return "Responde em português europeu de forma clara e concisa.";
    }

    private User getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }
}


package ai.ownfinance.backend.controller;

import ai.ownfinance.backend.dto.ChatRequest;
import ai.ownfinance.backend.dto.ChatResponse;
import ai.ownfinance.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-powered chat for financial insights")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;

    @Operation(summary = "Chat with the AI assistant",
            description = "Ask financial questions based on your monthly report data. ")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        String answer = aiService.chat(
                request.question(),
                request.year(),
                request.month(),
                acceptLanguage
        );
        return ResponseEntity.ok(new ChatResponse(answer, request.year(), request.month()));
    }
}
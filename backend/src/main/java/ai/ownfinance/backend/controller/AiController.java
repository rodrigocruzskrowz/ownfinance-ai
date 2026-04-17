package ai.ownfinance.backend.controller;

import ai.ownfinance.backend.dto.ChatRequest;
import ai.ownfinance.backend.dto.ChatResponse;
import ai.ownfinance.backend.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

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
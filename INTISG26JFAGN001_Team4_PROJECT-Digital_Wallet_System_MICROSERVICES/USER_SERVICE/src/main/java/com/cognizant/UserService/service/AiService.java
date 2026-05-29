package com.cognizant.UserService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-8b:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateInsight(List<?> transactions) {
        try {
            String prompt = buildPrompt(transactions);

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts    = Map.of("parts", List.of(textPart));
            Map<String, Object> body     = Map.of("contents", List.of(parts));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = GEMINI_URL + geminiApiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("Gemini AI call failed: {}", e.getMessage());
            return "Unable to generate AI insight at this time. Please try again later.";
        }
    }

    private String buildPrompt(List<?> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal finance assistant for a digital wallet app called PayVault.\n");
        sb.append("Analyze the following transaction history and provide a concise 3-4 line insight.\n");
        sb.append("Focus on: spending patterns, most frequent transaction type, largest transaction, and one money-saving tip.\n");
        sb.append("Keep the tone friendly and helpful. Use Indian Rupee (₹) for amounts.\n\n");
        sb.append("Transactions:\n");

        if (transactions == null || transactions.isEmpty()) {
            sb.append("No transactions found.");
        } else {
            for (Object tx : transactions) {
                sb.append("- ").append(tx.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(Map<?, ?> responseBody) {
        try {
            List<?> candidates = (List<?>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "No insight generated. Please try again.";
            }
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content   = (Map<?, ?>) candidate.get("content");
            List<?>   parts     = (List<?>) content.get("parts");
            Map<?, ?> part      = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return "Could not parse AI response.";
        }
    }
}

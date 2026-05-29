package com.cognizant.UserService.controller;

import com.cognizant.UserService.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/insight")
    public ResponseEntity<Map<String, String>> getInsight(@RequestBody Map<String, Object> body) {
        List<?> transactions = (List<?>) body.get("transactions");
        String insight = aiService.generateInsight(transactions);
        return ResponseEntity.ok(Map.of("insight", insight));
    }
}

package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

@RestController
@RequestMapping("/api/deepseek")
public class DeepseekController {
    @Value("${deepseek.api.key}")
    private String apiKey;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        String url = "https://api.deepseek.com/v1/chat/completions";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 支持前端传递 apiKey，优先用前端传递的密钥
        String key = apiKey;
        if (payload.containsKey("apiKey")) {
            Object k = payload.get("apiKey");
            if (k != null && k instanceof String && !((String)k).isEmpty()) {
                key = (String)k;
            }
            payload.remove("apiKey"); // 移除密钥字段，避免传递到 openai
        }
        headers.set("Authorization", "Bearer " + key);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

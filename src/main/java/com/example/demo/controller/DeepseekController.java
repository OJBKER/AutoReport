package com.example.demo.controller;

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
    // 强制设置response_format为json模式（官方要求为对象）
    Map<String, Object> responseFormat = new java.util.HashMap<>();
    responseFormat.put("type", "json_object");
    payload.put("response_format", responseFormat);
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
        // 强制设置temperature为1.5
        payload.put("temperature", 1.5);
        headers.set("Authorization", "Bearer " + key);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            // 只返回message里的json内容
            String messageContent = response;
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode msg = root.get("choices").get(0).get("message");
                    if (msg != null && msg.has("content")) {
                        messageContent = msg.get("content").asText();
                    }
                }
            } catch (Exception parseEx) {
                // 解析失败则返回原始内容
            }
            return ResponseEntity.ok(messageContent);
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细异常堆栈到控制台
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

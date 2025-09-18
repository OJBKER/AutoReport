package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {
    @GetMapping("/api/user/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> result = new HashMap<>();
        if (principal != null) {
            // GitHub 用户名、头像、邮箱等
            result.put("id", principal.getAttribute("id"));
            result.put("login", principal.getAttribute("login"));
            result.put("name", principal.getAttribute("name"));
            result.put("avatar_url", principal.getAttribute("avatar_url"));
            result.put("email", principal.getAttribute("email"));
        }
        return result;
    }
}
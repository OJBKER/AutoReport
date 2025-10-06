package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("oauth2")
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().csrfTokenRepository(csrfTokenRepository())
            .and()
            .authorizeRequests()
                .antMatchers("/", "/login**", "/error", "/webjars/**", "/api/slogin", "/api/user/me", "/api/user/bind-student-info", "/api/user-tasks", "/api/tasks", "/api/tasks/**", "/api/classes", "/api/classes/**", "/api/task-submissions", "/api/task-submissions/**", "/api/debug/**", "/csrf").permitAll()
                .anyRequest().authenticated()
                .and()
            .oauth2Login()
                // 登录成功后跳转到前端开发服务器 Home 页面
                .defaultSuccessUrl("http://localhost:5173/Main", true);//生产环境使用/Main
        return http.build();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repo = new HttpSessionCsrfTokenRepository();
        repo.setHeaderName("X-CSRF-TOKEN");
        return repo;
    }

    @Configuration
    public class CorsConfig {
        @Bean
        public WebMvcConfigurer corsConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addCorsMappings(CorsRegistry registry) {
                    registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .exposedHeaders("X-CSRF-TOKEN");
                }
            };
        }
    }
}

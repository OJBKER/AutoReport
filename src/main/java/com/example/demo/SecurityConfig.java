package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("oauth2")
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/", "/login**", "/error", "/webjars/**").permitAll()
                .anyRequest().authenticated()
                .and()
            .oauth2Login()
                // 登录成功后跳转到前端开发服务器 Home 页面
                .defaultSuccessUrl("http://localhost:5173/about", true);//生产环境使用/about
        return http.build();
    }
}

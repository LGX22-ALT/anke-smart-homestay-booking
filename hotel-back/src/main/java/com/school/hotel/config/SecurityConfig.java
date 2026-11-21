package com.school.hotel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper objectMapper) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler.authenticationEntryPoint((req, res, ex) -> writeError(res,objectMapper,401,"登录状态已失效，请重新登录"))
                        .accessDeniedHandler((req, res, ex) -> writeError(res,objectMapper,403,"当前账号没有访问权限")))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/ai/status", "/api/ai/search", "/api/user/login", "/api/user/register", "/api/user/refresh", "/api/hotels/**", "/api/common/dicts", "/uploads/**", "/doc.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }

    private static void writeError(HttpServletResponse response,ObjectMapper objectMapper,int code,String message) throws java.io.IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(code,message));
    }
}

package com.school.hotel.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokenService;
    private final org.springframework.beans.factory.ObjectProvider<com.school.hotel.mapper.UserMapper> users;
    public JwtAuthenticationFilter(JwtTokenService tokenService, org.springframework.beans.factory.ObjectProvider<com.school.hotel.mapper.UserMapper> users) { this.tokenService = tokenService; this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = tokenService.parse(header.substring(7));
                if ("access".equals(claims.get("type"))) {
                    var user=users.getObject().selectById(Long.valueOf(claims.getSubject()));
                    if(user==null || !Integer.valueOf(1).equals(user.getStatus())) {
                        SecurityContextHolder.clearContext();
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":401,\"message\":\"账号已禁用或不存在\"}");
                        return;
                    }
                    String role = user.getRole();
                    var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request, response);
    }
}

package com.school.hotel.utils;

import com.school.hotel.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {
    private AuthContext() { }
    public static Long userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new BusinessException(401, "请先登录");
        return Long.valueOf(String.valueOf(auth.getPrincipal()));
    }
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }
}

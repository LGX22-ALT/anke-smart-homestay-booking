package com.school.hotel;

import com.school.hotel.config.JwtTokenService;
import com.school.hotel.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HotelApplicationTests {
    @Test void jwtContainsIdentityAndRole() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("hotel-booking-test-secret-must-be-at-least-32-bytes");
        JwtTokenService tokenService = new JwtTokenService(properties);
        String token = tokenService.createAccessToken(3L, "user");
        assertThat(tokenService.parse(token).getSubject()).isEqualTo("3");
        assertThat(tokenService.parse(token).get("role")).isEqualTo("user");
    }
}

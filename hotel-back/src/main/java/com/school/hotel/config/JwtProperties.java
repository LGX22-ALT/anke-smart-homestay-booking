package com.school.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hotel.jwt")
public class JwtProperties {
    private String secret;
    private long accessExpireMinutes = 120;
    private long refreshExpireDays = 14;
}

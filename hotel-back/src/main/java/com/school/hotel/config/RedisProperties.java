package com.school.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hotel.redis")
public class RedisProperties { private boolean required; }

package com.school.hotel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, UploadProperties.class, RedisProperties.class})
public class WebConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;
    public WebConfig(UploadProperties uploadProperties) { this.uploadProperties = uploadProperties; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadProperties.getPath() + "/");
    }
}

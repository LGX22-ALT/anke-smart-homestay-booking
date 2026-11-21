package com.school.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hotel.upload")
public class UploadProperties { private String path = "./uploads"; }

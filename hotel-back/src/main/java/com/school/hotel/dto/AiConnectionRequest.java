package com.school.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AiConnectionRequest {
    @NotBlank @Size(max=80) private String name;
    @Size(max=40) private String provider = "custom";
    @NotBlank @Size(max=500) private String baseUrl;
    @NotBlank @Size(max=100) private String model;
    @Size(max=1000) private String apiKey = "";
    private boolean enabled = true;
    @Min(256) @Max(16384) private int maxTokens = 4096;
    @DecimalMin("0.0") @DecimalMax("2.0") private double temperature = 0.6;
}

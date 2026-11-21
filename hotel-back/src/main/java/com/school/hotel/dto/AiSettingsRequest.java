package com.school.hotel.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiSettingsRequest {
    private boolean enabled;
    @Size(max = 500) private String endpoint = "";
    @Size(max = 100) private String model = "";
    @Size(max = 1000) private String apiKey = "";
    private boolean clearApiKey;
}

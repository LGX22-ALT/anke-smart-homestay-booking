package com.school.hotel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiSearchRequest {
    @NotBlank @Size(max = 1000) private String message;
    @Size(max = 50) private String city = "";
    @Pattern(regexp = "auto|quick|deep") private String mode = "auto";
    @Valid @Size(max = 6) private List<Turn> history = new ArrayList<>();

    @Data
    public static class Turn {
        @NotBlank @Pattern(regexp = "user|assistant") private String role;
        @NotBlank @Size(max = 1000) private String content;
    }
}

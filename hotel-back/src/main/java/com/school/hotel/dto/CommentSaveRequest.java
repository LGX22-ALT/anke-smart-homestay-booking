package com.school.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentSaveRequest {
    @NotNull private Long orderId;
    @NotNull @Min(1) @Max(5) private Integer score;
    @NotBlank private String content;
    private String images;
}

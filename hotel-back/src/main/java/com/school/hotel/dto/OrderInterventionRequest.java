package com.school.hotel.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class OrderInterventionRequest {
    @NotBlank @Pattern(regexp="REFUNDED|CANCELLED|COMPLETED") private String status;
    @Size(max=500) private String remark;
}

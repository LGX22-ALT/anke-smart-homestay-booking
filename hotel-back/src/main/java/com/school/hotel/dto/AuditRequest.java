package com.school.hotel.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class AuditRequest { @NotBlank private String result; private String reason; }

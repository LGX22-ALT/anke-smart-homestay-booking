package com.school.hotel.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
@Data public class HotelSaveRequest { @NotBlank private String name; @NotBlank private String city; private String district; private String address; private String type; private String coverImage; private String gallery; private String description; private String facilities; @Size(max = 4000) private String nearbyInfo; @DecimalMin("0") private BigDecimal minPrice; }

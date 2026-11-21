package com.school.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@lombok.EqualsAndHashCode(callSuper=true)
public class HotelSearchRequest extends PageRequest {
    @Size(max = 50) private String city;
    @Size(max = 50) private String district;
    @Size(max = 30) private String type;
    @DecimalMin("0.00") private BigDecimal maxPrice;
    @Size(max = 50) private String facility;
    @Size(max = 30) private String bed;
    @Min(0) @Max(20) private Integer guests;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate checkIn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate checkOut;
    @Pattern(regexp = "score|priceAsc|priceDesc|reviews") private String sort = "score";
}

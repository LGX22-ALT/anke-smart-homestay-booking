package com.school.hotel.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data public class RoomCalendarUpdateRequest { @NotNull private Long roomTypeId; @NotNull private LocalDate startDate; @NotNull private LocalDate endDate; @NotNull @DecimalMin("0") private BigDecimal price; @NotNull @Min(0) private Integer stock; @NotNull private Boolean available; }

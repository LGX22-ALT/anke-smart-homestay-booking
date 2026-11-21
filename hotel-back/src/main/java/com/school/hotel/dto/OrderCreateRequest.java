package com.school.hotel.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
@Data public class OrderCreateRequest { @NotNull private Long hotelId; @NotNull private Long roomTypeId; @NotNull @FutureOrPresent private LocalDate checkIn; @NotNull @Future private LocalDate checkOut; @Min(1) private Integer guests = 1; @NotBlank private String guestName; @NotBlank @Pattern(regexp="^1\\d{10}$") private String guestPhone; private String guestIdCard; private String remark; private String requestId; }

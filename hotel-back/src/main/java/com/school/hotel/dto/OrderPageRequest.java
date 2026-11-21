package com.school.hotel.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class OrderPageRequest extends PageRequest {
    @Pattern(regexp="PENDING_PAYMENT|PAID|PENDING_CHECKIN|CONFIRMED|CHECKED_IN|COMPLETED|CANCELLED|REFUNDING|REFUNDED")
    private String status;
}

package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_order")
public class Order extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private String orderNo; private Long userId; private Long hotelId; private Long roomTypeId; private LocalDate checkIn; private LocalDate checkOut; private Integer nights; private Integer guests; private BigDecimal totalAmount; private String orderStatus; private String paymentStatus; private String cancelReason; private String remark; }

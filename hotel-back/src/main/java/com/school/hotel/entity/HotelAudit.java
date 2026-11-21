package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_hotel_audit")
public class HotelAudit extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long hotelId; private Long auditorId; private String result; private String reason; }

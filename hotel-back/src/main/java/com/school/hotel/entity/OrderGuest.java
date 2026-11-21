package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_order_guest")
public class OrderGuest extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long orderId; private String name; private String phone; private String idCard; }

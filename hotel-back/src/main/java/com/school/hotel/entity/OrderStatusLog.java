package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_order_status_log")
public class OrderStatusLog extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long orderId; private String fromStatus; private String toStatus; private Long operatorId; private String remark; }

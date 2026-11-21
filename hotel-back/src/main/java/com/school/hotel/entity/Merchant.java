package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_merchant")
public class Merchant extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long userId; private String companyName; private String contactName; private String licenseNo; private String status; }

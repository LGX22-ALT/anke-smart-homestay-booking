package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_banner")
public class Banner extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private String title; private String imageUrl; private String linkUrl; private String position; private Integer sortNo; private Integer status; }

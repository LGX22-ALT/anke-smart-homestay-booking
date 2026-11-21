package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_system_dict")
public class SystemDict extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private String dictType; private String dictLabel; private String dictValue; private Integer sortNo; private Integer status; }

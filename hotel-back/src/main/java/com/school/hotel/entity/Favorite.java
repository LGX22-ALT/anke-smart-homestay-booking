package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_favorite")
public class Favorite extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long userId; private Long hotelId; }

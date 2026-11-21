package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_comment")
public class Comment extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long orderId; private Long hotelId; private Long userId; private Integer score; private String content; private String images; private String merchantReply; private String status; }

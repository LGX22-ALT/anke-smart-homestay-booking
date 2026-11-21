package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_private_message")
public class PrivateMessage extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long userId;
    private Long merchantId;
    private Long senderUserId;
    private String senderRole;
    private String content;
    private Integer isRead;
}

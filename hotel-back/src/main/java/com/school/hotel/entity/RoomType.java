package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_room_type")
public class  RoomType extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long hotelId; private String name; private String image; private Integer area; private String bed; private Integer maxGuests; private java.math.BigDecimal basePrice; private Integer stock; private String facilities; }

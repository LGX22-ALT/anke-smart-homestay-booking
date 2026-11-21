package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_hotel")
public class Hotel extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long merchantId; private String name; private String city; private String district; private String address; private String type; private String coverImage; private String gallery; private String description; private String facilities; private String nearbyInfo; private String status; private java.math.BigDecimal minPrice; private java.math.BigDecimal score; private Integer reviewCount;
    @TableField(exist = false) private java.util.Map<String,Object> merchant;
}

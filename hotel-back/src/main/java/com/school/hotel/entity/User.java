package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_user")
public class User extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private String phone; private String email; @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) private String password; private String name; private String avatar; private String role; private Integer status; @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) private String idCard; }

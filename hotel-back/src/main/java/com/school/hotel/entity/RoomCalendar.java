package com.school.hotel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data @EqualsAndHashCode(callSuper = true) @TableName("tb_room_calendar")
public class RoomCalendar extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long roomTypeId; private LocalDate calendarDate; private java.math.BigDecimal price; private Integer availableStock; private Integer isAvailable; }

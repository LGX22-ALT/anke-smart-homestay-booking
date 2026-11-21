package com.school.hotel.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PageRequest {
    @NotNull @Min(1) private Integer pageNum=1;
    @NotNull @Min(1) @Max(100) private Integer pageSize=10;
    @Size(max=100) private String keyword;
    public <T> Page<T> page(){return new Page<>(pageNum,pageSize);}
}

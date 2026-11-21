package com.school.hotel.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long pages, List<T> list) { return new PageResult<>(total, pages, list); }
}

package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.hotel.common.BusinessException;
import com.school.hotel.dto.HotelSearchRequest;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HotelSearchTests {
    private final HotelMapper hotels = mock(HotelMapper.class);
    private final RoomTypeMapper rooms = mock(RoomTypeMapper.class);
    private final RoomCalendarMapper calendars = mock(RoomCalendarMapper.class);
    private final HotelService service = new HotelService(hotels, rooms, calendars,
            mock(CommentMapper.class), mock(MerchantMapper.class), mock(UserMapper.class));

    private HotelSearchRequest dated() {
        HotelSearchRequest filter = new HotelSearchRequest();
        filter.setCheckIn(LocalDate.now().plusDays(1));
        filter.setCheckOut(LocalDate.now().plusDays(3));
        return filter;
    }
    private RoomType room() {
        RoomType room = new RoomType();
        room.setId(10L); room.setHotelId(1L);
        return room;
    }
    private RoomCalendar day(LocalDate date) {
        RoomCalendar row = new RoomCalendar();
        row.setRoomTypeId(10L); row.setCalendarDate(date);
        row.setIsAvailable(1); row.setAvailableStock(1);
        return row;
    }
    @Test void rejectsMissingOrInvalidDatesBeforeQuerying() {
        HotelSearchRequest filter = dated();
        filter.setCheckOut(null);
        assertThrows(BusinessException.class, () -> service.list(filter));
        filter.setCheckOut(filter.getCheckIn());
        assertThrows(BusinessException.class, () -> service.list(filter));
        filter.setCheckOut(filter.getCheckIn().plusDays(31));
        assertThrows(BusinessException.class, () -> service.list(filter));
        verifyNoInteractions(hotels, rooms, calendars);
    }
    @Test void searchDoesNotMaterializeRoomOrCalendarRows() {
        Page<Hotel> page = new Page<>(1, 10);page.setTotal(1);page.setRecords(List.of(new Hotel()));
        when(hotels.selectPage(any(Page.class),any(Wrapper.class))).thenReturn(page);
        assertEquals(1,service.list(dated()).getTotal());
        verifyNoInteractions(rooms,calendars);
    }
    @Test void validatesPageLimitsAndSortWhitelist() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            HotelSearchRequest filter = new HotelSearchRequest();
            filter.setPageSize(10000);
            filter.setSort("min_price; DROP TABLE tb_hotel");
            assertEquals(2, factory.getValidator().validate(filter).size());
        }
    }
}

package com.school.hotel.service;

import com.school.hotel.common.BusinessException;
import com.school.hotel.entity.Order;
import com.school.hotel.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderTimeoutSchedulerTests {
    @Test void oneCorruptOrderDoesNotBlockOtherExpiredOrders() {
        var mapper=mock(OrderMapper.class);var service=mock(OrderService.class);
        ObjectProvider<OrderMapper> m=mock(ObjectProvider.class);ObjectProvider<OrderService> s=mock(ObjectProvider.class);
        when(m.getObject()).thenReturn(mapper);when(s.getObject()).thenReturn(service);
        Order first=new Order();first.setId(1L);Order second=new Order();second.setId(2L);
        when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(first,second));
        doThrow(new BusinessException("calendar missing")).when(service).cancelExpired(first);
        assertDoesNotThrow(()->new OrderTimeoutScheduler(m,s).cancelExpiredOrders());
        verify(service).cancelExpired(second);
    }
    @Test void expiredOrdersAreProcessedInBatchesWithoutSkippingLaterIds() {
        var mapper=mock(OrderMapper.class);var service=mock(OrderService.class);
        ObjectProvider<OrderMapper> m=mock(ObjectProvider.class);ObjectProvider<OrderService> s=mock(ObjectProvider.class);
        when(m.getObject()).thenReturn(mapper);when(s.getObject()).thenReturn(service);
        var first=java.util.stream.LongStream.rangeClosed(1,200).mapToObj(id->{var o=new Order();o.setId(id);return o;}).toList();
        Order last=new Order();last.setId(201L);
        when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(first,List.of(last));
        new OrderTimeoutScheduler(m,s).cancelExpiredOrders();
        verify(service).cancelExpired(last);verify(service,times(201)).cancelExpired(any());
    }
}

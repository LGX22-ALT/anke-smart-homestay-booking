package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.dto.OrderCreateRequest;
import com.school.hotel.dto.RoomCalendarUpdateRequest;
import com.school.hotel.entity.*;
import com.school.hotel.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RoomInventoryService {
    private final RoomTypeMapper rooms;
    private final RoomCalendarMapper calendars;
    private final OrderMapper orders;
    public RoomInventoryService(RoomTypeMapper rooms, RoomCalendarMapper calendars, OrderMapper orders) {
        this.rooms=rooms; this.calendars=calendars; this.orders=orders;
    }
    private RoomType lock(Long id) {
        RoomType room=rooms.selectOne(new LambdaQueryWrapper<RoomType>().eq(RoomType::getId,id).last("FOR UPDATE"));
        if(room==null) throw new BusinessException(404,"房型不存在");
        return room;
    }
    private RoomCalendar day(Long roomId, LocalDate date) {
        return calendars.selectOne(new LambdaQueryWrapper<RoomCalendar>()
            .eq(RoomCalendar::getRoomTypeId,roomId).eq(RoomCalendar::getCalendarDate,date).last("FOR UPDATE"));
    }
    @Transactional
    public BigDecimal reserve(OrderCreateRequest request) {
        RoomType room=lock(request.getRoomTypeId());
        if(!room.getHotelId().equals(request.getHotelId())) throw new BusinessException("房型不属于该房源");
        if(request.getGuests()==null || request.getGuests()<1 || request.getGuests()>room.getMaxGuests())
            throw new BusinessException("入住人数超过房型容量");
        BigDecimal amount=BigDecimal.ZERO;
        for(LocalDate date=request.getCheckIn(); date.isBefore(request.getCheckOut()); date=date.plusDays(1)) {
            RoomCalendar row=day(room.getId(),date);
            if(row==null || !Integer.valueOf(1).equals(row.getIsAvailable()) || row.getAvailableStock()<1)
                throw new BusinessException("所选日期库存不足");
            row.setAvailableStock(row.getAvailableStock()-1); calendars.updateById(row);
            amount=amount.add(row.getPrice());
        }
        return amount;
    }
    @Transactional
    public void release(Order order) {
        lock(order.getRoomTypeId());
        for(LocalDate date=order.getCheckIn(); date.isBefore(order.getCheckOut()); date=date.plusDays(1)) {
            RoomCalendar row=day(order.getRoomTypeId(),date);
            if(row==null) throw new BusinessException("订单房态缺失，无法恢复库存");
            row.setAvailableStock(row.getAvailableStock()+1);calendars.updateById(row);
        }
    }
    @Transactional
    public void updateCalendar(RoomCalendarUpdateRequest request) {
        RoomType room=lock(request.getRoomTypeId());
        if(request.getStartDate().isAfter(request.getEndDate()) || request.getEndDate().isAfter(request.getStartDate().plusDays(365)))
            throw new BusinessException("房态日期范围无效");
        for(LocalDate date=request.getStartDate(); !date.isAfter(request.getEndDate()); date=date.plusDays(1)) {
            long reserved=orders.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getRoomTypeId,room.getId()).le(Order::getCheckIn,date).gt(Order::getCheckOut,date)
                .notIn(Order::getOrderStatus,"CANCELLED","REFUNDED"));
            if(request.getStock()>Math.max(0,room.getStock()-reserved))
                throw new BusinessException("可售库存不能超过房型总量扣除已预订数量");
            RoomCalendar row=day(request.getRoomTypeId(),date);
            boolean fresh=row==null;
            if(fresh){row=new RoomCalendar();row.setRoomTypeId(request.getRoomTypeId());row.setCalendarDate(date);}
            row.setPrice(request.getPrice());row.setAvailableStock(request.getStock());row.setIsAvailable(request.getAvailable()?1:0);
            if(fresh)calendars.insert(row);else calendars.updateById(row);
        }
    }
}

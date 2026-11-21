package com.school.hotel.vo;

import com.school.hotel.entity.Order;
import com.school.hotel.entity.OrderGuest;
import com.school.hotel.entity.Hotel;
import com.school.hotel.entity.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderView(Long id,String orderNo,Long hotelId,Long roomTypeId,String hotel,String hotelImage,String hotelAddress,
    String roomName,String roomImage,Integer roomArea,String roomBed,Integer roomGuests,String roomFacilities,
    String guestName,String guestPhone,LocalDate checkIn,LocalDate checkOut,Integer nights,Integer guests,
    BigDecimal totalAmount,String orderStatus,String paymentStatus,String cancelReason,String remark,
    LocalDateTime createTime) {
    public static OrderView of(Order o,Hotel hotel,RoomType room,OrderGuest guest){
        return new OrderView(o.getId(),o.getOrderNo(),o.getHotelId(),o.getRoomTypeId(),
            hotel==null?null:hotel.getName(),hotel==null?null:hotel.getCoverImage(),hotel==null?null:hotel.getAddress(),
            room==null?null:room.getName(),room==null?null:room.getImage(),room==null?null:room.getArea(),
            room==null?null:room.getBed(),room==null?null:room.getMaxGuests(),room==null?null:room.getFacilities(),
            guest==null?null:guest.getName(),guest==null?null:guest.getPhone(),o.getCheckIn(),o.getCheckOut(),
            o.getNights(),o.getGuests(),o.getTotalAmount(),o.getOrderStatus(),o.getPaymentStatus(),
            o.getCancelReason(),o.getRemark(),o.getCreateTime());
    }
}

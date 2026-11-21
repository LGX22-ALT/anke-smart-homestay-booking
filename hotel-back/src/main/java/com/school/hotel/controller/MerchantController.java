package com.school.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.common.Result;
import com.school.hotel.dto.*;
import com.school.hotel.entity.RoomCalendar;
import com.school.hotel.mapper.RoomCalendarMapper;
import com.school.hotel.service.HotelService;
import com.school.hotel.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@RestController @RequestMapping("/api/merchant") @PreAuthorize("hasRole('MERCHANT')") public class MerchantController {
    private final MerchantService service; private final HotelService hotelService; private final RoomCalendarMapper calendarMapper; private final com.school.hotel.service.RoomInventoryService inventory; private final com.school.hotel.service.OrderService orders;
    public MerchantController(MerchantService service,HotelService hotelService,RoomCalendarMapper calendarMapper,com.school.hotel.service.RoomInventoryService inventory,com.school.hotel.service.OrderService orders){this.service=service;this.hotelService=hotelService;this.calendarMapper=calendarMapper;this.inventory=inventory;this.orders=orders;}
    @GetMapping("/dashboard") public Result<?> dashboard(){return Result.success(service.dashboard());}
    @GetMapping("/hotels/{id}/rooms") public Result<?> rooms(@PathVariable Long id){return Result.success(service.roomTypes(id));}
    @GetMapping("/hotels") public Result<?> hotels(@RequestParam(required=false) String status,@Valid @ModelAttribute PageRequest request){return Result.success(hotelService.merchantHotels(status,request.getPageNum(),request.getPageSize()));}
    @PostMapping("/hotels") public Result<?> create(@Valid @RequestBody HotelSaveRequest request){return Result.success(hotelService.saveMerchantHotel(request,null));}
    @PutMapping("/hotels/{id}") public Result<?> update(@PathVariable Long id,@Valid @RequestBody HotelSaveRequest request){return Result.success(hotelService.saveMerchantHotel(request,id));}
    @PutMapping("/hotels/{id}/status") public Result<?> status(@PathVariable Long id,@RequestParam String status){return Result.success(hotelService.updateMerchantHotelStatus(id,status));}
    @GetMapping("/room-calendar") public Result<?> calendar(@RequestParam Long roomTypeId,@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate){service.assertOwnsRoomType(roomTypeId);return Result.success(calendarMapper.selectList(new LambdaQueryWrapper<RoomCalendar>().eq(RoomCalendar::getRoomTypeId,roomTypeId).ge(RoomCalendar::getCalendarDate,startDate).le(RoomCalendar::getCalendarDate,endDate).orderByAsc(RoomCalendar::getCalendarDate)));}
    @PutMapping("/room-calendar") public Result<?> updateCalendar(@Valid @RequestBody RoomCalendarUpdateRequest request){service.assertOwnsRoomType(request.getRoomTypeId());inventory.updateCalendar(request);return Result.success("房态已更新");}
    @GetMapping("/orders") public Result<?> orders(@Valid @ModelAttribute OrderPageRequest request){return Result.success(service.orderPage(request));}
    @PutMapping("/orders/{id}/confirm") public Result<?> confirm(@PathVariable Long id){return Result.success(orders.view(service.transitionOrder(id,"PENDING_CHECKIN","商家确认订单")));}
    @PutMapping("/orders/{id}/check-in") public Result<?> checkIn(@PathVariable Long id){return Result.success(orders.view(service.transitionOrder(id,"CHECKED_IN","办理入住")));}
    @PutMapping("/orders/{id}/cancel") public Result<?> cancel(@PathVariable Long id){return Result.success(orders.view(service.transitionOrder(id,"CANCELLED","商家取消订单")));}
    @GetMapping("/reviews") public Result<?> reviews(){return Result.success(service.reviews());}
    @PutMapping("/reviews/{id}/reply") public Result<?> reply(@PathVariable Long id,@Valid @RequestBody ReplyRequest request){return Result.success(service.reply(id,request));}
    @GetMapping("/statistics") public Result<?> statistics(){return Result.success(java.util.Map.of("orders",service.orders(null).size(),"revenue",service.orders(null).stream().map(com.school.hotel.entity.Order::getTotalAmount).filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add)));}
}

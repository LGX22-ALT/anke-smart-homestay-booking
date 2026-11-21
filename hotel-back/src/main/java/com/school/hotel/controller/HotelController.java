package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.HotelSearchRequest;
import jakarta.validation.Valid;
import com.school.hotel.service.HotelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/hotels") public class HotelController {
    private final HotelService service; public HotelController(HotelService service){this.service=service;}
    @GetMapping public Result<?> list(@Valid @ModelAttribute HotelSearchRequest filter){return Result.success(service.list(filter));}
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id){return Result.success(service.detail(id));}
    @GetMapping("/{id}/reviews") public Result<?> reviews(@PathVariable Long id){return Result.success(service.reviews(id));}
    @GetMapping("/{id}/availability") public Result<?> availability(@PathVariable Long id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkIn,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkOut){return Result.success(service.availability(id,checkIn,checkOut));}
}

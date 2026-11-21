package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.OrderCreateRequest;
import com.school.hotel.dto.OrderPageRequest;
import com.school.hotel.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders") public class OrderController {
    private final OrderService service; public OrderController(OrderService service){this.service=service;}
    @PostMapping public Result<?> create(@Valid @RequestBody OrderCreateRequest request){return Result.success(service.view(service.create(request)));}
    @GetMapping public Result<?> list(@Valid @ModelAttribute OrderPageRequest request){return Result.success(service.page(request,null,true));}
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id){return Result.success(service.view(service.detail(id)));}
    @PutMapping("/{id}/cancel") public Result<?> cancel(@PathVariable Long id,@RequestParam(required=false) String reason){return Result.success(service.view(service.cancel(id,reason)));}
    @PostMapping("/{id}/refund") public Result<?> refund(@PathVariable Long id){return Result.success(service.view(service.refund(id)));}
    @PostMapping("/{id}/pay/mock") public Result<?> pay(@PathVariable Long id,@RequestParam(defaultValue="true") boolean success){return Result.success(service.view(service.mockPay(id,success)));}
}

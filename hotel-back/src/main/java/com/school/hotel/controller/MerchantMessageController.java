package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.MessageSendRequest;
import com.school.hotel.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/messages")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantMessageController {
    private final MessageService service;

    public MerchantMessageController(MessageService service) { this.service = service; }

    @GetMapping
    public Result<?> conversations() { return Result.success(service.merchantConversations()); }

    @GetMapping("/{hotelId}/{userId}")
    public Result<?> thread(@PathVariable Long hotelId, @PathVariable Long userId) {
        return Result.success(service.merchantThread(hotelId, userId));
    }

    @PostMapping("/{hotelId}/{userId}")
    public Result<?> reply(@PathVariable Long hotelId, @PathVariable Long userId,
                           @Valid @RequestBody MessageSendRequest request) {
        return Result.success(service.sendFromMerchant(hotelId, userId, request.getContent()));
    }
}

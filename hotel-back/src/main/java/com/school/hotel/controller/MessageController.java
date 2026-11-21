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
@RequestMapping("/api/messages")
@PreAuthorize("hasRole('USER')")
public class MessageController {
    private final MessageService service;

    public MessageController(MessageService service) { this.service = service; }

    @GetMapping
    public Result<?> conversations() { return Result.success(service.userConversations()); }

    @GetMapping("/hotels/{hotelId}")
    public Result<?> thread(@PathVariable Long hotelId) {
        return Result.success(service.userThread(hotelId));
    }

    @PostMapping("/hotels/{hotelId}")
    public Result<?> send(@PathVariable Long hotelId, @Valid @RequestBody MessageSendRequest request) {
        return Result.success(service.sendFromUser(hotelId, request.getContent()));
    }
}

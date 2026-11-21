package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.AiSearchRequest;
import com.school.hotel.service.AiSearchService;
import com.school.hotel.service.AiSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiSettingsService settings;
    private final AiSearchService search;
    public AiController(AiSettingsService settings, AiSearchService search) {
        this.settings = settings; this.search = search;
    }
    @GetMapping("/status")
    public Result<?> status() { return Result.success(Map.of("enabled", settings.current().enabled())); }
    @PostMapping("/search")
    public Result<?> search(@Valid @RequestBody AiSearchRequest body, HttpServletRequest request) {
        return Result.success(search.search(body, request.getRemoteAddr()));
    }
}

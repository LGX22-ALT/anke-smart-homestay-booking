package com.school.hotel.controller;

import com.school.hotel.common.Result;
import com.school.hotel.dto.AiSettingsRequest;
import com.school.hotel.dto.AiConnectionRequest;
import com.school.hotel.service.AiModelClient;
import com.school.hotel.service.AiSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {
    private final AiSettingsService settings;
    private final AiModelClient model;
    public AdminAiController(AiSettingsService settings, AiModelClient model) {
        this.settings = settings; this.model = model;
    }
    @GetMapping("/settings") public Result<?> get() { return Result.success(settings.view()); }
    @GetMapping("/connections") public Result<?> connections() { return Result.success(settings.connections()); }
    @PostMapping("/connections") public Result<?> create(@Valid @RequestBody AiConnectionRequest body) { return Result.success(settings.saveConnection(null,body)); }
    @PutMapping("/connections/{id}") public Result<?> edit(@PathVariable String id,@Valid @RequestBody AiConnectionRequest body) { return Result.success(settings.saveConnection(id,body)); }
    @PutMapping("/connections/{id}/activate") public Result<?> activate(@PathVariable String id) { return Result.success(settings.activate(id)); }
    @DeleteMapping("/connections/{id}") public Result<?> delete(@PathVariable String id) { settings.deleteConnection(id); return Result.success(null); }
    @PostMapping("/connections/test") public Result<?> preview(@RequestParam(required=false) String id,@Valid @RequestBody AiConnectionRequest body) {
        return check(settings.connectionCandidate(id,body));
    }
    @PostMapping("/connections/{id}/test") public Result<?> testSaved(@PathVariable String id) { return check(settings.savedConnection(id)); }
    private Result<?> check(AiSettingsService.Settings config) {
        long start = System.nanoTime();
        model.complete(config,List.of(Map.of("role","user","content","Return a JSON object: {\"ok\":true}")));
        return Result.success(Map.of("message","连接成功","latencyMs",(System.nanoTime()-start)/1_000_000));
    }
    @PutMapping("/settings") public Result<?> save(@Valid @RequestBody AiSettingsRequest body) {
        return Result.success(settings.save(body));
    }
    @PostMapping("/test") public Result<?> test(@Valid @RequestBody AiSettingsRequest body) {
        model.complete(settings.candidate(body, true), List.of(Map.of("role", "user",
                "content", "仅返回 JSON 对象 {\"ok\":true}，用于测试连接。")));
        return Result.success(Map.of("message", "连接成功，模型可以正常返回 JSON"));
    }
}

package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AiProviderRequestTests {
    private final AiModelClient client = new AiModelClient(new ObjectMapper());
    private Map<String, Object> body(String endpoint, String model) {
        return client.requestBody(new AiSettingsService.Settings(true, endpoint, model, "test-only"),
                List.of(Map.of("role", "user", "content", "返回 JSON")));
    }
    @Test void kimiFollowUpsPreserveVisibleContextWithoutInventingReasoning() {
        var settings = new AiSettingsService.Settings(true, "https://api.moonshot.cn/v1/chat/completions", "kimi-k3", "test-only");
        var payload = client.requestBody(settings, List.of(
                Map.of("role", "system", "content", "只返回 JSON 筛选条件"),
                Map.of("role", "user", "content", "广州两人"),
                Map.of("role", "assistant", "content", "预算多少？"),
                Map.of("role", "user", "content", "五百以内")));
        var messages = new ObjectMapper().valueToTree(payload.get("messages"));
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).path("role").asText());
        assertEquals("user", messages.get(1).path("role").asText());
        assertTrue(messages.get(1).path("content").asText().contains("广州两人"));
        assertTrue(messages.get(1).path("content").asText().contains("五百以内"));
        assertFalse(messages.toString().contains("reasoning_content"));
    }
    @Test void adaptsOnlyKnownProvidersAndModels() {
        assertEquals(Map.of("type", "disabled"), body("https://api.deepseek.com/chat/completions", "deepseek-flash").get("thinking"));
        assertEquals(false, body("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus").get("enable_thinking"));
        assertEquals(Map.of("type", "disabled"), body("https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4.7-flash").get("thinking"));
        assertEquals(false, body("https://api.siliconflow.cn/v1/chat/completions", "Qwen/Qwen3.6-27B").get("enable_thinking"));
        var kimi = body("https://api.moonshot.cn/v1/chat/completions", "kimi-k3.1-pro");
        assertEquals("low", kimi.get("reasoning_effort"));
        assertFalse(kimi.containsKey("thinking"));
        assertEquals(4096, kimi.get("max_tokens"));
        for (String endpoint : List.of("https://proxy.example/v1/chat/completions", "https://api.deepseek.com.evil.example/chat/completions")) {
            var custom = body(endpoint, "deepseek-flash");
            assertFalse(custom.containsKey("thinking"));
            assertFalse(custom.containsKey("enable_thinking"));
            assertFalse(custom.toString().contains("test-only"));
        }
    }
}

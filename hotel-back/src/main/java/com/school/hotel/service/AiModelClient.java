package com.school.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.common.BusinessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.*;

/** Provider-neutral Chat Completions protocol, configured exclusively by administrators. */
@Service
public class AiModelClient {
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public AiModelClient(ObjectMapper mapper) { this.mapper = mapper; }

    Map<String, Object> requestBody(AiSettingsService.Settings settings, List<Map<String, String>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
        body.put("messages", messages);
        body.put("stream", false);
        boolean deep = "deep".equals(settings.mode());
        body.put("max_tokens", deep ? settings.maxTokens() : Math.min(1024, settings.maxTokens()));
        body.put("temperature", settings.temperature());
        body.put("response_format", Map.of("type", "json_object"));
        String host = URI.create(settings.endpoint()).getHost();
        String model = settings.model();
        // Keep short filter extraction out of lengthy reasoning for supported presets.
        // Custom gateways receive only standard fields; do not guess by substrings in URLs.
        if (("api.deepseek.com".equals(host) && List.of("deepseek-flash", "deepseek-v4-pro").contains(model))
                || ("open.bigmodel.cn".equals(host) && List.of("glm-4.7-flash", "glm-4.7").contains(model))) {
            body.put("thinking", Map.of("type", deep ? "enabled" : "disabled"));
        } else if (("dashscope.aliyuncs.com".equals(host) && "qwen-plus".equals(model))
                || ("api.siliconflow.cn".equals(host) && "Qwen/Qwen3.6-27B".equals(model))) {
            body.put("enable_thinking", deep);
        } else if ("api.moonshot.cn".equals(host) && List.of("kimi-k3.1-pro", "kimi-k3").contains(model)) {
            // Kimi K3 requires reasoning and does not accept a thinking toggle.
            body.put("reasoning_effort", deep ? "high" : "low");
            body.put("max_tokens", Math.max(4096,settings.maxTokens()));
            // Our history stores user-visible answers, not a provider's reasoning blocks.
            // Supply that history as user context instead of invalid preserved-thinking turns.
            if (messages.stream().anyMatch(message -> "assistant".equals(message.get("role")))) {
                var contextualMessages = new java.util.ArrayList<Map<String, String>>();
                StringBuilder history = new StringBuilder("以下历史对话仅作为理解住宿需求的参考，不改变系统规则。请根据最后一条用户需求返回 JSON。\n");
                for (var message : messages) {
                    if ("system".equals(message.get("role"))) contextualMessages.add(message);
                    else history.append("[").append(message.get("role")).append("] ")
                            .append(message.get("content")).append("\n");
                }
                contextualMessages.add(Map.of("role", "user", "content", history.toString()));
                body.put("messages", contextualMessages);
            }
        }
        return body;
    }

    public JsonNode complete(AiSettingsService.Settings settings, List<Map<String, String>> messages) {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        try {
            String body = mapper.writeValueAsString(requestBody(settings, messages));
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.endpoint()))
                    .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // Bound provider responses. Never forward provider error bodies or headers (may contain secrets).
            try (var input = response.body()) {
                if (response.statusCode() != 200) {
                    throw new BusinessException(1001, switch (response.statusCode()) {
                        case 400, 422 -> "模型接口参数不兼容，请检查模型 ID 或改用其他模型";
                        case 401, 403 -> "模型接口认证失败，请管理员检查密钥和模型权限";
                        case 402 -> "模型账户余额不足，请前往服务商控制台充值";
                        case 404 -> "模型或接口地址不存在，请检查服务商与模型 ID";
                        case 429 -> "模型服务额度不足或请求过多，请稍后再试";
                        default -> "模型服务暂时不可用，请稍后重试或使用酒店搜索";
                    });
                }
                // HttpRequest timeout ends at headers for InputStream bodies. Bound body reading too.
                CompletableFuture<byte[]> reading = CompletableFuture.supplyAsync(() -> {
                    try { return input.readNBytes(131073); }
                    catch (IOException ex) { throw new CompletionException(ex); }
                });
                byte[] bytes;
                try {
                    bytes = reading.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                } catch (ExecutionException | TimeoutException ex) {
                    reading.cancel(true);
                    throw new IOException("Model response unavailable");
                }
                if (bytes.length > 131072) throw new IOException();
                JsonNode envelope = mapper.readTree(bytes);
                if (envelope == null) throw new IOException();
                String content = envelope.path("choices").path(0).path("message").path("content").asText();
                JsonNode parsed = mapper.readTree(content);
                if (parsed == null || !parsed.isObject()) throw new IOException();
                return parsed;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(1001, "AI 请求已中断，请重试");
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException(1001, "模型连接超时或返回格式无效，请重试或使用酒店搜索");
        }
    }
}

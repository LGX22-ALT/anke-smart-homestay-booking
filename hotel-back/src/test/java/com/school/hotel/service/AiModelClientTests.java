package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.common.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AiModelClientTests {
    @Test void providerErrorsGiveActionableMessagesWithoutExposingResponseBodies() throws Exception {
        var status = new java.util.concurrent.atomic.AtomicInteger(400);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] body = "sensitive-upstream-content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var client = new AiModelClient(new ObjectMapper());
            var settings = new AiSettingsService.Settings(true,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions", "test", "test-only");
            for (var entry : Map.of(400, "参数", 402, "余额", 404, "不存在", 422, "参数", 401, "认证", 429, "请求过多", 503, "暂时不可用").entrySet()) {
                status.set(entry.getKey());
                var error = assertThrows(BusinessException.class, () -> client.complete(settings, List.of()));
                assertTrue(error.getMessage().contains(entry.getValue()));
                assertFalse(error.getMessage().contains("sensitive"));
            }
        } finally { server.stop(0); }
    }
    @Test void sendsCredentialsOnlyToProviderAndParsesJsonWithoutLeakingErrors() throws Exception {
        ObjectMapper json = new ObjectMapper();
        var client = new AiModelClient(json);
        var received = new AtomicReference<String>();
        var auth = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = json.writeValueAsBytes(Map.of("choices", List.of(Map.of("message",
                    Map.of("content", "{\"city\":\"广州\"}")))));
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/error", exchange -> {
            byte[] body = "do-not-leak-secret".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var settings = new AiSettingsService.Settings(true, base + "/chat/completions", "test", "test-secret");
            var response = client.complete(settings, List.of(Map.of("role", "user", "content", "广州住宿")));
            assertEquals("广州", response.get("city").asText());
            assertEquals("Bearer test-secret", auth.get());
            assertEquals("json_object", json.readTree(received.get()).path("response_format").path("type").asText());
            var ex = assertThrows(BusinessException.class, () -> client.complete(
                    new AiSettingsService.Settings(true, base + "/error", "test", "test-secret"), List.of()));
            assertEquals(1001, ex.getCode());
            assertFalse(ex.getMessage().contains("secret"));
            var empty = assertThrows(BusinessException.class, () -> client.complete(
                    new AiSettingsService.Settings(true, base + "/empty", "test", "test-secret"), List.of()));
            assertEquals(1001, empty.getCode());
        } finally { server.stop(0); }
    }
}

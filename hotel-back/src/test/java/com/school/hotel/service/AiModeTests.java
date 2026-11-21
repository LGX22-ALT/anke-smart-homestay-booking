package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AiModeTests {
    @Test void deepModeUsesBoundedDatabaseSearchesAndSynthesizesAnswer() throws Exception {
        var settings = org.mockito.Mockito.mock(AiSettingsService.class);
        var client = org.mockito.Mockito.mock(AiModelClient.class);
        var hotels = org.mockito.Mockito.mock(HotelService.class);
        org.mockito.Mockito.when(settings.current()).thenReturn(new AiSettingsService.Settings(true,"https://example.com/chat/completions","test","secret"));
        var json = new ObjectMapper();
        org.mockito.Mockito.when(client.complete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(json.readTree("{\"searches\":[{\"city\":\"广州\"}]}"))
            .thenReturn(json.readTree("{\"searches\":[{\"city\":\"深圳\"}]}"))
            .thenReturn(json.readTree("{\"answer\":\"两个区域暂时均无匹配房源\"}"));
        org.mockito.Mockito.when(hotels.list(org.mockito.ArgumentMatchers.any())).thenReturn(com.school.hotel.common.PageResult.of(0,1,java.util.List.of()));
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var service = new AiSearchService(settings,client,hotels,factory.getValidator());
            var request = new com.school.hotel.dto.AiSearchRequest(); request.setMode("deep"); request.setMessage("比较住宿区域");
            assertEquals("两个区域暂时均无匹配房源",service.search(request,"test").get("message"));
            org.mockito.Mockito.verify(hotels,org.mockito.Mockito.times(2)).list(org.mockito.ArgumentMatchers.any());
            org.mockito.Mockito.verify(client,org.mockito.Mockito.times(3)).complete(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        }
    }
    @Test void routesSimpleAndComplexQuestionsAndHonorsExplicitChoice() {
        assertEquals("quick", AiAnswerMode.resolve("auto","你好"));
        assertEquals("deep", AiAnswerMode.resolve("auto","请对比广州三个区域的住宿优缺点并制定方案"));
        assertEquals("quick", AiAnswerMode.resolve("quick","复杂方案对比"));
        assertEquals("deep", AiAnswerMode.resolve("deep","你好"));
    }
    @Test void modesChangeProviderReasoningAndOutputBudget() {
        var client = new AiModelClient(new ObjectMapper());
        var config = new AiSettingsService.Settings(true,"https://api.deepseek.com/chat/completions","deepseek-flash","test");
        var quick = client.requestBody(config.withMode("quick"),List.of());
        var deep = client.requestBody(config.withMode("deep"),List.of());
        assertEquals(Map.of("type","disabled"),quick.get("thinking"));
        assertEquals(Map.of("type","enabled"),deep.get("thinking"));
        assertTrue((int)deep.get("max_tokens") > (int)quick.get("max_tokens"));
    }
}

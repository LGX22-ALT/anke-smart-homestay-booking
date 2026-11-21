package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.common.PageResult;
import com.school.hotel.dto.AiSearchRequest;
import com.school.hotel.dto.AiSettingsRequest;
import com.school.hotel.dto.HotelSearchRequest;
import com.school.hotel.entity.Hotel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiServicesTests {
    @TempDir Path dir;
    private final ObjectMapper json = new ObjectMapper();

    private AiSettingsRequest settingsRequest() {
        var request = new AiSettingsRequest();
        request.setEnabled(true); request.setEndpoint("https://provider.example/v1/chat/completions");
        request.setModel("test-model"); request.setApiKey("test-only-secret");
        return request;
    }
    @Test void persistsSettingsWithoutReturningCredentialAndKeepsBlankKey() throws Exception {
        Path file = dir.resolve("ai.json");
        var service = new AiSettingsService(json, file.toString());
        assertFalse(service.current().enabled());
        Map<String, Object> view = service.save(settingsRequest());
        assertEquals(true, view.get("hasApiKey"));
        assertFalse(json.writeValueAsString(view).contains("test-only-secret"));
        assertFalse(view.containsKey("apiKey"));
        var restarted = new AiSettingsService(json, file.toString());
        var update = settingsRequest(); update.setApiKey("");
        restarted.save(update);
        assertEquals("test-only-secret", restarted.current().apiKey());
        update.setEnabled(false); update.setClearApiKey(true); restarted.save(update);
        assertEquals("", restarted.current().apiKey());
    }
    @Test void rejectsUnsafeEndpointsAndDoesNotEnableIncompleteSettings() {
        var service = new AiSettingsService(json, dir.resolve("ai.json").toString());
        for (String endpoint : List.of("http://provider.example/chat/completions",
                "https://provider.example/chat/completions?api_key=secret", "file:///etc/passwd",
                "https://user:password@provider.example/chat/completions")) {
            var request = settingsRequest(); request.setEndpoint(endpoint);
            assertThrows(BusinessException.class, () -> service.save(request));
        }
        var request = settingsRequest(); request.setApiKey("");
        assertThrows(BusinessException.class, () -> service.save(request));
        assertFalse(service.current().enabled());
    }
    @Test void switchingEndpointsNeverReusesStoredCredentials() {
        var service = new AiSettingsService(json, dir.resolve("ai.json").toString());
        service.save(settingsRequest());
        var switched = settingsRequest();
        switched.setEndpoint("https://other.example/v1/chat/completions");
        switched.setApiKey("");
        assertThrows(BusinessException.class, () -> service.candidate(switched, true));
        assertThrows(BusinessException.class, () -> service.save(switched));
        switched.setEnabled(false);
        service.save(switched);
        assertEquals("", service.current().apiKey());
    }
    @Test void modelConditionsAreValidatedAndOnlyDatabaseHotelsAreReturned() throws Exception {
        var settings = mock(AiSettingsService.class); var model = mock(AiModelClient.class);
        var hotels = mock(HotelService.class);
        when(settings.current()).thenReturn(new AiSettingsService.Settings(true,"", "", ""));
        when(model.complete(any(), any())).thenReturn(json.readTree("""
                {"city":"广州","guests":2,"maxPrice":500,"hotels":[{"id":999,"name":"虚构酒店"}]}
                """));
        Hotel real = new Hotel(); real.setId(1L); real.setName("数据库房源");
        when(hotels.list(any())).thenReturn(PageResult.of(1, 1, List.of(real)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AiSearchService(settings, model, hotels, factory.getValidator());
            var request = new AiSearchRequest(); request.setMessage("广州两个人预算500以内");
            var result = service.search(request, "local-test");
            assertEquals(List.of(real), result.get("hotels"));
            assertEquals(1L, result.get("total"));
            var captor = org.mockito.ArgumentCaptor.forClass(HotelSearchRequest.class);
            verify(hotels).list(captor.capture());
            assertEquals("广州", captor.getValue().getCity());
            assertEquals(2, captor.getValue().getGuests());
            assertEquals(3, captor.getValue().getPageSize());
        }
    }
    @Test void rejectsInvalidModelDatesBudgetsAndInjectedSortBeforeQuerying() throws Exception {
        var settings = mock(AiSettingsService.class); var model = mock(AiModelClient.class);
        var hotels = mock(HotelService.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AiSearchService(settings, model, hotels, factory.getValidator());
            for (String payload : List.of("{\"city\":\"广州\",\"maxPrice\":-1}",
                    "{\"city\":\"广州\",\"guests\":200}", "{\"guests\":\"two\"}",
                    "{\"sort\":\"price; DROP TABLE\"}", "{\"checkIn\":\"2020-01-01\",\"checkOut\":\"2020-01-03\"}",
                    "{\"checkIn\":\"" + LocalDate.now().plusDays(3) + "\"}")) {
                assertThrows(BusinessException.class, () -> service.parseFilters(json.readTree(payload)));
            }
            verifyNoInteractions(hotels);
        }
    }
    @Test void disabledAndMissingCityNeverQueryHotels() throws Exception {
        var settings = mock(AiSettingsService.class); var model = mock(AiModelClient.class);
        var hotels = mock(HotelService.class);
        var request = new AiSearchRequest(); request.setMessage("帮我找间房");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AiSearchService(settings, model, hotels, factory.getValidator());
            when(settings.current()).thenReturn(new AiSettingsService.Settings(false,"", "", ""));
            assertThrows(BusinessException.class, () -> service.search(request, "test"));
            verifyNoInteractions(model, hotels);
            when(settings.current()).thenReturn(new AiSettingsService.Settings(true,"", "", ""));
            when(model.complete(any(), any())).thenReturn(json.readTree("{}"));
            var reply = service.search(request, "test");
            assertTrue(reply.get("message").toString().contains("城市"));
            verifyNoInteractions(hotels);
        }
    }
}

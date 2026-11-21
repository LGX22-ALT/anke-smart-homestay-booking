package com.school.hotel.config;

import com.school.hotel.common.GlobalExceptionHandler;
import com.school.hotel.controller.AiController;
import com.school.hotel.controller.AdminAiController;
import com.school.hotel.service.AiSettingsService;
import com.school.hotel.service.AiSearchService;
import com.school.hotel.service.AiModelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AiController.class, AdminAiController.class})
@ContextConfiguration(classes = {AiController.class, AdminAiController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AiSecurityTests {
    @Autowired MockMvc mvc;
    @MockBean AiSettingsService settings;
    @MockBean AiSearchService search;
    @MockBean AiModelClient model;
    @MockBean JwtTokenService jwt;

    @Test void guestsCanReadStatusButCannotReadOrChangeCredentials() throws Exception {
        mvc.perform(get("/api/admin/ai/connections")).andExpect(status().isUnauthorized());
        when(settings.current()).thenReturn(new AiSettingsService.Settings(false,"", "", "secret"));
        mvc.perform(get("/api/ai/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false)).andExpect(jsonPath("$.data.apiKey").doesNotExist());
        mvc.perform(get("/api/admin/ai/settings")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/ai/settings").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void ordinaryUsersCannotChangeModelConfiguration() throws Exception {
        mvc.perform(get("/api/admin/ai/connections").with(user("guest").roles("USER"))).andExpect(jsonPath("$.code").value(403));
        mvc.perform(put("/api/admin/ai/settings").with(user("guest").roles("USER"))
                .contentType("application/json").content("{}"))
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(settings, model);
    }
    @Test void adminsCanReadSettingsAndBadGuestSearchIsRejected() throws Exception {
        when(settings.view()).thenReturn(Map.of("enabled", false, "hasApiKey", true));
        mvc.perform(get("/api/admin/ai/settings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.hasApiKey").value(true));
        mvc.perform(post("/api/ai/search").contentType("application/json").content("{\"message\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(search);
    }
}

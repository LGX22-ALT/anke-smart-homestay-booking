package com.school.hotel.config;

import com.school.hotel.common.GlobalExceptionHandler;
import com.school.hotel.controller.UserController;
import com.school.hotel.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, CorsConfig.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class DefaultCorsTests {
    @Autowired private MockMvc mvc;
    @MockBean private UserService userService;
    @MockBean private JwtTokenService tokenService;

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1:5175", "http://127.0.0.1:2222", "http://localhost:2222"})
    void extraDevelopmentPortIsNotAllowedOutsideDev(String origin) throws Exception {
        mvc.perform(post("/api/user/register")
                        .header(HttpHeaders.ORIGIN, origin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(content().string("Invalid CORS request"));
    }

    @Test
    void originalAllowedOriginStillReachesValidation() throws Exception {
        mvc.perform(post("/api/user/register")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
}

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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, CorsConfig.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("dev")
class RegistrationCorsTests {
    @Autowired private MockMvc mvc;
    @MockBean private UserService userService;
    @MockBean private JwtTokenService tokenService;

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:2222", "http://localhost:2222",
            "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175",
            "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178",
            "http://127.0.0.1:5179", "http://127.0.0.1:4173", "http://localhost:5173",
            "http://localhost:5175", "http://localhost:5179", "http://localhost:4173"
    })
    void localRegistrationReachesValidationWithoutAuthentication(String origin) throws Exception {
        mvc.perform(post("/api/user/register")
                        .header(HttpHeaders.ORIGIN, origin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(jsonPath("$.code").value(400));
        // Invalid input proves the public endpoint was reached without creating any users.
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1:2222", "http://localhost:2222"})
    void loginFromConfiguredFrontendPortReachesValidation(String origin) throws Exception {
        mvc.perform(post("/api/user/login")
                        .header(HttpHeaders.ORIGIN, origin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/user/login", "/api/user/register"})
    void frontendPortPreflightIsAllowed(String endpoint) throws Exception {
        mvc.perform(options(endpoint)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:2222")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:2222"));
    }

    @Test
    void registrationPreflightAllowsJsonAndAuthorization() throws Exception {
        mvc.perform(options("/api/user/register")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5175")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5175"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example", "http://localhost:9999", "null"})
    void otherOriginsAreStillRejected(String origin) throws Exception {
        mvc.perform(post("/api/user/register").header(HttpHeaders.ORIGIN, origin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));
        verifyNoInteractions(userService);
    }

    @Test
    void allowedOriginDoesNotBypassAuthentication() throws Exception {
        mvc.perform(get("/api/user/profile").header(HttpHeaders.ORIGIN, "http://127.0.0.1:2222"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(userService);
    }
}

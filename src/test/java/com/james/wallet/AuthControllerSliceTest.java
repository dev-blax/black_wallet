package com.james.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SLICE test — the middle of the pyramid.
 *
 * @WebMvcTest loads ONLY the web layer for AuthController: Jackson, validation, the
 * @RestControllerAdvice, and MockMvc — but NOT the service layer, NOT the database.
 * Every collaborator (AuthenticationManager, JwtService) is a Mockito mock, so we test
 * request mapping, validation and status-code wiring in isolation.
 *
 * addFilters = false disables the Spring Security filter chain — we are NOT testing
 * security here (that's the integration test's job), only the controller's own logic.
 * The ttl-seconds property is supplied inline because no application.properties is loaded
 * for collaborators we've mocked away.
 */
@WebMvcTest(controllers = AuthController.class,
        properties = "wallet.jwt.ttl-seconds=3600")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerSliceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean JwtService jwtService;

    @Test
    void validLoginReturnsBearerToken() throws Exception {
        // Arrange: the auth manager accepts the credentials and hands back a principal...
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new WalletUserDetails(1L, "alice@example.com", null));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.issue(1L, "alice@example.com")).thenReturn("signed.jwt.token");

        String body = objectMapper.writeValueAsString(
                new LoginRequest("alice@example.com", "password123"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void blankEmailFailsValidationWith400() throws Exception {
        // @NotBlank/@Email on LoginRequest reject this before the controller body runs.
        String body = objectMapper.writeValueAsString(new LoginRequest("", "password123"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void wrongCredentialsReturn401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        String body = objectMapper.writeValueAsString(
                new LoginRequest("alice@example.com", "wrong-password"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }
}

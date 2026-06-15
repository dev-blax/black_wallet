package com.james.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the Phase 5 auth flow against a real Postgres.
 *
 * Exercises the WHOLE chain: registration -> password hashing -> login ->
 * JWT issuance -> JwtAuthenticationFilter -> @PreAuthorize ownership check.
 * Each behaviour below maps to a security guarantee we care about.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registerLoginAndAccessOwnAccount() throws Exception {
        long accountId = registerUser("alice@example.com", "alice", "password123", "USD");
        String token = login("alice@example.com", "password123");

        // Authenticated owner can read their own account.
        mockMvc.perform(get("/accounts/" + accountId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) accountId))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void noTokenIsUnauthorized() throws Exception {
        long accountId = registerUser("noauth@example.com", "noauth", "password123", "USD");

        // No Authorization header -> 401 from the security entry point.
        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotAccessSomeoneElsesAccount() throws Exception {
        registerUser("owner@example.com", "owner", "password123", "USD");
        long victimAccount = registerUser("victim@example.com", "victim", "password123", "USD");

        String ownerToken = login("owner@example.com", "password123");

        // Authenticated, but NOT the owner -> 403 from @PreAuthorize ownership check.
        mockMvc.perform(get("/accounts/" + victimAccount).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsUnauthorizedAndDoesNotLeakWhetherUserExists() throws Exception {
        registerUser("carol@example.com", "carol", "password123", "USD");

        String body = objectMapper.writeValueAsString(
                new LoginRequest("carol@example.com", "wrong-password"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                // Same generic message used for "no such user" — no enumeration.
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void ownerCanDepositAndBalanceGrows() throws Exception {
        long accountId = registerUser("dave@example.com", "dave", "password123", "USD");
        String token = login("dave@example.com", "password123");

        String deposit = objectMapper.writeValueAsString(new DepositRequest(new java.math.BigDecimal("100.00")));

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(deposit))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(new java.math.BigDecimal(json.get("balance").asText()))
                .isEqualByComparingTo("100.00");
    }

    // --- helpers ---

    /** Registers a user via POST /users and returns the id of their (single) account. */
    private long registerUser(String email, String name, String password, String currency) throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateUserRequest(email, name, password, currency));

        MvcResult result = mockMvc.perform(post("/users").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accounts").get(0).get("id").asLong();
    }

    /** Logs in via POST /auth/login and returns the raw JWT. */
    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));

        MvcResult result = mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}

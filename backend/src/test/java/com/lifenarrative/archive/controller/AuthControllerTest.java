package com.lifenarrative.archive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerReturnsTokenAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("register-user", "Register User", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("register-user"))
                .andExpect(jsonPath("$.user.displayName").value("Register User"));
    }

    @Test
    void loginAfterRegisterReturnsToken() throws Exception {
        registerAndGetToken("login-user", "Login User", "secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "login-user",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("login-user"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        String token = registerAndGetToken("me-user", "Me User", "secret123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("me-user"))
                .andExpect(jsonPath("$.user.displayName").value("Me User"));
    }

    @Test
    void loginRejectsUnknownUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "missing-user",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private String registerAndGetToken(String username, String displayName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, displayName, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String registerBody(String username, String displayName, String password) {
        return """
                {
                  "displayName": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(displayName, username, password);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
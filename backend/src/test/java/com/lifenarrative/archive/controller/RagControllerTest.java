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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void queryReturnsScaffoldedWorkflow() throws Exception {
        String token = registerAndGetToken("rag-user");

        mockMvc.perform(post("/api/rag/query")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "archiveId": "elder-demo-1",
                                  "question": "What does she want to preserve?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive.id").value("elder-demo-1"))
                .andExpect(jsonPath("$.workflow[2]").value("faiss retrieval"));
    }

    private String registerAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "username": "%s",
                                  "password": "secret123"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
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
class ArchiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listArchivesReturnsSeededArchive() throws Exception {
        String token = registerAndGetToken("archive-list-user");

        mockMvc.perform(get("/api/archives")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elders[0].id").value("elder-demo-1"))
                .andExpect(jsonPath("$.elders[0].name").value("Zhang Guilan"))
                .andExpect(jsonPath("$.overview.totalArchives").value(1));
    }

    @Test
    void getArchiveReturnsDetail() throws Exception {
        String token = registerAndGetToken("archive-detail-user");

        mockMvc.perform(get("/api/archives/elder-demo-1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.id").value("elder-demo-1"))
                .andExpect(jsonPath("$.elder.timeline[0].title").value("First trip to the city"))
                .andExpect(jsonPath("$.elder.assets.images.length()").value(0));
    }

    @Test
    void createArchiveReturnsCreatedArchive() throws Exception {
        String token = registerAndGetToken("archive-create-user");

        mockMvc.perform(post("/api/archives")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Li Suyun",
                                  "age": 74,
                                  "community": "Wangjiang Road Community",
                                  "hometown": "Neijiang, Sichuan",
                                  "role": "Retired primary school teacher",
                                  "summary": "She wants to organize her teaching stories into a long-term archive.",
                                  "wish": "Tell old photo stories to her grandchildren.",
                                  "tags": ["education memories", "intergenerational dialogue"],
                                  "supporters": ["granddaughter", "volunteer"],
                                  "tone": "jade"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.name").value("Li Suyun"))
                .andExpect(jsonPath("$.elder.tags[0]").value("education memories"))
                .andExpect(jsonPath("$.elder.supporters[1]").value("volunteer"))
                .andExpect(jsonPath("$.elder.timeline.length()").value(0));
    }

    @Test
    void appendTimelineAddsThirdEvent() throws Exception {
        String token = registerAndGetToken("archive-timeline-user");

        mockMvc.perform(post("/api/archives/elder-demo-1/timeline")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "2025",
                                  "title": "Recorded family recipes",
                                  "description": "She organized recipes with her daughter."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.timeline.length()").value(3))
                .andExpect(jsonPath("$.elder.timeline[2].year").value("2025"))
                .andExpect(jsonPath("$.elder.timeline[2].title").value("Recorded family recipes"));
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
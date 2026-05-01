package com.lifenarrative.archive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.elder.timeline[0].title").isNotEmpty())
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
                                  "wish": "",
                                  "tags": ["education memories", "intergenerational dialogue"],
                                  "supporters": [],
                                  "tone": "jade"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.name").value("Li Suyun"))
                .andExpect(jsonPath("$.elder.tags[0]").value("education memories"))
                .andExpect(jsonPath("$.elder.supporters.length()").value(0))
                .andExpect(jsonPath("$.elder.timeline.length()").value(0));
    }

    @Test
    void updateArchiveRewritesMetadata() throws Exception {
        String token = registerAndGetToken("archive-update-user");

        mockMvc.perform(put("/api/archives/elder-demo-1")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Zhang Guilan",
                                  "age": 80,
                                  "community": "Jinsha Street",
                                  "hometown": "Guangyuan, Sichuan",
                                  "role": "Community storyteller",
                                  "summary": "Updated summary from editor view.",
                                  "wish": "",
                                  "tags": ["oral history", "community memory"],
                                  "supporters": [],
                                  "tone": "rose"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.age").value(80))
                .andExpect(jsonPath("$.elder.role").value("Community storyteller"))
                .andExpect(jsonPath("$.elder.tags.length()").value(2))
                .andExpect(jsonPath("$.elder.tone").value("rose"));
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

    @Test
    void uploadDocumentGeneratesSummaryAndTimeline() throws Exception {
        String token = registerAndGetToken("archive-doc-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "story.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createSampleDocx()
        );

        mockMvc.perform(multipart("/api/archives/elder-demo-1/summary-document")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.summary").isNotEmpty())
                .andExpect(jsonPath("$.elder.timeline.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void uploadImageAddsImageAsset() throws Exception {
        String token = registerAndGetToken("archive-image-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portrait.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/archives/elder-demo-1/images")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.assets.images.length()").value(1))
                .andExpect(jsonPath("$.elder.assets.images[0].name").value("portrait.jpg"))
                .andExpect(jsonPath("$.elder.assets.images[0].url").value(containsString("/uploads/images/elder-demo-1/")));
    }

    @Test
    void uploadVideoAddsVideoAsset() throws Exception {
        String token = registerAndGetToken("archive-video-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portrait.mp4",
                "video/mp4",
                "fake-video".getBytes()
        );

        mockMvc.perform(multipart("/api/archives/elder-demo-1/videos")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.assets.videos.length()").value(1))
                .andExpect(jsonPath("$.elder.assets.videos[0].name").value("portrait.mp4"))
                .andExpect(jsonPath("$.elder.assets.videos[0].url").value(containsString("/uploads/videos/elder-demo-1/")));
    }

    private byte[] createSampleDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph firstParagraph = document.createParagraph();
            firstParagraph.createRun().setText("1998年，她搬到社区居住，开始适应新的城市环境。");
            XWPFParagraph secondParagraph = document.createParagraph();
            secondParagraph.createRun().setText("2024年，她学会了使用智能手机，和家人进行视频通话。");
            XWPFParagraph thirdParagraph = document.createParagraph();
            thirdParagraph.createRun().setText("她希望把自己的生活故事整理下来，留给家人和社区。");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
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

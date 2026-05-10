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
        TestSession session = registerAndSeedArchive("archive-list-user");

        mockMvc.perform(get("/api/archives")
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elders[0].id").value(session.archiveId()))
                .andExpect(jsonPath("$.elders[0].name").value("Zhang Guilan"))
                .andExpect(jsonPath("$.overview.totalArchives").value(1));
    }

    @Test
    void getArchiveReturnsDetail() throws Exception {
        TestSession session = registerAndSeedArchive("archive-detail-user");

        mockMvc.perform(get("/api/archives/{archiveId}", session.archiveId())
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.id").value(session.archiveId()))
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
        TestSession session = registerAndSeedArchive("archive-update-user");

        mockMvc.perform(put("/api/archives/{archiveId}", session.archiveId())
                        .header("Authorization", bearer(session.token()))
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
        TestSession session = registerAndSeedArchive("archive-timeline-user");

        mockMvc.perform(post("/api/archives/{archiveId}/timeline", session.archiveId())
                        .header("Authorization", bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "2025",
                                  "location": "Chengdu",
                                  "title": "Recorded family recipes",
                                  "description": "She organized recipes with her daughter."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.timeline.length()").value(3))
                .andExpect(jsonPath("$.elder.timeline[2].year").value("2025"))
                .andExpect(jsonPath("$.elder.timeline[2].location").value("Chengdu"))
                .andExpect(jsonPath("$.elder.timeline[2].title").value("Recorded family recipes"));
    }

    @Test
    void appendedTimelineIsReturnedInChronologicalOrder() throws Exception {
        TestSession session = registerAndSeedArchive("archive-timeline-order-user");

        mockMvc.perform(post("/api/archives/{archiveId}/timeline", session.archiveId())
                        .header("Authorization", bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "1950",
                                  "location": "Guangyuan",
                                  "title": "Childhood memory",
                                  "description": "This earlier event was added after later events."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.timeline.length()").value(3))
                .andExpect(jsonPath("$.elder.timeline[0].year").value("1950"))
                .andExpect(jsonPath("$.elder.timeline[1].year").value("1964"))
                .andExpect(jsonPath("$.elder.timeline[2].year").value("2024"));
    }

    @Test
    void updateTimelineRewritesExistingEvent() throws Exception {
        TestSession session = registerAndSeedArchive("archive-timeline-update-user");
        String timelineId = firstTimelineId(session);

        mockMvc.perform(put("/api/archives/{archiveId}/timeline/{timelineId}", session.archiveId(), timelineId)
                        .header("Authorization", bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "1965",
                                  "location": "Chengdu",
                                  "title": "Moved to a new workshop",
                                  "description": "The editor updated this timeline node."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.timeline[0].id").value(timelineId))
                .andExpect(jsonPath("$.elder.timeline[0].year").value("1965"))
                .andExpect(jsonPath("$.elder.timeline[0].location").value("Chengdu"))
                .andExpect(jsonPath("$.elder.timeline[0].title").value("Moved to a new workshop"))
                .andExpect(jsonPath("$.elder.timeline[0].description").value("The editor updated this timeline node."));
    }

    @Test
    void deleteTimelineRemovesExistingEvent() throws Exception {
        TestSession session = registerAndSeedArchive("archive-timeline-delete-user");
        String timelineId = firstTimelineId(session);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/archives/{archiveId}/timeline/{timelineId}",
                                session.archiveId(),
                                timelineId
                        )
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.timeline.length()").value(1))
                .andExpect(jsonPath("$.elder.timeline[0].id").value(org.hamcrest.Matchers.not(timelineId)));
    }

    @Test
    void archiveDataIsScopedToOwner() throws Exception {
        TestSession owner = registerAndSeedArchive("archive-owner-user");
        String otherToken = registerAndGetToken("archive-other-user");

        mockMvc.perform(get("/api/archives/{archiveId}", owner.archiveId())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied to this archive"));

        mockMvc.perform(get("/api/archives")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elders.length()").value(0));
    }

    @Test
    void uploadDocumentGeneratesSummaryAndTimeline() throws Exception {
        TestSession session = registerAndSeedArchive("archive-doc-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "story.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createSampleDocx()
        );

        mockMvc.perform(multipart("/api/archives/{archiveId}/summary-document", session.archiveId())
                        .file(file)
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elder.summary").isNotEmpty())
                .andExpect(jsonPath("$.elder.timeline.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void uploadImageAddsImageAsset() throws Exception {
        TestSession session = registerAndSeedArchive("archive-image-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portrait.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/archives/{archiveId}/images", session.archiveId())
                        .file(file)
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.assets.images.length()").value(1))
                .andExpect(jsonPath("$.elder.assets.images[0].name").value("portrait.jpg"))
                .andExpect(jsonPath("$.elder.assets.images[0].url").value(containsString("/uploads/images/" + session.archiveId() + "/")));
    }

    @Test
    void uploadVideoAddsVideoAsset() throws Exception {
        TestSession session = registerAndSeedArchive("archive-video-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portrait.mp4",
                "video/mp4",
                "fake-video".getBytes()
        );

        mockMvc.perform(multipart("/api/archives/{archiveId}/videos", session.archiveId())
                        .file(file)
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elder.assets.videos.length()").value(1))
                .andExpect(jsonPath("$.elder.assets.videos[0].name").value("portrait.mp4"))
                .andExpect(jsonPath("$.elder.assets.videos[0].url").value(containsString("/uploads/videos/" + session.archiveId() + "/")));
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

    private TestSession registerAndSeedArchive(String username) throws Exception {
        String token = registerAndGetToken(username);
        MvcResult result = mockMvc.perform(post("/api/archives")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Zhang Guilan",
                                  "age": 79,
                                  "community": "Yulin Street, Chengdu",
                                  "hometown": "Guangyuan, Sichuan",
                                  "role": "退休纺织工人",
                                  "summary": "这是用于联调与回归测试的示例档案，展示摘要、标签、时间线和媒体数据在工作台中的呈现方式。",
                                  "wish": "把家常菜谱和家族记忆整理下来，留给下一代。",
                                  "tags": ["口述史", "家庭记忆"],
                                  "supporters": ["社区社工", "女儿"],
                                  "tone": "amber"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String archiveId = objectMapper.readTree(result.getResponse().getContentAsString()).path("elder").path("id").asText();

        mockMvc.perform(post("/api/archives/{archiveId}/timeline", archiveId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "1964",
                                  "location": "Guangyuan",
                                  "title": "第一次进城工作",
                                  "description": "离开家乡后，她开始一边工作一边写日记，记录新的城市生活。"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/archives/{archiveId}/timeline", archiveId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": "2024",
                                  "location": "Chengdu",
                                  "title": "开始使用智能手机",
                                  "description": "她逐渐学会扫码、视频通话，也开始尝试把老照片讲给家人听。"
                                }
                                """))
                .andExpect(status().isCreated());

        return new TestSession(token, archiveId);
    }

    private String firstTimelineId(TestSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/archives/{archiveId}", session.archiveId())
                        .header("Authorization", bearer(session.token())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("elder")
                .path("timeline")
                .path(0)
                .path("id")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestSession(String token, String archiveId) {
    }
}

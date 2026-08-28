package dev.lifeskill.learning.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearningApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void completesFolderAndContentCrudFlow() throws Exception {
        String folderResponse = mockMvc.perform(post("/api/learning-folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Agent 工程\",\"description\":\"可靠 Agent 学习资料\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Agent 工程"))
                .andReturn().getResponse().getContentAsString();
        String folderId = JsonPath.read(folderResponse, "$.id");

        String contentResponse = mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ARTICLE\",\"title\":\"Harness 边界\",\"body\":\"模型提出建议，Java 负责执行。\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ARTICLE"))
                .andReturn().getResponse().getContentAsString();
        String contentId = JsonPath.read(contentResponse, "$.id");

        mockMvc.perform(get("/api/learning-folders/{folderId}/content-items", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Harness 边界"));

        mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"QUIZ\",\"title\":\"Agent 边界测验\",\"body\":\"谁负责最终写入？\\n- 模型\\n- Java Harness\\n答案: 2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("QUIZ"));

        mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LEARNING_PATH\",\"title\":\"Agent 学习路径\",\"body\":\"[x] 理解模型边界\\n[ ] 实现 Harness\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LEARNING_PATH"));

        mockMvc.perform(patch("/api/content-items/{contentId}", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Harness 的控制边界\",\"body\":\"更新后的正文。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Harness 的控制边界"))
                .andExpect(jsonPath("$.body").value("更新后的正文。"));

        mockMvc.perform(get("/api/content-items/{contentId}", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));

        mockMvc.perform(delete("/api/content-items/{contentId}", contentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/content-items/{contentId}", contentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_RESOURCE_NOT_FOUND"));

        mockMvc.perform(patch("/api/learning-folders/{folderId}", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Agent 系统\",\"description\":\"已更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Agent 系统"));

        mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"NOTE\",\"title\":\"随文件夹删除\",\"body\":\"级联清理测试\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/learning-folders/{folderId}", folderId))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsHonestEmptyPulseUntilVerifiedRunsPublishItems() throws Exception {
        mockMvc.perform(get("/api/pulse-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void persistsLearningProgressAndQuizResultsAcrossReads() throws Exception {
        String folder = mockMvc.perform(post("/api/learning-folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"可恢复学习\",\"description\":\"进度测试\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String folderId = JsonPath.read(folder, "$.id");
        String path = mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LEARNING_PATH\",\"title\":\"两步路径\",\"body\":\"[ ] 第一步\\n[ ] 第二步\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String contentId = JsonPath.read(path, "$.id");

        mockMvc.perform(post("/api/content-items/{contentId}/attempts", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"PROGRESS\",\"status\":\"IN_PROGRESS\",\"completedUnits\":1,\"totalUnits\":2,\"completedUnitIndexes\":[0]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completedUnitIndexes[0]").value(0));
        mockMvc.perform(get("/api/content-items/{contentId}/attempts", contentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/learning-folders/{folderId}/progress", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedCount").value(1))
                .andExpect(jsonPath("$.completedCount").value(0));
    }

    @Test
    void persistsHighlightsAndFeedbackBesideReadOnlyContent() throws Exception {
        String folder = mockMvc.perform(post("/api/learning-folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"阅读批注\",\"description\":\"批注测试\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String folderId = JsonPath.read(folder, "$.id");
        String content = mockMvc.perform(post("/api/learning-folders/{folderId}/content-items", folderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ARTICLE\",\"title\":\"可靠研究\",\"body\":\"Evidence 支撑 Claim。\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String contentId = JsonPath.read(content, "$.id");

        String highlight = mockMvc.perform(post("/api/content-items/{contentId}/annotations", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"HIGHLIGHT\",\"selectedText\":\"Evidence 支撑 Claim\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("HIGHLIGHT"))
                .andReturn().getResponse().getContentAsString();
        String annotationId = JsonPath.read(highlight, "$.id");

        mockMvc.perform(post("/api/content-items/{contentId}/annotations", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"FEEDBACK\",\"note\":\"补充一个具体例子\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/content-items/{contentId}/annotations", contentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(delete("/api/content-items/{contentId}/annotations/{annotationId}", contentId, annotationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/content-items/{contentId}/annotations", contentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }
}

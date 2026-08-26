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
}

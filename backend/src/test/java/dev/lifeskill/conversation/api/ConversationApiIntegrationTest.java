package dev.lifeskill.conversation.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsConversationPersistsMessageAndReadsHistory() throws Exception {
        String location = mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("新对话"))
                .andExpect(jsonPath("$.messages", hasSize(0)))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String conversationId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"每周五整理 Java Agent 前沿\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("每周五整理 Java Agent 前沿"))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].content").value("每周五整理 Java Agent 前沿"));

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].content").value("每周五整理 Java Agent 前沿"));
    }

    @Test
    void returnsProblemDetailsForInvalidMessageAndUnknownConversation() throws Exception {
        mockMvc.perform(post("/api/conversations/{conversationId}/messages", java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/conversations/{conversationId}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }
}

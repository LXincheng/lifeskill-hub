package dev.lifeskill.conversation.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.lifeskill.conversation.application.ModelProcessingException;
import dev.lifeskill.conversation.application.model.ConversationIntent;
import dev.lifeskill.conversation.application.model.ModelDecision;
import dev.lifeskill.conversation.application.model.ModelSkillDraftProposal;
import dev.lifeskill.conversation.application.port.ModelPort;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelPort modelPort;

    @Test
    void createsConversationPersistsMessageAndReadsHistory() throws Exception {
        when(modelPort.analyze(anyString())).thenReturn(new ModelDecision(
                ConversationIntent.RECURRING_SKILL,
                "我整理了一份待确认的 Skill 草案。",
                new ModelSkillDraftProposal(
                        "Java Agent Weekly",
                        "每周整理 Java Agent 前沿，并优先核对官方来源。",
                        "FRIDAY",
                        "09:00",
                        "Asia/Shanghai"),
                "test-v1"));

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
                .andExpect(jsonPath("$.messages[0].content").value("每周五整理 Java Agent 前沿"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.skillDrafts", hasSize(1)))
                .andExpect(jsonPath("$.skillDrafts[0].title").value("Java Agent Weekly"))
                .andExpect(jsonPath("$.skillDrafts[0].dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.skillDrafts[0].status").value("PENDING_CONFIRMATION"));

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].content").value("每周五整理 Java Agent 前沿"))
                .andExpect(jsonPath("$.skillDrafts", hasSize(1)))
                .andExpect(jsonPath("$.skillDrafts[0].objective")
                        .value("每周整理 Java Agent 前沿，并优先核对官方来源。"));
    }

    @Test
    void keepsTheUserMessageAndReturnsSafeFeedbackWhenTheModelFails() throws Exception {
        when(modelPort.analyze(anyString())).thenThrow(new ModelProcessingException("provider timeout"));
        String location = mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String conversationId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"每周整理前沿\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[1].content")
                        .value("消息已保存，但 AI 草案暂时不可用：模型服务可能异常，或结果没有通过校验。你可以稍后重试；系统不会据此创建长期任务。"))
                .andExpect(jsonPath("$.skillDrafts", hasSize(0)));
    }

    @Test
    void blocksAnUnverifiedSearchAnswerEvenWhenTheModelReturnsOne() throws Exception {
        when(modelPort.analyze(anyString())).thenReturn(new ModelDecision(
                ConversationIntent.SEARCH,
                "这是模型尝试生成的搜索答案，不应直接展示。",
                null,
                "test-v1"));
        String location = mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String conversationId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"搜索最新的 Spring AI 发布\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[1].content")
                        .value("我识别到这是一次搜索需求。可靠来源检索将在下一阶段接入，在此之前我不会生成未经核验的搜索结论。"))
                .andExpect(jsonPath("$.skillDrafts", hasSize(0)));
    }

    @Test
    void rejectsAWellFormedModelResponseThatBreaksScheduleRules() throws Exception {
        when(modelPort.analyze(anyString())).thenReturn(new ModelDecision(
                ConversationIntent.RECURRING_SKILL,
                "模型认为草案有效。",
                new ModelSkillDraftProposal(
                        "无效计划",
                        "这份 JSON 结构正确，但星期值不符合业务规则。",
                        "FUNDAY",
                        "09:00",
                        "Asia/Shanghai"),
                "test-v1"));
        String location = mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String conversationId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"每个 FUNDAY 提醒我\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[1].content")
                        .value("消息已保存，但 AI 草案暂时不可用：模型服务可能异常，或结果没有通过校验。你可以稍后重试；系统不会据此创建长期任务。"))
                .andExpect(jsonPath("$.skillDrafts", hasSize(0)));
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

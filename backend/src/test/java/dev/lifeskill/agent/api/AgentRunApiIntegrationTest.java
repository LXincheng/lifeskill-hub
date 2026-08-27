package dev.lifeskill.agent.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.agent.application.port.OfficialSourcePort;
import dev.lifeskill.skill.application.port.SkillRepository;
import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillStatus;
import dev.lifeskill.skill.domain.SkillVersion;
import dev.lifeskill.skill.domain.WeeklySchedule;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentRunApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SkillRepository skillRepository;

    @MockitoBean AgentModelPort model;
    @MockitoBean OfficialSourcePort source;

    private UUID skillId;

    @BeforeEach
    void setUpAgentTablesAndSkill() {
        jdbc.execute("""
                create table if not exists skill_run (
                    id uuid primary key, skill_id uuid not null, skill_version integer not null, status varchar(24) not null,
                    started_at timestamp with time zone, completed_at timestamp with time zone, error_code varchar(80),
                    created_at timestamp with time zone not null, audit_id uuid not null, trigger_type varchar(24) not null,
                    schedule_slot varchar(80), max_steps integer not null, step_count integer not null,
                    timeout_at timestamp with time zone, duration_ms bigint, failure_summary varchar(1000))
                """);
        jdbc.execute("""
                create table if not exists agent_step (
                    id uuid primary key, skill_run_id uuid not null, step_order integer not null, role varchar(32) not null,
                    status varchar(24) not null, input_summary varchar(1000), output_summary varchar(1000), tool_name varchar(100),
                    duration_ms bigint, created_at timestamp with time zone not null, event_type varchar(48) not null,
                    source_url varchar(1000), error_summary varchar(1000), completed_at timestamp with time zone)
                """);
        jdbc.execute("""
                create table if not exists evidence (
                    id uuid primary key, skill_run_id uuid, source_type varchar(40) not null, source_name varchar(160) not null,
                    source_url varchar(1000) not null, external_id varchar(240), title varchar(1000), excerpt varchar(5000),
                    published_at timestamp with time zone, fetched_at timestamp with time zone not null, content_hash varchar(64) not null,
                    metadata_json varchar(1000), raw_content varchar(10000), official_source boolean not null)
                """);
        jdbc.execute("""
                create table if not exists claim (
                    id uuid primary key, skill_run_id uuid not null, statement varchar(2000) not null,
                    verification_status varchar(32) not null, confidence numeric(4,3), created_at timestamp with time zone not null,
                    verification_summary varchar(1000), verified_at timestamp with time zone)
                """);
        jdbc.execute("create table if not exists claim_evidence (claim_id uuid not null, evidence_id uuid not null, primary key (claim_id, evidence_id))");

        skillId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T08:00:00Z");
        skillRepository.save(
                new Skill(skillId, draftId, "Java Agent Weekly", "每周整理 Spring AI 官方更新", SkillStatus.ACTIVE, 1, now, now),
                new SkillVersion(UUID.randomUUID(), skillId, 1, "每周整理 Spring AI 官方更新",
                        new WeeklySchedule(DayOfWeek.FRIDAY, LocalTime.of(9, 0), ZoneId.of("Asia/Shanghai")), now));

        when(source.collect(any())).thenReturn(List.of(new OfficialSourcePort.OfficialSourceDocument(
                "GITHUB_RELEASE", "Spring AI GitHub Releases",
                "https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0", "v2.0.0",
                "Spring AI 2.0.0", "Official notes", "Official notes", now, now, "a".repeat(64), true)));
        when(model.research(anyString(), any())).thenAnswer(invocation -> {
            List<dev.lifeskill.agent.domain.Evidence> evidence = invocation.getArgument(1);
            return new AgentModelPort.ResearchResult("Spring AI 发布了 2.0.0。", List.of(evidence.getFirst().id().toString()));
        });
        when(model.verify(anyString(), any(), any())).thenAnswer(invocation -> {
            dev.lifeskill.agent.domain.Claim claim = invocation.getArgument(1);
            return new AgentModelPort.VerificationResult(
                    true, 0.96, "官方 Release 直接支持该结论。",
                    claim.evidenceIds().stream().map(UUID::toString).toList());
        });
        when(model.compose(anyString(), any(), any())).thenReturn(new AgentModelPort.CompositionResult(
                "Spring AI 2.0.0 官方发布", "官方 Release 已发布并完成独立核验。", "Java Agent", "值得更新版本认知。"));
        when(model.composeLearning(any(), any())).thenReturn(new AgentModelPort.LearningResult(
                "Spring AI 官方更新", "来自已核验动态", "学习路径", "1. 阅读 Release\n2. 验证变化",
                "版本解读", "基于官方 Release 的结构化解读。", "理解测验", "1. 来源是什么？\nA. 官方 Release\nB. 论坛\n答案：A"));
    }

    @Test
    void runsOfficialEvidencePipelinePublishesPulseAndGeneratesLearningBundle() throws Exception {
        String started = mockMvc.perform(post("/api/skills/{skillId}/runs", skillId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(started, "$.id");

        String run = waitForTerminalRun(runId);
        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(run, "$.status")).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(JsonPath.<Integer>read(run, "$.stepCount")).isEqualTo(7);

        String pulseResponse = mockMvc.perform(get("/api/pulse-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$[0].sourceCount").value(1))
                .andReturn().getResponse().getContentAsString();
        String pulseId = JsonPath.read(pulseResponse, "$[0].id");

        mockMvc.perform(get("/api/pulse-items/{pulseId}/evidence", pulseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].officialSource").value(true));

        String firstBundle = mockMvc.perform(post("/api/pulse-items/{pulseId}/learning-folder", pulseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentItems", hasSize(3)))
                .andExpect(jsonPath("$.contentItems[0].verificationStatus").value("VERIFIED"))
                .andReturn().getResponse().getContentAsString();
        String folderId = JsonPath.read(firstBundle, "$.folder.id");

        mockMvc.perform(post("/api/pulse-items/{pulseId}/learning-folder", pulseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folder.id").value(folderId))
                .andExpect(jsonPath("$.contentItems", hasSize(3)));
    }

    private String waitForTerminalRun(String runId) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            String body = mockMvc.perform(get("/api/skill-runs/{runId}", runId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String status = JsonPath.read(body, "$.status");
            if (List.of("COMPLETED", "BLOCKED", "FAILED", "TIMED_OUT").contains(status)) return body;
            Thread.sleep(25);
        }
        throw new AssertionError("AgentRun did not finish in time");
    }
}

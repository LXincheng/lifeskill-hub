package dev.lifeskill.integration.deepseek;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;

@Component
@ConditionalOnProperty(name = "lifeskill.model.enabled", havingValue = "true")
public class DeepSeekAgentModelAdapter implements AgentModelPort {
    static final String PROMPT_VERSION = "m2-official-research-v1";

    private final ChatClient researcher;
    private final ChatClient verifier;
    private final ChatClient composer;
    private final ChatClient learningPathComposer;
    private final ChatClient learningArticleComposer;
    private final ChatClient learningQuizComposer;
    private final ObjectMapper objectMapper;

    public DeepSeekAgentModelAdapter(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.researcher = builder.clone().defaultSystem("""
                你是 Researcher。只基于输入的官方 Evidence 生成一条可核验事实 Claim。
                evidenceIds 必须至少包含一个输入中真实存在的 UUID，不能编造 URL、版本、日期或 ID。
                statement 使用简洁中文，保留准确的版本号和事实。只返回结构化结果，不输出思维链。
                """).build();
        this.verifier = builder.clone().defaultSystem("""
                你是独立 Verifier。重新阅读 Claim 与官方 Evidence，判断 Evidence 是否直接支持 Claim。
                不沿用 Researcher 的判断；只有完全被材料支持才 verified=true。
                confidence 为 0 到 1；evidenceIds 只能引用输入里的 UUID；summary 说明可公开的核验依据，不输出思维链。
                """).build();
        this.composer = builder.clone().defaultSystem("""
                你是 Composer。把已核验 Claim 整理成一条中文动态。
                title 不超过 80 字，summary 不超过 300 字，category 固定为 Java Agent，
                recommendationReason 解释这条官方更新为何值得用户阅读。不得增加 Evidence 中没有的事实，不输出思维链。
                """).build();
        this.learningPathComposer = builder.clone().defaultSystem("""
                你是学习路径 Composer。只根据已核验 Claim 和官方 Evidence 返回 title 与 body。
                body 必须正好由 3-5 行组成，每行格式为“[ ] 一项完整学习任务”，不要标题、空行或续行。
                不得扩写材料外事实，不输出思维链。
                """).build();
        this.learningArticleComposer = builder.clone().defaultSystem("""
                你是学习文章 Composer。只根据已核验 Claim 和官方 Evidence 返回 title 与 body。
                body 解释更新、背景、影响与实践建议，并保留输入中的官方来源链接。不得扩写材料外事实，不输出思维链。
                """).build();
        this.learningQuizComposer = builder.clone().defaultSystem("""
                你是测验 Composer。只根据已核验 Claim 和官方 Evidence 返回 title 与 body。
                body 包含 3 道选择题。每题严格使用四行：题目、两个以“- ”开头的选项、“答案: 1”或“答案: 2”；
                题与题之间只用一行“---”分隔。不得增加标题或统一答案区，不得扩写材料外事实，不输出思维链。
                """).build();
    }

    @Override
    public ResearchResult research(String objective, List<Evidence> evidence) {
        ResearchResponse response = call(researcher, "目标：\n" + objective + "\nEvidence：\n" + evidenceJson(evidence), ResearchResponse.class);
        requireText(response.statement(), "Researcher statement");
        requireIds(response.evidenceIds());
        return new ResearchResult(response.statement().trim(), response.evidenceIds());
    }

    @Override
    public VerificationResult verify(String objective, Claim claim, List<Evidence> evidence) {
        VerificationResponse response = call(verifier,
                "目标：\n" + objective + "\nClaim：\n" + claim.statement() + "\nEvidence：\n" + evidenceJson(evidence),
                VerificationResponse.class);
        requireText(response.summary(), "Verifier summary");
        requireIds(response.evidenceIds());
        if (response.confidence() < 0 || response.confidence() > 1) {
            throw new IllegalStateException("Verifier confidence is outside 0..1");
        }
        return new VerificationResult(
                response.verified(), response.confidence(), response.summary().trim(), response.evidenceIds());
    }

    @Override
    public CompositionResult compose(String objective, Claim claim, List<Evidence> evidence) {
        CompositionResponse response = call(composer,
                "目标：\n" + objective + "\n已核验 Claim：\n" + claim.statement() + "\nEvidence：\n" + evidenceJson(evidence),
                CompositionResponse.class);
        requireText(response.title(), "Pulse title");
        requireText(response.summary(), "Pulse summary");
        requireText(response.recommendationReason(), "Recommendation reason");
        return new CompositionResult(
                response.title().trim(), response.summary().trim(), "Java Agent", response.recommendationReason().trim());
    }

    @Override
    public LearningResult composeLearning(Claim claim, List<Evidence> evidence) {
        String input = "已核验 Claim：\n" + claim.statement() + "\nEvidence：\n" + evidenceJson(evidence);
        ContentResponse path = call(learningPathComposer, input, ContentResponse.class);
        ContentResponse article = call(learningArticleComposer, input, ContentResponse.class);
        ContentResponse quiz = call(learningQuizComposer, input, ContentResponse.class);
        for (String value : List.of(path.title(), path.body(), article.title(), article.body(), quiz.title(), quiz.body())) {
            requireText(value, "Learning content field");
        }
        return new LearningResult(
                "Java Agent Weekly", "由已核验官方更新生成，所有结论可回到 Evidence。",
                path.title().trim(), path.body().trim(), article.title().trim(), article.body().trim(),
                quiz.title().trim(), quiz.body().trim());
    }

    private <T> T call(ChatClient client, String prompt, Class<T> responseType) {
        try {
            T response = client.prompt().user(prompt).call().entity(responseType, spec -> spec.validateSchema());
            if (response == null) throw new IllegalStateException("DeepSeek returned no structured response");
            return response;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("DeepSeek " + PROMPT_VERSION + " request failed", exception);
        }
    }

    private String evidenceJson(List<Evidence> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence.stream().map(item -> new EvidencePrompt(
                    item.id().toString(), item.title(), item.sourceUrl(), item.publishedAt(), item.excerpt())).toList());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize Evidence for the model", exception);
        }
    }

    private void requireIds(List<String> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("Model must cite at least one Evidence ID");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException(field + " is required");
    }

    record EvidencePrompt(String id, String title, String url, java.time.Instant publishedAt, String excerpt) {}
    record ResearchResponse(String statement, List<String> evidenceIds) {}
    record VerificationResponse(boolean verified, double confidence, String summary, List<String> evidenceIds) {}
    record CompositionResponse(String title, String summary, String category, String recommendationReason) {}
    record ContentResponse(String title, String body) {}
}

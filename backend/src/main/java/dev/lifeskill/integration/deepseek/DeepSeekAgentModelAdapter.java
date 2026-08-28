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
    private final ChatClient reportComposer;
    private final ChatClient learningPathComposer;
    private final ChatClient learningArticleComposer;
    private final ChatClient learningQuizComposer;
    private final ChatClient personalLearningComposer;
    private final ChatClient personalPathComposer;
    private final ChatClient personalArticleComposer;
    private final ChatClient personalQuizComposer;
    private final ChatClient learningRevisionComposer;
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
                title 不超过 80 字，summary 不超过 300 字，category 根据材料使用“Java Agent”或“黄金研究”，
                recommendationReason 解释这条官方更新为何值得用户阅读。不得增加 Evidence 中没有的事实，不输出思维链。
                """).build();
        this.reportComposer = builder.clone().defaultSystem("""
                你是资深贵金属研究报告 Composer。只使用已核验 Claim 和 World Gold Council 官方 Evidence，
                输出清晰、克制、可追溯的中文研究报告，不提供个性化投资建议，不预测确定收益。
                title 使用专业报告标题。body 必须按以下 Markdown 顺序输出：
                > 一段不超过 180 字的执行摘要
                ## 01 核心观点
                3-5 条编号观点，每条包含判断与证据依据
                ## 02 关键数据与判断
                Markdown 表格，列为“观察指标 | 最新事实 | 方向 | Evidence”，Evidence 单元格写真实 UUID
                ## 03 风险提示
                3-5 条风险，明确什么变化会使当前判断失效
                ## 04 可验证的观察清单
                3-6 行“- 日期或窗口 | 事件 | 需要观察什么”
                ## 05 结论
                给出条件式结论和下一次复核条件
                ## 06 官方来源
                每条写“[Evidence UUID] 标题 | 发布时间 | URL”。
                不得编造价格、日期、URL、机构观点或 Evidence ID，不输出思维链。
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
        this.personalLearningComposer = builder.clone().defaultSystem("""
                你是个人课程设计师。根据用户学习目标返回简洁的 folderName 和 folderDescription。
                不查询或声称掌握最新外部事实，内容使用中文，不输出思维链。
                """).build();
        this.personalPathComposer = builder.clone().defaultSystem("""
                你是学习路径设计师。根据用户目标返回 title 和 body。
                body 必须由 4-8 行组成，每行严格使用“[ ] 一项清晰任务”，覆盖理解、实践和复盘。
                不查询或编造最新外部事实，使用中文，不输出思维链。
                """).build();
        this.personalArticleComposer = builder.clone().defaultSystem("""
                你是学习导读作者。根据用户目标返回 title 和 body。
                body 是一篇结构清楚的起步指南，可使用“## ”二级标题，但不得编造版本、价格、日期或来源。
                内容使用中文，不输出思维链。
                """).build();
        this.personalQuizComposer = builder.clone().defaultSystem("""
                你是学习测验设计师。根据用户目标返回 title 和 body。
                body 必须包含 3-5 道选择题；每题由题目、两个以“- ”开头的选项和“答案: 1/2”组成，题间用“---”分隔。
                内容使用中文，不查询或编造最新外部事实，不输出思维链。
                """).build();
        this.learningRevisionComposer = builder.clone().defaultSystem("""
                你是学习内容修订师。根据原内容和用户反馈返回新的 title 与 body。
                保留正确且有用的内容，明确修复反馈指出的问题；不要声称新增内容已经过官方 Evidence 核验。
                使用结构清楚的中文 Markdown：标题、段落、列表、引用卡片、代码块和带描述文字的链接。
                若内容类型是 LEARNING_PATH，body 必须包含 3-10 个以“- [ ] ”开头的独立步骤。
                若内容类型是 QUIZ，body 必须包含 3-8 道题，每题使用两个“- ”选项和“答案: 1/2”，题间用“---”分隔。
                不得输出思维链、HTML、裸露长 URL 或 Markdown 教程说明。
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
        requireText(response.category(), "Pulse category");
        String category = response.category().trim();
        if (!List.of("Java Agent", "黄金研究").contains(category)) {
            throw new IllegalStateException("Pulse category is outside the supported set");
        }
        return new CompositionResult(
                response.title().trim(), response.summary().trim(), category, response.recommendationReason().trim());
    }

    @Override
    public ReportResult composeReport(String objective, Claim claim, List<Evidence> evidence) {
        ContentResponse response = call(reportComposer,
                "研究目标：\n" + objective + "\n已核验 Claim：\n" + claim.statement()
                        + "\nEvidence：\n" + evidenceJson(evidence), ContentResponse.class);
        requireText(response.title(), "Report title");
        requireText(response.body(), "Report body");
        return new ReportResult(response.title().trim(), response.body().trim());
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

    @Override
    public LearningResult composePersonalLearning(String objective) {
        String input = "学习目标：\n" + objective;
        FolderResponse folder = call(personalLearningComposer, input, FolderResponse.class);
        ContentResponse path = call(personalPathComposer, input, ContentResponse.class);
        ContentResponse article = call(personalArticleComposer, input, ContentResponse.class);
        ContentResponse quiz = call(personalQuizComposer, input, ContentResponse.class);
        List<String> values = List.of(
                folder.folderName(), folder.folderDescription(), path.title(), path.body(),
                article.title(), article.body(), quiz.title(), quiz.body());
        values.forEach(value -> requireText(value, "Personal learning field"));
        long pathUnits = path.body().lines().filter(line -> line.trim().startsWith("[ ]")).count();
        long quizUnits = java.util.regex.Pattern.compile("(?m)^(?:答案|answer)\\s*[:：]\\s*[12]\\s*$",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(quiz.body()).results().count();
        if (pathUnits < 4 || pathUnits > 8 || quizUnits < 3 || quizUnits > 5) {
            throw new IllegalStateException("Personal learning plan does not satisfy the deterministic content contract");
        }
        return new LearningResult(
                folder.folderName().trim(), folder.folderDescription().trim(),
                path.title().trim(), path.body().trim(), article.title().trim(),
                article.body().trim(), quiz.title().trim(), quiz.body().trim());
    }

    @Override
    public ContentRevision reviseLearningContent(String type, String title, String body, String feedback) {
        ContentResponse response = call(learningRevisionComposer,
                "内容类型：" + type + "\n原标题：" + title + "\n用户反馈：\n" + feedback + "\n原内容：\n" + body,
                ContentResponse.class);
        requireText(response.title(), "Revised learning title");
        requireText(response.body(), "Revised learning body");
        if (response.body().length() > 20_000) throw new IllegalStateException("Revised learning content is too long");
        return new ContentRevision(response.title().trim(), response.body().trim());
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
    record FolderResponse(String folderName, String folderDescription) {}
}

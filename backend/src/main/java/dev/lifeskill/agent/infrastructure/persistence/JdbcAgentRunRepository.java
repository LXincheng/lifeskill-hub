package dev.lifeskill.agent.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.lifeskill.agent.application.port.AgentRunRepository;
import dev.lifeskill.agent.domain.AgentRun;
import dev.lifeskill.agent.domain.AgentRunStatus;
import dev.lifeskill.agent.domain.AgentStep;
import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;

@Repository
public class JdbcAgentRunRepository implements AgentRunRepository {
    private final JdbcTemplate jdbc;

    public JdbcAgentRunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AgentRun create(
            UUID runId, UUID skillId, int version, UUID auditId, String triggerType,
            String scheduleSlot, int maxSteps, Instant startedAt, Instant timeoutAt) {
        jdbc.update("""
                insert into skill_run (
                    id, skill_id, skill_version, status, audit_id, trigger_type, schedule_slot,
                    max_steps, step_count, started_at, timeout_at, created_at)
                values (?, ?, ?, 'RECEIVED', ?, ?, ?, ?, 0, ?, ?, ?)
                """, runId, skillId, version, auditId, triggerType, scheduleSlot, maxSteps,
                dbTime(startedAt), dbTime(timeoutAt), dbTime(startedAt));
        return requireRun(runId);
    }

    @Override
    public AgentRun createResearch(
            UUID runId, UUID conversationId, UUID sourceMessageId, String objective, String capability,
            UUID auditId, int maxSteps, Instant startedAt, Instant timeoutAt) {
        jdbc.update("""
                insert into skill_run (
                    id, skill_id, skill_version, conversation_id, source_message_id, objective, capability,
                    status, audit_id, trigger_type, max_steps, step_count, started_at, timeout_at, created_at)
                values (?, null, 0, ?, ?, ?, ?, 'RECEIVED', ?, 'CONVERSATION_RESEARCH', ?, 0, ?, ?, ?)
                """, runId, conversationId, sourceMessageId, objective, capability, auditId, maxSteps,
                dbTime(startedAt), dbTime(timeoutAt), dbTime(startedAt));
        return requireRun(runId);
    }

    @Override
    public Optional<AgentRun> findRun(UUID runId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("select * from skill_run where id = ?", this::mapRun, runId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AgentRun> findLatestBySkill(UUID skillId) {
        return jdbc.query("select * from skill_run where skill_id = ? order by created_at desc limit 1", this::mapRun, skillId)
                .stream().findFirst();
    }

    @Override
    public List<AgentStep> findSteps(UUID runId) {
        return jdbc.query("select * from agent_step where skill_run_id = ? order by step_order", this::mapStep, runId);
    }

    @Override
    public AgentStep addStep(
            UUID stepId, UUID runId, int order, String role, String status, String eventType,
            String inputSummary, String outputSummary, String toolName, String sourceUrl,
            Long durationMs, String errorSummary, Instant createdAt, Instant completedAt,
            AgentRunStatus runStatus) {
        jdbc.update("""
                insert into agent_step (
                    id, skill_run_id, step_order, role, status, event_type, input_summary, output_summary,
                    tool_name, source_url, duration_ms, error_summary, created_at, completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, stepId, runId, order, role, status, eventType, inputSummary, outputSummary,
                toolName, sourceUrl, durationMs, errorSummary, dbTime(createdAt), dbTime(completedAt));
        jdbc.update("update skill_run set status = ?, step_count = ? where id = ?",
                runStatus.name(), order, runId);
        return findSteps(runId).stream().filter(step -> step.id().equals(stepId)).findFirst().orElseThrow();
    }

    @Override
    public Evidence saveEvidence(Evidence evidence) {
        List<Evidence> existing = jdbc.query(
                "select * from evidence where source_url = ? and content_hash = ?",
                this::mapEvidence, evidence.sourceUrl(), evidence.contentHash());
        if (!existing.isEmpty()) return existing.getFirst();
        jdbc.update("""
                insert into evidence (
                    id, skill_run_id, source_type, source_name, source_url, external_id, title, excerpt,
                    raw_content, published_at, fetched_at, content_hash, official_source, metadata_json)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}')
                """, evidence.id(), evidence.runId(), evidence.sourceType(), evidence.sourceName(), evidence.sourceUrl(),
                evidence.externalId(), evidence.title(), evidence.excerpt(), evidence.rawContent(), dbTime(evidence.publishedAt()),
                dbTime(evidence.fetchedAt()), evidence.contentHash(), evidence.officialSource());
        return evidence;
    }

    @Override
    public Claim saveClaim(Claim claim) {
        jdbc.update("""
                insert into claim (
                    id, skill_run_id, statement, verification_status, confidence, verification_summary, created_at, verified_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, claim.id(), claim.runId(), claim.statement(), claim.verificationStatus(), claim.confidence(),
                claim.verificationSummary(), dbTime(claim.createdAt()), dbTime(claim.verifiedAt()));
        claim.evidenceIds().forEach(evidenceId -> jdbc.update(
                "insert into claim_evidence (claim_id, evidence_id) values (?, ?)", claim.id(), evidenceId));
        return claim;
    }

    @Override
    public Claim updateVerification(UUID claimId, String status, double confidence, String summary, Instant verifiedAt) {
        jdbc.update("""
                update claim set verification_status = ?, confidence = ?, verification_summary = ?, verified_at = ?
                where id = ?
                """, status, confidence, summary, dbTime(verifiedAt), claimId);
        return findClaim(claimId).orElseThrow();
    }

    @Override
    public void finish(UUID runId, AgentRunStatus status, Instant completedAt, long durationMs, String failureSummary) {
        jdbc.update("""
                update skill_run set status = ?, completed_at = ?, duration_ms = ?, failure_summary = ? where id = ?
                """, status.name(), dbTime(completedAt), durationMs, failureSummary, runId);
    }

    @Override
    public void attachResultContent(UUID runId, UUID contentId) {
        jdbc.update("update skill_run set result_content_id = ? where id = ?", contentId, runId);
    }

    @Override
    public List<Evidence> findEvidenceForClaim(UUID claimId) {
        return jdbc.query("""
                select e.* from evidence e
                join claim_evidence ce on ce.evidence_id = e.id
                where ce.claim_id = ? order by e.published_at desc nulls last
                """, this::mapEvidence, claimId);
    }

    @Override
    public Optional<Claim> findClaim(UUID claimId) {
        List<UUID> evidenceIds = jdbc.query(
                "select evidence_id from claim_evidence where claim_id = ?",
                (result, row) -> result.getObject(1, UUID.class), claimId);
        return jdbc.query("select * from claim where id = ?", (result, row) -> mapClaim(result, evidenceIds), claimId)
                .stream().findFirst();
    }

    private AgentRun requireRun(UUID runId) {
        return findRun(runId).orElseThrow(() -> new IllegalStateException("Agent run was not persisted"));
    }

    private AgentRun mapRun(ResultSet result, int row) throws SQLException {
        return new AgentRun(
                result.getObject("id", UUID.class), result.getObject("skill_id", UUID.class),
                result.getInt("skill_version"), result.getObject("conversation_id", UUID.class),
                result.getObject("source_message_id", UUID.class), result.getString("objective"),
                result.getString("capability"), result.getObject("result_content_id", UUID.class),
                result.getObject("audit_id", UUID.class), result.getString("trigger_type"),
                AgentRunStatus.valueOf(result.getString("status")), result.getInt("max_steps"), result.getInt("step_count"),
                instant(result, "started_at"), instant(result, "timeout_at"), instant(result, "completed_at"),
                nullableLong(result, "duration_ms"), result.getString("failure_summary"), instant(result, "created_at"));
    }

    private AgentStep mapStep(ResultSet result, int row) throws SQLException {
        return new AgentStep(
                result.getObject("id", UUID.class), result.getObject("skill_run_id", UUID.class), result.getInt("step_order"),
                result.getString("role"), result.getString("status"), result.getString("event_type"),
                result.getString("input_summary"), result.getString("output_summary"), result.getString("tool_name"),
                result.getString("source_url"), nullableLong(result, "duration_ms"), result.getString("error_summary"),
                instant(result, "created_at"), instant(result, "completed_at"));
    }

    private Evidence mapEvidence(ResultSet result, int row) throws SQLException {
        return new Evidence(
                result.getObject("id", UUID.class), result.getObject("skill_run_id", UUID.class),
                result.getString("source_type"), result.getString("source_name"), result.getString("source_url"),
                result.getString("external_id"), result.getString("title"), result.getString("excerpt"),
                result.getString("raw_content"), instant(result, "published_at"), instant(result, "fetched_at"),
                result.getString("content_hash"), result.getBoolean("official_source"));
    }

    private Claim mapClaim(ResultSet result, List<UUID> evidenceIds) throws SQLException {
        Number confidence = (Number) result.getObject("confidence");
        return new Claim(
                result.getObject("id", UUID.class), result.getObject("skill_run_id", UUID.class),
                result.getString("statement"), evidenceIds, result.getString("verification_status"),
                confidence == null ? 0 : confidence.doubleValue(), result.getString("verification_summary"),
                instant(result, "created_at"), instant(result, "verified_at"));
    }

    private Instant instant(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private java.time.OffsetDateTime dbTime(Instant value) {
        return value == null ? null : java.time.OffsetDateTime.ofInstant(value, java.time.ZoneOffset.UTC);
    }
}

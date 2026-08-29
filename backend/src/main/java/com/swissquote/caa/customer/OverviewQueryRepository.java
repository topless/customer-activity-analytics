package com.swissquote.caa.customer;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.swissquote.caa.customer.CustomerDtos.CurrencyTotalDto;
import com.swissquote.caa.customer.CustomerDtos.StatusCountDto;
import com.swissquote.caa.customer.CustomerDtos.TriggeredRuleDto;
import com.swissquote.caa.customer.CustomerDtos.TypeBreakdownDto;

/** SQL aggregations behind GET /api/customers/{id}/overview. */
@Repository
public class OverviewQueryRepository {

    private final JdbcTemplate jdbc;

    public OverviewQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Window(Instant from, Instant to, long transactionCount) {
    }

    public Window window(UUID customerId) {
        return jdbc.queryForObject("""
                select min(created_at) as w_from, max(created_at) as w_to, count(*) as tx_count
                from transactions where customer_id = ?
                """,
            (rs, i) -> {
                Timestamp from = rs.getObject("w_from", Timestamp.class);
                Timestamp to = rs.getObject("w_to", Timestamp.class);
                return new Window(from == null ? null : from.toInstant(),
                    to == null ? null : to.toInstant(), rs.getLong("tx_count"));
            },
            customerId);
    }

    public List<TypeBreakdownDto> byType(UUID customerId) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        jdbc.query("""
                select activity_type, count(*) as cnt,
                       count(*) filter (where status = 'FAILED') as failed_cnt
                from transactions where customer_id = ?
                group by activity_type order by activity_type
                """,
            rs -> {
                counts.put(rs.getString("activity_type"),
                    new long[]{rs.getLong("cnt"), rs.getLong("failed_cnt")});
            },
            customerId);

        Map<String, List<CurrencyTotalDto>> totals = new LinkedHashMap<>();
        jdbc.query("""
                select activity_type, currency, sum(amount) as total
                from transactions where customer_id = ?
                group by activity_type, currency
                order by activity_type, sum(amount) desc
                """,
            rs -> {
                totals.computeIfAbsent(rs.getString("activity_type"), k -> new ArrayList<>())
                    .add(new CurrencyTotalDto(rs.getString("currency"), rs.getBigDecimal("total")));
            },
            customerId);

        return counts.entrySet().stream()
            .map(e -> new TypeBreakdownDto(e.getKey(), e.getValue()[0], e.getValue()[1],
                totals.getOrDefault(e.getKey(), List.of())))
            .toList();
    }

    public List<StatusCountDto> byStatus(UUID customerId) {
        return jdbc.query("""
                select status, count(*) as cnt from transactions
                where customer_id = ? group by status order by count(*) desc
                """,
            (rs, i) -> new StatusCountDto(rs.getString("status"), rs.getLong("cnt")),
            customerId);
    }

    /** month (yyyy-MM) -> counts per activity type; months with no activity are absent here. */
    public Map<String, Map<String, Long>> monthlyCounts(UUID customerId) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        jdbc.query("""
                select to_char(date_trunc('month', created_at at time zone 'UTC'), 'YYYY-MM') as month,
                       activity_type, count(*) as cnt
                from transactions where customer_id = ?
                group by 1, 2 order by 1
                """,
            rs -> {
                result.computeIfAbsent(rs.getString("month"), k -> new LinkedHashMap<>())
                    .put(rs.getString("activity_type"), rs.getLong("cnt"));
            },
            customerId);
        return result;
    }

    public List<TriggeredRuleDto> triggeredRules(UUID customerId) {
        return jdbc.query("""
                select r.rule_id, r.rule_name, r.applies_to,
                       count(*) as times_triggered,
                       sum(ra.score_contribution) as total_contribution,
                       max(ra.triggered_at) as last_triggered_at
                from risk_assessments ra
                join risk_rules r on r.rule_id = ra.rule_id
                join transactions t on t.transaction_id = ra.transaction_id
                where t.customer_id = ?
                group by r.rule_id, r.rule_name, r.applies_to
                order by sum(ra.score_contribution) desc
                """,
            (rs, i) -> new TriggeredRuleDto(
                rs.getObject("rule_id", UUID.class),
                rs.getString("rule_name"),
                rs.getString("applies_to"),
                rs.getLong("times_triggered"),
                rs.getBigDecimal("total_contribution"),
                rs.getObject("last_triggered_at", Timestamp.class).toInstant()),
            customerId);
    }

    public BigDecimal riskScore(UUID customerId) {
        BigDecimal score = jdbc.queryForObject("""
                select coalesce(sum(ra.score_contribution), 0)
                from risk_assessments ra
                join transactions t on t.transaction_id = ra.transaction_id
                where t.customer_id = ?
                """,
            BigDecimal.class, customerId);
        return score == null ? BigDecimal.ZERO : score;
    }
}

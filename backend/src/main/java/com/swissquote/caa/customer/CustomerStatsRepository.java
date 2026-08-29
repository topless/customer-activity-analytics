package com.swissquote.caa.customer;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Per-customer aggregates for the search listing (SQL GROUP BY — simpler than JPQL here). */
@Repository
public class CustomerStatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CustomerStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TxStats(long count, Instant lastActivityAt) {
    }

    public Map<UUID, TxStats> transactionStats(Collection<UUID> customerIds) {
        Map<UUID, TxStats> result = new HashMap<>();
        if (customerIds.isEmpty()) {
            return result;
        }
        jdbc.query("""
                select customer_id, count(*) as tx_count, max(created_at) as last_activity
                from transactions
                where customer_id in (:ids)
                group by customer_id
                """,
            Map.of("ids", customerIds),
            rs -> {
                result.put(rs.getObject("customer_id", UUID.class),
                    new TxStats(rs.getLong("tx_count"),
                        rs.getObject("last_activity", Timestamp.class).toInstant()));
            });
        return result;
    }

    public Map<UUID, BigDecimal> riskScores(Collection<UUID> customerIds) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (customerIds.isEmpty()) {
            return result;
        }
        jdbc.query("""
                select t.customer_id, sum(ra.score_contribution) as risk_score
                from risk_assessments ra
                join transactions t on t.transaction_id = ra.transaction_id
                where t.customer_id in (:ids)
                group by t.customer_id
                """,
            Map.of("ids", customerIds),
            rs -> {
                result.put(rs.getObject("customer_id", UUID.class), rs.getBigDecimal("risk_score"));
            });
        return result;
    }
}

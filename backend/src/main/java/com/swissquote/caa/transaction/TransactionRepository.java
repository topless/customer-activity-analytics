package com.swissquote.caa.transaction;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Filtered page of a customer's activity, newest first, with the type-specific detail rows
     * fetched in the same query (to-one joins — safe to combine with pagination).
     */
    @Query(value = """
        select t from Transaction t
        left join fetch t.card
        left join fetch t.payment
        left join fetch t.crypto
        where t.customerId = :customerId
          and (:type is null or t.activityType = :type)
          and (:status is null or t.status = :status)
        order by t.createdAt desc, t.id
        """,
        countQuery = """
        select count(t) from Transaction t
        where t.customerId = :customerId
          and (:type is null or t.activityType = :type)
          and (:status is null or t.status = :status)
        """)
    Page<Transaction> findPage(@Param("customerId") UUID customerId,
                               @Param("type") ActivityType type,
                               @Param("status") TransactionStatus status,
                               Pageable pageable);

    /** Full activity of a customer, oldest first, details fetched — input for the AI analysis. */
    @Query("""
        select t from Transaction t
        left join fetch t.card
        left join fetch t.payment
        left join fetch t.crypto
        where t.customerId = :customerId
        order by t.createdAt, t.id
        """)
    List<Transaction> findAllForAnalysis(@Param("customerId") UUID customerId);
}

package com.swissquote.caa.customer;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /** {@code query} must be pre-escaped with '!' for LIKE wildcards (see CustomerService). */
    @Query("""
        select c from Customer c
        where upper(c.customerNumber) like upper(concat(:query, '%')) escape '!'
           or upper(c.fullName) like upper(concat('%', :query, '%')) escape '!'
           or (:id is not null and c.id = :id)
        order by c.customerNumber
        """)
    Page<Customer> search(@Param("query") String query, @Param("id") UUID id, Pageable pageable);
}

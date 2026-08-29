package com.swissquote.caa.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    Optional<Operator> findByUsername(String username);
}

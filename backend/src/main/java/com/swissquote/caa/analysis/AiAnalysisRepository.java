package com.swissquote.caa.analysis;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    List<AiAnalysis> findByCustomerIdOrderByRequestedAtDesc(UUID customerId);
}

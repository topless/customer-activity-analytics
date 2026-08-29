package com.swissquote.caa.analysis;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.swissquote.caa.analysis.AnalysisDtos.AnalysisDetailDto;
import com.swissquote.caa.analysis.AnalysisDtos.AnalysisSummaryDto;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/customers/{customerId}/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisDetailDto run(@PathVariable UUID customerId, @AuthenticationPrincipal Jwt jwt) {
        return analysisService.run(customerId, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/customers/{customerId}/analyses")
    public List<AnalysisSummaryDto> list(@PathVariable UUID customerId) {
        return analysisService.list(customerId);
    }

    @GetMapping("/analyses/{analysisId}")
    public AnalysisDetailDto get(@PathVariable UUID analysisId) {
        return analysisService.get(analysisId);
    }
}

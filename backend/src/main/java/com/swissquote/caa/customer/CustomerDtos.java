package com.swissquote.caa.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerSummaryDto(UUID id, String customerNumber, String fullName, String email,
                                     String country, String kycLevel, BigDecimal riskScore,
                                     long transactionCount, Instant lastActivityAt) {
    }

    public record CustomerDetailDto(UUID id, String customerNumber, String fullName, String email,
                                    String country, String kycLevel, LocalDate dateOfBirth,
                                    Instant onboardedAt) {

        public static CustomerDetailDto of(Customer c) {
            return new CustomerDetailDto(c.getId(), c.getCustomerNumber(), c.getFullName(),
                c.getEmail(), c.getCountry(), c.getKycLevel(), c.getDateOfBirth(), c.getOnboardedAt());
        }
    }

    public record ActivityOverviewDto(BigDecimal riskScore, Instant windowFrom, Instant windowTo,
                                      long transactionCount, List<TypeBreakdownDto> byType,
                                      List<StatusCountDto> byStatus,
                                      List<MonthlyCountDto> monthlyCounts,
                                      List<TriggeredRuleDto> triggeredRules) {
    }

    public record TypeBreakdownDto(String activityType, long count, long failedCount,
                                   List<CurrencyTotalDto> totalsByCurrency) {
    }

    public record CurrencyTotalDto(String currency, BigDecimal totalAmount) {
    }

    public record StatusCountDto(String status, long count) {
    }

    public record MonthlyCountDto(String month, long card, long payment, long crypto) {
    }

    public record TriggeredRuleDto(UUID ruleId, String ruleName, String appliesTo,
                                   long timesTriggered, BigDecimal totalContribution,
                                   Instant lastTriggeredAt) {
    }
}

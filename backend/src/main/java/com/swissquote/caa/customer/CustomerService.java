package com.swissquote.caa.customer;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swissquote.caa.common.NotFoundException;
import com.swissquote.caa.common.PageDto;
import com.swissquote.caa.customer.CustomerDtos.ActivityOverviewDto;
import com.swissquote.caa.customer.CustomerDtos.CustomerDetailDto;
import com.swissquote.caa.customer.CustomerDtos.CustomerSummaryDto;
import com.swissquote.caa.customer.CustomerDtos.MonthlyCountDto;
import com.swissquote.caa.customer.CustomerStatsRepository.TxStats;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerStatsRepository stats;
    private final OverviewQueryRepository overviewQueries;

    public CustomerService(CustomerRepository customers, CustomerStatsRepository stats,
                           OverviewQueryRepository overviewQueries) {
        this.customers = customers;
        this.stats = stats;
        this.overviewQueries = overviewQueries;
    }

    public PageDto<CustomerSummaryDto> search(String query, int page, int size) {
        String q = query == null ? "" : query.trim();
        UUID asUuid = tryParseUuid(q);
        Page<Customer> result = customers.search(q, asUuid, PageRequest.of(page, size));

        List<UUID> ids = result.getContent().stream().map(Customer::getId).toList();
        Map<UUID, TxStats> txStats = stats.transactionStats(ids);
        Map<UUID, BigDecimal> riskScores = stats.riskScores(ids);

        List<CustomerSummaryDto> content = result.getContent().stream()
            .map(c -> {
                TxStats s = txStats.get(c.getId());
                return new CustomerSummaryDto(c.getId(), c.getCustomerNumber(), c.getFullName(),
                    c.getEmail(), c.getCountry(), c.getKycLevel(),
                    riskScores.getOrDefault(c.getId(), BigDecimal.ZERO),
                    s == null ? 0 : s.count(),
                    s == null ? null : s.lastActivityAt());
            })
            .toList();
        return PageDto.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public CustomerDetailDto get(UUID id) {
        return CustomerDetailDto.of(require(id));
    }

    public Customer require(UUID id) {
        return customers.findById(id)
            .orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
    }

    public ActivityOverviewDto overview(UUID id) {
        require(id);
        OverviewQueryRepository.Window window = overviewQueries.window(id);
        return new ActivityOverviewDto(
            overviewQueries.riskScore(id),
            window.from(), window.to(), window.transactionCount(),
            overviewQueries.byType(id),
            overviewQueries.byStatus(id),
            zeroFilledMonths(window, overviewQueries.monthlyCounts(id)),
            overviewQueries.triggeredRules(id));
    }

    /** Expands the sparse month/type counts into one row per month of the window, zero-filled. */
    private static List<MonthlyCountDto> zeroFilledMonths(OverviewQueryRepository.Window window,
                                                          Map<String, Map<String, Long>> sparse) {
        List<MonthlyCountDto> months = new ArrayList<>();
        if (window.from() == null) {
            return months;
        }
        YearMonth first = YearMonth.from(window.from().atZone(ZoneOffset.UTC));
        YearMonth last = YearMonth.from(window.to().atZone(ZoneOffset.UTC));
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            Map<String, Long> counts = sparse.getOrDefault(m.toString(), Map.of());
            months.add(new MonthlyCountDto(m.toString(),
                counts.getOrDefault("CARD", 0L),
                counts.getOrDefault("PAYMENT", 0L),
                counts.getOrDefault("CRYPTO", 0L)));
        }
        return months;
    }

    private static UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

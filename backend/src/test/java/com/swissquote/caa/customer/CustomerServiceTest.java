package com.swissquote.caa.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.swissquote.caa.common.NotFoundException;
import com.swissquote.caa.customer.CustomerDtos.ActivityOverviewDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private CustomerRepository customers;
    @Mock
    private CustomerStatsRepository stats;
    @Mock
    private OverviewQueryRepository overviewQueries;

    @Test
    void overviewZeroFillsMonthsAcrossTheWindow() {
        when(customers.findById(ID)).thenReturn(Optional.of(customer()));
        when(overviewQueries.window(ID)).thenReturn(new OverviewQueryRepository.Window(
            Instant.parse("2026-03-15T10:00:00Z"), Instant.parse("2026-06-02T10:00:00Z"), 7));
        when(overviewQueries.riskScore(ID)).thenReturn(BigDecimal.ZERO);
        when(overviewQueries.byType(ID)).thenReturn(List.of());
        when(overviewQueries.byStatus(ID)).thenReturn(List.of());
        Map<String, Map<String, Long>> sparse = new LinkedHashMap<>();
        sparse.put("2026-03", Map.of("CARD", 3L));
        sparse.put("2026-06", Map.of("PAYMENT", 4L));
        when(overviewQueries.monthlyCounts(ID)).thenReturn(sparse);
        when(overviewQueries.triggeredRules(ID)).thenReturn(List.of());

        ActivityOverviewDto overview =
            new CustomerService(customers, stats, overviewQueries).overview(ID);

        assertThat(overview.monthlyCounts()).extracting(CustomerDtos.MonthlyCountDto::month)
            .containsExactly("2026-03", "2026-04", "2026-05", "2026-06");
        assertThat(overview.monthlyCounts().get(0).card()).isEqualTo(3);
        assertThat(overview.monthlyCounts().get(1).card()).isZero();
        assertThat(overview.monthlyCounts().get(3).payment()).isEqualTo(4);
    }

    @Test
    void likeWildcardsInSearchInputAreEscaped() {
        assertThat(CustomerService.escapeLike("100%_a!b")).isEqualTo("100!%!_a!!b");
        assertThat(CustomerService.escapeLike("weber")).isEqualTo("weber");
    }

    @Test
    void unknownCustomerYields404() {
        when(customers.findById(ID)).thenReturn(Optional.empty());
        CustomerService service = new CustomerService(customers, stats, overviewQueries);
        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(NotFoundException.class);
    }

    private static Customer customer() {
        return new Customer(ID, "CUST-10001", "Anna Keller", "anna@example.ch", "CH",
            null, "VERIFIED", Instant.parse("2023-06-01T08:00:00Z"));
    }
}

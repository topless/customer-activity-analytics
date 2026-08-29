package com.swissquote.caa.customer;

import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.swissquote.caa.common.PageDto;
import com.swissquote.caa.customer.CustomerDtos.ActivityOverviewDto;
import com.swissquote.caa.customer.CustomerDtos.CustomerDetailDto;
import com.swissquote.caa.customer.CustomerDtos.CustomerSummaryDto;

@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public PageDto<CustomerSummaryDto> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return customerService.search(query, page, size);
    }

    @GetMapping("/{id}")
    public CustomerDetailDto get(@PathVariable UUID id) {
        return customerService.get(id);
    }

    @GetMapping("/{id}/overview")
    public ActivityOverviewDto overview(@PathVariable UUID id) {
        return customerService.overview(id);
    }
}

package com.swissquote.caa.transaction;

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
import com.swissquote.caa.customer.CustomerService;
import com.swissquote.caa.transaction.TransactionDtos.TransactionDto;

@RestController
@RequestMapping("/api/customers/{customerId}/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final CustomerService customerService;

    public TransactionController(TransactionService transactionService,
                                 CustomerService customerService) {
        this.transactionService = transactionService;
        this.customerService = customerService;
    }

    @GetMapping
    public PageDto<TransactionDto> list(
            @PathVariable UUID customerId,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int size) {
        customerService.require(customerId);
        return transactionService.page(customerId, type, status, page, size);
    }
}

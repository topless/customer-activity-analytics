package com.swissquote.caa.transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swissquote.caa.common.PageDto;
import com.swissquote.caa.risk.RiskAssessmentRepository;
import com.swissquote.caa.transaction.TransactionDtos.CardDto;
import com.swissquote.caa.transaction.TransactionDtos.CryptoDto;
import com.swissquote.caa.transaction.TransactionDtos.PaymentDto;
import com.swissquote.caa.transaction.TransactionDtos.TransactionDto;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactions;
    private final RiskAssessmentRepository riskAssessments;

    public TransactionService(TransactionRepository transactions,
                              RiskAssessmentRepository riskAssessments) {
        this.transactions = transactions;
        this.riskAssessments = riskAssessments;
    }

    public PageDto<TransactionDto> page(UUID customerId, ActivityType type,
                                        TransactionStatus status, int page, int size) {
        Page<Transaction> result = transactions.findPage(customerId, type, status,
            PageRequest.of(page, size));

        List<UUID> txIds = result.getContent().stream().map(Transaction::getId).toList();
        Map<UUID, RiskInfo> riskByTx = riskInfo(txIds);

        List<TransactionDto> content = result.getContent().stream()
            .map(t -> {
                RiskInfo risk = riskByTx.getOrDefault(t.getId(), RiskInfo.NONE);
                return new TransactionDto(t.getId(), t.getActivityType(), t.getAmount(),
                    t.getCurrency(), t.getStatus(), t.getCreatedAt(),
                    risk.score(), risk.ruleNames(),
                    CardDto.of(t.getCard()), PaymentDto.of(t.getPayment()), CryptoDto.of(t.getCrypto()));
            })
            .toList();
        return PageDto.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private record RiskInfo(BigDecimal score, List<String> ruleNames) {

        static final RiskInfo NONE = new RiskInfo(BigDecimal.ZERO, List.of());
    }

    private Map<UUID, RiskInfo> riskInfo(List<UUID> txIds) {
        Map<UUID, RiskInfo> result = new HashMap<>();
        if (txIds.isEmpty()) {
            return result;
        }
        for (var row : riskAssessments.findByTransactionIds(txIds)) {
            result.merge(row.getTransactionId(),
                new RiskInfo(row.getScoreContribution(), List.of(row.getRuleName())),
                (a, b) -> {
                    List<String> names = new java.util.ArrayList<>(a.ruleNames());
                    names.addAll(b.ruleNames());
                    return new RiskInfo(a.score().add(b.score()), List.copyOf(names));
                });
        }
        return result;
    }
}

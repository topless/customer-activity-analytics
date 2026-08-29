package com.swissquote.caa.analysis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swissquote.caa.analysis.ActivityDigest.CustomerFacts;
import com.swissquote.caa.analysis.ActivityDigest.NotableTransaction;
import com.swissquote.caa.config.AppProperties;
import com.swissquote.caa.customer.Customer;
import com.swissquote.caa.customer.OverviewQueryRepository;
import com.swissquote.caa.risk.RiskAssessmentRepository;
import com.swissquote.caa.transaction.Transaction;
import com.swissquote.caa.transaction.TransactionRepository;

/** Assembles the {@link ActivityDigest} that feeds prompt building and the stub analyst. */
@Service
public class DigestBuilder {

    private final TransactionRepository transactions;
    private final RiskAssessmentRepository riskAssessments;
    private final OverviewQueryRepository overviewQueries;
    private final AppProperties properties;

    public DigestBuilder(TransactionRepository transactions,
                         RiskAssessmentRepository riskAssessments,
                         OverviewQueryRepository overviewQueries,
                         AppProperties properties) {
        this.transactions = transactions;
        this.riskAssessments = riskAssessments;
        this.overviewQueries = overviewQueries;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public ActivityDigest build(Customer customer) {
        UUID customerId = customer.getId();
        List<Transaction> all = transactions.findAllForAnalysis(customerId);
        OverviewQueryRepository.Window window = overviewQueries.window(customerId);

        Map<UUID, BigDecimal> scoreByTx = new HashMap<>();
        Map<UUID, List<String>> rulesByTx = new HashMap<>();
        if (!all.isEmpty()) {
            for (var row : riskAssessments.findByTransactionIds(all.stream().map(Transaction::getId).toList())) {
                scoreByTx.merge(row.getTransactionId(), row.getScoreContribution(), BigDecimal::add);
                rulesByTx.computeIfAbsent(row.getTransactionId(), k -> new ArrayList<>())
                    .add(row.getRuleName());
            }
        }

        List<NotableTransaction> notable = all.stream()
            .sorted(Comparator
                .comparing((Transaction t) -> scoreByTx.getOrDefault(t.getId(), BigDecimal.ZERO))
                .thenComparing(Transaction::getAmount)
                .reversed())
            .limit(properties.analysis().maxPromptTransactions())
            .sorted(Comparator.comparing(Transaction::getCreatedAt))
            .map(t -> new NotableTransaction(t.getId(), t.getCreatedAt(), t.getActivityType(),
                t.getAmount(), t.getCurrency(), t.getStatus(), descriptor(t),
                scoreByTx.getOrDefault(t.getId(), BigDecimal.ZERO),
                List.copyOf(rulesByTx.getOrDefault(t.getId(), List.of()))))
            .toList();

        return new ActivityDigest(
            new CustomerFacts(customer.getCustomerNumber(), customer.getCountry(),
                customer.getKycLevel(), customer.getOnboardedAt()),
            window.from(), window.to(), window.transactionCount(),
            overviewQueries.riskScore(customerId),
            overviewQueries.byType(customerId),
            overviewQueries.byStatus(customerId),
            overviewQueries.triggeredRules(customerId),
            notable);
    }

    private static String descriptor(Transaction t) {
        return switch (t.getActivityType()) {
            case CARD -> {
                var c = t.getCard();
                StringBuilder sb = new StringBuilder();
                sb.append(c.getCardType()).append(" card at '").append(c.getMerchantName())
                    .append("' (MCC ").append(c.getMccCode()).append("), ")
                    .append(c.isCardPresent() ? "card-present" : "card-not-present");
                if (c.getDeclineReason() != null) {
                    sb.append(", declined: ").append(c.getDeclineReason());
                }
                yield sb.toString();
            }
            case PAYMENT -> {
                var p = t.getPayment();
                yield p.getPaymentMethod() + " payment, receiver bank country "
                    + p.getReceiverBankCountry()
                    + " (accounts " + mask(p.getSenderAccount()) + " -> " + mask(p.getReceiverAccount()) + ")";
            }
            case CRYPTO -> {
                var c = t.getCrypto();
                yield c.getBlockchain() + " on-chain transfer "
                    + mask(c.getWalletAddressFrom()) + " -> " + mask(c.getWalletAddressTo())
                    + (c.getExchangeName() == null
                        ? ", no attributed exchange (unhosted counterparty)"
                        : ", via exchange " + c.getExchangeName());
            }
        };
    }

    private static String mask(String account) {
        if (account == null || account.length() <= 8) {
            return account;
        }
        return account.substring(0, 4) + "…" + account.substring(account.length() - 4);
    }
}

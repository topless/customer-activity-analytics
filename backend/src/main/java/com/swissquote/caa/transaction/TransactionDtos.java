package com.swissquote.caa.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionDto(UUID id, ActivityType activityType, BigDecimal amount,
                                 String currency, TransactionStatus status, Instant createdAt,
                                 BigDecimal riskScore, List<String> triggeredRules,
                                 CardDto card, PaymentDto payment, CryptoDto crypto) {
    }

    public record CardDto(String cardPan, String cardType, String merchantName, String mccCode,
                          boolean cardPresent, String authorizationCode, String declineReason) {

        public static CardDto of(CardActivity c) {
            return c == null ? null : new CardDto(c.getCardPan(), c.getCardType(),
                c.getMerchantName(), c.getMccCode(), c.isCardPresent(),
                c.getAuthorizationCode(), c.getDeclineReason());
        }
    }

    public record PaymentDto(String paymentMethod, String senderAccount, String receiverAccount,
                             String receiverBankCountry) {

        public static PaymentDto of(PaymentActivity p) {
            return p == null ? null : new PaymentDto(p.getPaymentMethod(), p.getSenderAccount(),
                p.getReceiverAccount(), p.getReceiverBankCountry());
        }
    }

    public record CryptoDto(String blockchain, String walletAddressFrom, String walletAddressTo,
                            String txHash, String exchangeName) {

        public static CryptoDto of(CryptoActivity c) {
            return c == null ? null : new CryptoDto(c.getBlockchain(), c.getWalletAddressFrom(),
                c.getWalletAddressTo(), c.getTxHash(), c.getExchangeName());
        }
    }
}

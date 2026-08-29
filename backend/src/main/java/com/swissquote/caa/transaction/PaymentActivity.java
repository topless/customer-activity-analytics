package com.swissquote.caa.transaction;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_activity")
public class PaymentActivity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "sender_account", nullable = false)
    private String senderAccount;

    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;

    @Column(name = "receiver_bank_country", nullable = false, columnDefinition = "bpchar")
    private String receiverBankCountry;

    protected PaymentActivity() {
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public String getReceiverBankCountry() {
        return receiverBankCountry == null ? null : receiverBankCountry.trim();
    }
}

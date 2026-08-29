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
@Table(name = "card_activity")
public class CardActivity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "card_pan", nullable = false)
    private String cardPan;

    @Column(name = "card_type", nullable = false)
    private String cardType;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "mcc_code", nullable = false)
    private String mccCode;

    @Column(name = "card_present", nullable = false)
    private boolean cardPresent;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "decline_reason")
    private String declineReason;

    protected CardActivity() {
    }

    public String getCardPan() {
        return cardPan;
    }

    public String getCardType() {
        return cardType;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMccCode() {
        return mccCode;
    }

    public boolean isCardPresent() {
        return cardPresent;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getDeclineReason() {
        return declineReason;
    }
}

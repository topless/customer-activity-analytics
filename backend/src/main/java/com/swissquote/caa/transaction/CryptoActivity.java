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
@Table(name = "crypto_activity")
public class CryptoActivity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private String blockchain;

    @Column(name = "wallet_address_from", nullable = false)
    private String walletAddressFrom;

    @Column(name = "wallet_address_to", nullable = false)
    private String walletAddressTo;

    @Column(name = "tx_hash", nullable = false)
    private String txHash;

    @Column(name = "exchange_name")
    private String exchangeName;

    protected CryptoActivity() {
    }

    public String getBlockchain() {
        return blockchain;
    }

    public String getWalletAddressFrom() {
        return walletAddressFrom;
    }

    public String getWalletAddressTo() {
        return walletAddressTo;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getExchangeName() {
        return exchangeName;
    }
}

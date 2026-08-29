package com.swissquote.caa.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private UUID id;

    @Column(name = "customer_number", nullable = false, unique = true)
    private String customerNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Column(nullable = false, columnDefinition = "bpchar")
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "kyc_level", nullable = false)
    private String kycLevel;

    @Column(name = "onboarded_at", nullable = false)
    private Instant onboardedAt;

    protected Customer() {
    }

    public Customer(UUID id, String customerNumber, String fullName, String email, String country,
                    LocalDate dateOfBirth, String kycLevel, Instant onboardedAt) {
        this.id = id;
        this.customerNumber = customerNumber;
        this.fullName = fullName;
        this.email = email;
        this.country = country;
        this.dateOfBirth = dateOfBirth;
        this.kycLevel = kycLevel;
        this.onboardedAt = onboardedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country == null ? null : country.trim();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getKycLevel() {
        return kycLevel;
    }

    public Instant getOnboardedAt() {
        return onboardedAt;
    }
}

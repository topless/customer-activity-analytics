# High-Risk Jurisdictions and Sanctions Screening

Internal policy SAN-402, version 4.0. Country-risk classification for payment screening
and the handling of blocked or attempted transfers. (Fictional demo document.)

## Prohibited and call-for-action jurisdictions

Payments to or from beneficiary banks in jurisdictions on the FATF call-for-action list —
Iran (IR), the Democratic People's Republic of Korea (KP) and Myanmar (MM) — are blocked
by the payment engine. An ATTEMPTED transfer to such a jurisdiction, even when blocked or
failed, is itself a significant risk event: it must be recorded, scored, and escalated to
the sanctions desk. Two or more attempts, or a single attempt above CHF 10,000, require
immediate escalation and a temporary review hold on outgoing payments.

## Enhanced-monitoring jurisdictions

Payments involving jurisdictions under increased monitoring or with elevated typology
exposure for the platform's customer base (including, for this policy's purposes, the
United Arab Emirates (AE) and Türkiye (TR) corridors for high-value personal transfers)
are permitted but attract enhanced scrutiny: purpose-of-payment information should be
collected for transfers at or above CHF 10,000 equivalent, and repeated sub-threshold
transfers to these corridors must be reviewed against the structuring guidance in the AML
Transaction Monitoring Policy.

## Screening and operator duties

Sanctions screening runs automatically on every payment. Operators must never advise a
customer on how to re-route a blocked payment, split amounts, or use an alternative
corridor — doing so may constitute facilitation. When a customer asks about a blocked
transfer, the approved wording is that the payment "could not be processed for regulatory
reasons" and that the compliance team will contact them if information is required.

## Record keeping

All sanction-related events — blocks, near-matches, manual releases — are retained for ten
years and must be linked to the customer case file. AI-generated analyses that reference a
sanctions signal must be preserved with the underlying prompt and model output for audit.

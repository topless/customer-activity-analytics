# Card Fraud and Merchant Risk Guidance

Internal guidance CARD-114, version 2.1. Operational guidance for reviewing card activity
alerts: declines, card-not-present spending and high-risk merchant categories.
(Fictional demo document.)

## Card-not-present transactions

Card-not-present (CNP) transactions carry materially higher fraud risk than card-present
ones because neither chip nor PIN is verified. A single high-value CNP purchase (at or
above 2,000 in account currency) is a weak signal on its own; it becomes significant when
combined with a new merchant, an unusual merchant category, night-time timestamps, or a
recent series of declines. For flagged CNP activity the operator should confirm the
purchase with the customer through a verified channel before considering a card block.

## Repeated declines

Three or more failed card authorisations within 24 hours indicate either card testing by a
fraudster (typically small amounts at online merchants), a compromised card being used
beyond its limits, or a customer in financial difficulty repeatedly attempting a purchase.
Decline reasons matter: SUSPECTED_FRAUD declines should lead to an immediate outbound
contact and possible card replacement; INSUFFICIENT_FUNDS and CARD_LIMIT_EXCEEDED patterns
at gambling merchants should be reviewed under the responsible-gaming guidance below.

## High-risk merchant categories

Merchant category codes treated as high-risk: 7995 (betting and casino gambling), 6051
(quasi-cash: crypto vouchers, money orders, prepaid top-ups) and 4829 (money transfer
services). Sustained spending in these categories changes the customer's risk profile and
can indicate gambling harm, use of cards to acquire crypto outside the platform, or
third-party remittance activity. Quasi-cash and money-transfer spending that closely
follows incoming payments is a layering indicator and should be cross-checked against the
customer's payment activity.

## Responsible gaming considerations

Where gambling-category spending exceeds roughly a quarter of observed monthly card volume,
or where gambling transactions cluster late at night with escalating amounts and declines,
the operator should flag the account for the responsible-gaming review process. This is a
customer-protection measure, not an accusation: the recommended first step is a
documented affordability review, not a restriction.

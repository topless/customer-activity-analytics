# AML Transaction Monitoring Policy

Internal policy AML-201, version 3.2. Applies to all retail customer accounts. This policy
defines how transaction monitoring alerts are generated, triaged and escalated, and what
customer-care operators must do when reviewing flagged activity. (Fictional demo document.)

## Purpose and scope

Transaction monitoring exists to detect money laundering, terrorist financing and fraud
across all activity types: card transactions, account-to-account payments (ACH, WIRE,
SWIFT, P2P) and cryptocurrency transfers. Every automated rule signal contributes a
weighted score to the customer's cumulative risk score. Operators reviewing a customer
must consider the combination of signals, not each alert in isolation: several
medium-weight signals in a short window are frequently more significant than a single
high-weight signal.

## High-value and cross-border payments

Outgoing payments at or above CHF/EUR/USD 10,000 to a beneficiary bank in a different
country than the customer's country of residence require a documented plausibility check.
The operator should verify whether the amount and destination match the customer's known
profile (salary, declared wealth, stated account purpose). Repeated high-value cross-border
payments to the same beneficiary within 30 days must be treated as a single pattern and
escalated together. Corporate-style inbound wires to a personal account (large, round
amounts from company accounts abroad) followed by rapid outflows are a classic layering
indicator.

## Structuring and smurfing

Structuring means splitting a transfer into several smaller payments to stay below a
reporting or monitoring threshold. Indicative pattern: three or more outgoing payments
between 9,000 and 9,999 (in CHF, EUR or USD) within any rolling 7-day window, especially
to beneficiaries in the same country or to the same beneficiary bank. When a structuring
pattern is detected, the operator must NOT contact the customer about the specific alerts
(tipping-off risk) and must escalate directly to the AML compliance team.

## Velocity and dormancy changes

A sudden spike in transaction count — more than five times the customer's trailing 90-day
daily average — or a dormant account (no activity for 60+ days) that abruptly resumes with
high-frequency activity warrants a profile review. Common benign explanations include
travel, a move, or seasonal spending; these should be recorded in the case notes if
confirmed. Where no plausible explanation exists, treat the change as a potential account
takeover or mule-account indicator.

## Escalation thresholds

Cumulative rule-based risk scores map to review urgency as follows: below 50 — routine, no
action required; 50 to 149 — heightened monitoring, review at next periodic check; 150 to
399 — investigation required, escalate to compliance within 5 business days; 400 and above
— immediate escalation to the AML compliance team within 24 hours, and consider whether a
report to MROS (Money Laundering Reporting Office Switzerland) is warranted. Any single
confirmed sanctions-related signal escalates immediately regardless of score.

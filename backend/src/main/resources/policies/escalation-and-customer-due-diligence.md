# Escalation Procedures and Customer Due Diligence

Internal procedure OPS-118, version 2.6. What customer-care operators do after a risk
review: escalation paths, customer contact rules and KYC refresh triggers.
(Fictional demo document.)

## Escalation paths

Level 1 (operator): document the review outcome in the case file; no external action.
Level 2 (compliance referral): create a compliance case with the flagged transactions,
the rule signals and the AI analysis attached; SLA 5 business days. Level 3 (urgent):
phone the AML duty officer, then file the case; SLA 24 hours. Use Level 3 for any
sanctions-related signal, cumulative risk score of 400 or more, suspected account
takeover, or a customer-initiated request that would move more than CHF 100,000 while a
review is open.

## Customer contact rules

Operators may contact customers to verify card transactions, confirm travel, or clarify
account usage. Operators must NOT reveal that a payment triggered an AML rule, that a
compliance case exists, or that a suspicious-activity report is being considered —
tipping off is a criminal offence. If activity must be discussed, use neutral wording
("routine account review"). All customer contact about flagged activity is logged with
timestamp and operator identity.

## KYC refresh and enhanced due diligence

An enhanced due diligence (EDD) refresh is triggered by: a HIGH or CRITICAL AI analysis
outcome confirmed by an operator; a change in transaction behaviour inconsistent with the
recorded profile (new corridors, new activity types, order-of-magnitude volume changes);
or a BASIC-KYC customer breaching CHF 25,000 in monthly volume. EDD collects
source-of-funds evidence, occupation and expected activity, and upgrades the customer's
KYC level. A customer's declared profile must make their observed activity plausible;
"the customer has always done this" is not a substitute for documentation.

## Use of AI analyses

AI-generated analyses are decision support, never decisions. The operator remains
responsible for the outcome and must review the cited transactions before escalating.
Every analysis run is persisted with its risk level, findings, recommendations, prompt and
raw model output; supervisors sample completed analyses monthly for quality assurance.
An analysis that fails to complete must not silently disappear — failed runs are recorded
and visible in the customer's analysis history.

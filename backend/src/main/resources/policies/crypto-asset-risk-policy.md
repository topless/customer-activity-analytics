# Crypto-Asset Transfer Risk Policy

Internal policy CRY-330, version 1.4. Rules for assessing cryptocurrency deposits and
withdrawals, counterparty wallets and exchange attribution. (Fictional demo document.)

## Exchange-attributed counterparties

Transfers where the counterparty wallet is attributed to a registered Virtual Asset
Service Provider (VASP) — e.g. an account at a known exchange such as Kraken, Coinbase or
Binance — are standard-risk. The travel rule applies: for transfers at or above CHF 1,000
equivalent, originator and beneficiary information must accompany the transfer between
VASPs. Frequent trading between the customer's own attributed accounts is a normal
pattern for an active trader profile.

## Unhosted and unattributed wallets

An outgoing transfer to a wallet with no VASP attribution ("unhosted" or self-hosted
wallet) is not prohibited, but it removes downstream visibility and therefore requires
proportionate diligence. For transfers at or above CHF 10,000 equivalent to an unhosted
wallet, the customer should be asked to demonstrate ownership of the destination wallet
(e.g. a signed message or a micro-deposit test). Repeated transfers to the same
unattributed wallet, particularly shortly after fiat inflows, are a strong layering
indicator and must be escalated. Any wallet matching a known mixer or tumbler cluster is
treated as a sanctions-equivalent signal: freeze further crypto withdrawals pending
compliance review.

## Fiat-to-crypto pass-through

A pass-through pattern is: substantial fiat inflow (wire or salary-atypical deposit),
conversion to crypto within 72 hours, followed by an on-chain withdrawal — leaving little
or no balance. This defeats the purpose of a custodial account and is a primary
money-laundering typology. Where cumulative pass-through volume exceeds CHF 20,000
equivalent, escalate to compliance; below that, record the pattern and monitor. The
plausibility of a trading motive should be assessed against the customer's history: an
established trader moving funds to a personal cold wallet after profit-taking is a common
benign explanation, and proof of wallet ownership resolves it.

## Valuation and thresholds

All crypto thresholds in this policy are evaluated at the fiat equivalent at transaction
time using the platform reference rate. Operators should not re-derive valuations
manually; the monitoring system's recorded fiat value is authoritative for threshold
comparisons.

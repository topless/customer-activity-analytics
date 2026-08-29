#!/usr/bin/env python3
"""Deterministic demo-data generator for Customer Activity Analytics.

Generates backend/src/main/resources/db/migration/V3__seed_demo_data.sql:
8 fictional customers with distinct behaviour archetypes, ~500 transactions
(card / payment / crypto) over the 6 months up to 2026-08-28, and
risk_assessments produced by re-implementing the risk_rules catalogue
(V2__reference_data.sql) as a small rule engine over the generated timeline —
so the seeded risk signals are consistent with the rules they reference.

All names, accounts, IBANs, wallets and hashes are fictional. The RNG is
seeded, so the output is fully reproducible.

Usage: python3 scripts/generate_seed_data.py
"""

from __future__ import annotations

import random
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

rng = random.Random(42)

NOW = datetime(2026, 8, 28, 12, 0, 0, tzinfo=timezone.utc)
WINDOW_DAYS = 180

# Fixed rate table used only to reason about fiat-equivalents of crypto amounts.
FIAT_RATE = {"BTC": 60_000.0, "ETH": 2_500.0}

HIGH_RISK_COUNTRIES = {"IR", "KP", "MM"}
HIGH_RISK_MCCS = {"7995", "6051", "4829"}

RULES = {
    "R1": ("0b000000-0000-0000-0000-000000000001", 25.00),  # high-value cross-border payment
    "R2": ("0b000000-0000-0000-0000-000000000002", 40.00),  # payment to high-risk jurisdiction
    "R3": ("0b000000-0000-0000-0000-000000000003", 30.00),  # structuring
    "R4": ("0b000000-0000-0000-0000-000000000004", 15.00),  # high-value card-not-present
    "R5": ("0b000000-0000-0000-0000-000000000005", 20.00),  # repeated card declines
    "R6": ("0b000000-0000-0000-0000-000000000006", 15.00),  # high-risk MCC
    "R7": ("0b000000-0000-0000-0000-000000000007", 45.00),  # unhosted/mixer wallet
    "R8": ("0b000000-0000-0000-0000-000000000008", 30.00),  # rapid fiat-to-crypto pass-through
    "R9": ("0b000000-0000-0000-0000-000000000009", 20.00),  # velocity spike
    "R10": ("0b000000-0000-0000-0000-000000000010", 25.00),  # high-value crypto withdrawal
}


def new_uuid() -> str:
    return str(uuid.UUID(int=rng.getrandbits(128), version=4))


def ts(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S+00")


def day(offset_days: float, hour: int | None = None, minute: int | None = None) -> datetime:
    """A timestamp `offset_days` before NOW, at a plausible daytime hour."""
    base = NOW - timedelta(days=offset_days)
    h = hour if hour is not None else rng.randint(8, 21)
    m = minute if minute is not None else rng.randint(0, 59)
    return base.replace(hour=h, minute=m, second=rng.randint(0, 59))


def q(s: str | None) -> str:
    if s is None:
        return "NULL"
    return "'" + s.replace("'", "''") + "'"


def iban(country: str) -> str:
    return f"{country}{rng.randint(10, 98)} {rng.randint(1000, 9999)} {rng.randint(1000, 9999)} {rng.randint(1000, 9999)} {rng.randint(10, 99)}"


def btc_addr() -> str:
    return "bc1q" + "".join(rng.choices("023456789acdefghjklmnpqrstuvwxyz", k=32))


def eth_addr() -> str:
    return "0x" + "".join(rng.choices("0123456789abcdef", k=40))


def tx_hash(coin: str) -> str:
    """BTC txids are bare hex; EVM chains prefix 0x."""
    digest = "".join(rng.choices("0123456789abcdef", k=64))
    return digest if coin == "BTC" else "0x" + digest


MERCHANTS = [
    ("AlpMart Zurich", "5411", 8, 220),          # groceries
    ("Cafe Central", "5812", 5, 80),             # restaurants
    ("SwissRail Tickets", "4111", 3, 150),       # transport
    ("Pharma Plus", "5912", 10, 90),             # pharmacy
    ("TechnoWorld Electronics", "5732", 40, 900),
    ("Modehaus Bergmann", "5651", 30, 350),      # clothing
    ("Bella Italia Ristorante", "5812", 25, 140),
    ("CityParking AG", "7523", 2, 40),
]
GAMBLING_MERCHANT = ("Golden Palace Casino Online", "7995")
QUASI_CASH_MERCHANT = ("CryptoDirect Voucher Shop", "6051")
MONEY_TRANSFER_MERCHANT = ("QuickCash Transfer Services", "4829")
CNP_MERCHANTS = [
    ("AirTravel Booking", "4511", 400, 3800),
    ("LuxWatch Boutique Online", "5944", 2000, 6500),
    ("TechnoWorld Online Store", "5732", 800, 3200),
    ("Grand Hotel Reservations", "7011", 500, 2400),
]

DECLINE_REASONS = ["INSUFFICIENT_FUNDS", "SUSPECTED_FRAUD", "CARD_LIMIT_EXCEEDED", "INCORRECT_CVV"]


class Customer:
    def __init__(self, n, number, name, email, country, dob, kyc, onboarded):
        self.id = f"c0000000-0000-0000-0000-00000000000{n}"
        self.number = number
        self.name = name
        self.email = email
        self.country = country
        self.dob = dob
        self.kyc = kyc
        self.onboarded = onboarded
        self.own_iban = iban(country)
        self.own_btc = btc_addr()
        self.own_eth = eth_addr()
        self.txs: list[dict] = []


CUSTOMERS = [
    Customer(1, "CUST-10001", "Anna Keller", "anna.keller@example.ch", "CH", "1988-04-12", "VERIFIED", "2023-06-01"),
    Customer(2, "CUST-10002", "Marco Rossi", "marco.rossi@example.it", "IT", "1979-11-03", "VERIFIED", "2022-02-14"),
    Customer(3, "CUST-10003", "Sophie Laurent", "sophie.laurent@example.fr", "FR", "1992-07-25", "VERIFIED", "2024-01-20"),
    Customer(4, "CUST-10004", "Lukas Weber", "lukas.weber@example.ch", "CH", "1983-01-30", "ENHANCED", "2021-09-08"),
    Customer(5, "CUST-10005", "Elena Papadopoulou", "elena.papadopoulou@example.gr", "GR", "1995-03-17", "BASIC", "2024-11-02"),
    Customer(6, "CUST-10006", "James Miller", "james.miller@example.co.uk", "GB", "1975-08-09", "VERIFIED", "2020-05-30"),
    Customer(7, "CUST-10007", "Yuki Tanaka", "yuki.tanaka@example.jp", "JP", "1990-12-01", "VERIFIED", "2023-03-11"),
    Customer(8, "CUST-10008", "Carlos Mendes", "carlos.mendes@example.pt", "PT", "1986-06-22", "BASIC", "2023-10-05"),
]


def add_card(c: Customer, when: datetime, amount: float, merchant: str, mcc: str,
             card_present: bool, status: str = "COMPLETED", decline_reason: str | None = None,
             card_type: str = "DEBIT") -> dict:
    t = {
        "id": new_uuid(), "customer": c, "type": "CARD", "amount": round(amount, 2),
        "currency": {"CH": "CHF", "GB": "GBP", "JP": "CHF"}.get(c.country, "EUR"),
        "status": status, "at": when, "fiat": round(amount, 2),
        "detail": {
            "card_pan": "****" + str(rng.randint(1000, 9999)),
            "card_type": card_type,
            "merchant_name": merchant,
            "mcc_code": mcc,
            "card_present": card_present,
            "authorization_code": None if status == "FAILED" else "".join(rng.choices("ABCDEF0123456789", k=6)),
            "decline_reason": decline_reason,
        },
    }
    c.txs.append(t)
    return t


def add_payment(c: Customer, when: datetime, amount: float, currency: str, method: str,
                outgoing: bool, counterparty_iban: str, counterparty_country: str,
                status: str = "COMPLETED") -> dict:
    t = {
        "id": new_uuid(), "customer": c, "type": "PAYMENT", "amount": round(amount, 2),
        "currency": currency, "status": status, "at": when, "fiat": round(amount, 2),
        "outgoing": outgoing,
        "detail": {
            "payment_method": method,
            "sender_account": c.own_iban if outgoing else counterparty_iban,
            "receiver_account": counterparty_iban if outgoing else c.own_iban,
            "receiver_bank_country": counterparty_country if outgoing else c.country,
        },
    }
    c.txs.append(t)
    return t


def add_crypto(c: Customer, when: datetime, coin_amount: float, coin: str, outgoing: bool,
               exchange: str | None, external_unhosted: bool = False,
               counterparty: str | None = None, status: str = "COMPLETED") -> dict:
    own = c.own_btc if coin == "BTC" else c.own_eth
    other = counterparty or (btc_addr() if coin == "BTC" else eth_addr())
    t = {
        "id": new_uuid(), "customer": c, "type": "CRYPTO", "amount": round(coin_amount, 2),
        "currency": coin, "status": status, "at": when,
        "fiat": round(coin_amount * FIAT_RATE[coin], 2),
        "outgoing": outgoing, "exchange": exchange, "external_unhosted": external_unhosted,
        "detail": {
            "blockchain": coin,
            "wallet_address_from": own if outgoing else other,
            "wallet_address_to": other if outgoing else own,
            "tx_hash": tx_hash(coin),
            "exchange_name": exchange,
        },
    }
    c.txs.append(t)
    return t


def everyday_card_spend(c: Customer, count: int, from_day: int = WINDOW_DAYS, to_day: int = 0):
    for _ in range(count):
        merchant, mcc, lo, hi = MERCHANTS[rng.randrange(len(MERCHANTS))]
        status = "COMPLETED" if rng.random() > 0.04 else "REVERSED"
        add_card(c, day(rng.uniform(to_day, from_day)), rng.uniform(lo, hi), merchant, mcc,
                 card_present=rng.random() > 0.25, status=status,
                 card_type=rng.choice(["DEBIT", "DEBIT", "CREDIT"]))


def monthly_salary(c: Customer, amount: float, currency: str, months: int = 6):
    for m in range(months):
        add_payment(c, day(WINDOW_DAYS - 5 - m * 30, hour=6), amount * rng.uniform(0.98, 1.02),
                    currency, "WIRE", outgoing=False,
                    counterparty_iban=iban("CH" if c.country == "CH" else "DE"),
                    counterparty_country="CH" if c.country == "CH" else "DE")


# ---------------------------------------------------------------- archetypes

def build_anna(c: Customer):
    """Regular retail customer: groceries, restaurants, salary, rent. Low risk."""
    everyday_card_spend(c, 85)
    monthly_salary(c, 8_500, "CHF")
    for m in range(6):  # rent, domestic
        add_payment(c, day(WINDOW_DAYS - 8 - m * 30, hour=7), 2_200, "CHF", "ACH",
                    outgoing=True, counterparty_iban=iban("CH"), counterparty_country="CH")
    # one larger online purchase -> single CNP signal
    add_card(c, day(40), 2_150, "TechnoWorld Online Store", "5732", card_present=False, card_type="CREDIT")


def build_marco(c: Customer):
    """Retail plus small recurring exchange-based crypto buys. Low risk."""
    everyday_card_spend(c, 55)
    monthly_salary(c, 4_900, "EUR")
    for m in range(6):
        add_crypto(c, day(WINDOW_DAYS - 12 - m * 30), rng.uniform(0.05, 0.15), "ETH",
                   outgoing=False, exchange="Coinbase")
    add_crypto(c, day(70), 0.21, "ETH", outgoing=True, exchange="Coinbase")


def build_sophie(c: Customer):
    """Payments-heavy: salary in, utilities and rent out, one large cross-border transfer."""
    everyday_card_spend(c, 20)
    monthly_salary(c, 5_600, "EUR")
    for m in range(6):
        for amount in (1_450, 180, 95):  # rent, utilities, telecom
            add_payment(c, day(WINDOW_DAYS - 10 - m * 30), amount * rng.uniform(0.95, 1.05),
                        "EUR", "ACH", outgoing=True,
                        counterparty_iban=iban("FR"), counterparty_country="FR")
    # house deposit to the UK: legitimate but crosses the R1 threshold
    add_payment(c, day(55, hour=10), 12_000, "EUR", "SWIFT", outgoing=True,
                counterparty_iban=iban("GB"), counterparty_country="GB")


def build_lukas(c: Customer):
    """High-risk archetype: large inbound wires, structured outbound SWIFT payments to AE/TR,
    an attempted payment to a FATF high-risk jurisdiction, rapid fiat-to-crypto pass-through
    and repeated transfers to one unhosted wallet."""
    everyday_card_spend(c, 25)
    mixer_wallet = btc_addr()  # the same unattributed destination, reused

    # Large inbound wires from foreign corporate accounts
    inbound_days = [150, 110, 68, 26]
    for d in inbound_days:
        add_payment(c, day(d, hour=7), rng.uniform(24_000, 42_000), "CHF", "WIRE",
                    outgoing=False, counterparty_iban=iban("LU"), counterparty_country="LU")

    # Structuring: clusters of just-under-10k SWIFT payments within a few days
    for cluster_start in (95, 60, 21):
        for i in range(rng.randint(3, 4)):
            add_payment(c, day(cluster_start - i * 1.5, hour=9 + i), rng.uniform(9_100, 9_850),
                        "CHF", "SWIFT", outgoing=True,
                        counterparty_iban=iban("AE" if rng.random() < 0.6 else "TR"),
                        counterparty_country="AE" if rng.random() < 0.6 else "TR")

    # Two overt high-value cross-border wires
    add_payment(c, day(44, hour=11), 18_500, "CHF", "SWIFT", outgoing=True,
                counterparty_iban=iban("AE"), counterparty_country="AE")
    add_payment(c, day(17, hour=15), 27_300, "CHF", "SWIFT", outgoing=True,
                counterparty_iban=iban("AE"), counterparty_country="AE")

    # Attempted payment to a FATF high-risk jurisdiction, blocked
    add_payment(c, day(33, hour=14), 25_000, "CHF", "SWIFT", outgoing=True,
                counterparty_iban="MM61 2910 0034 8812 7754 20", counterparty_country="MM",
                status="FAILED")

    # Fiat-to-crypto pass-through: BTC bought on Kraken within 72h of the big inbound wires,
    # then moved on to the same unattributed wallet — outflow stays inside R8's 72h window
    # relative to the wire (wire at day(68)/day(26), buy ~51h later, outflow ~59h later)
    for d in (66.2, 24.8):
        add_crypto(c, day(d, hour=10), rng.uniform(0.38, 0.55), "BTC", outgoing=False, exchange="Kraken")
        add_crypto(c, day(d - 0.5, hour=18), rng.uniform(0.36, 0.5), "BTC", outgoing=True,
                   exchange=None, external_unhosted=True, counterparty=mixer_wallet)
    add_crypto(c, day(9, hour=22), 0.42, "BTC", outgoing=True, exchange=None,
               external_unhosted=True, counterparty=mixer_wallet)

    # High-value card-not-present spending
    for d in (52, 12):
        m, mcc, lo, hi = CNP_MERCHANTS[1]
        add_card(c, day(d), rng.uniform(2_800, 4_500), m, mcc, card_present=False, card_type="CREDIT")


def build_elena(c: Customer):
    """Gambling-heavy card use with a burst of declines. Medium risk."""
    everyday_card_spend(c, 45)
    monthly_salary(c, 2_400, "EUR")
    for _ in range(8):
        add_card(c, day(rng.uniform(2, 120), hour=rng.randint(20, 23)),
                 rng.uniform(50, 900), *GAMBLING_MERCHANT, card_present=False)
    for _ in range(2):
        add_card(c, day(rng.uniform(10, 100)), rng.uniform(150, 400), *QUASI_CASH_MERCHANT,
                 card_present=False)
    # one evening: four declined attempts in a row at the casino
    for i in range(4):
        add_card(c, day(14, hour=21, minute=3 + i * 11), 500, *GAMBLING_MERCHANT,
                 card_present=False, status="FAILED",
                 decline_reason="INSUFFICIENT_FUNDS" if i < 3 else "CARD_LIMIT_EXCEEDED")


def build_james(c: Customer):
    """Established customer with occasional cross-border payments. Low-medium risk."""
    everyday_card_spend(c, 40)
    monthly_salary(c, 7_200, "GBP")
    for d in (130, 85, 42):
        add_payment(c, day(d), rng.uniform(3_000, 8_000), "GBP", "SWIFT", outgoing=True,
                    counterparty_iban=iban("CH"), counterparty_country="CH")
    add_payment(c, day(29, hour=9), 14_000, "GBP", "SWIFT", outgoing=True,
                counterparty_iban=iban("US"), counterparty_country="US")
    add_card(c, day(75), 2_450, *CNP_MERCHANTS[0][:2], card_present=False, card_type="CREDIT")


def build_yuki(c: Customer):
    """Active exchange-based crypto trader. Medium risk from one large cold-wallet withdrawal."""
    everyday_card_spend(c, 15)
    for _ in range(60):
        coin = rng.choice(["BTC", "ETH", "ETH"])
        amt = rng.uniform(0.03, 0.2) if coin == "BTC" else rng.uniform(0.3, 3.0)
        add_crypto(c, day(rng.uniform(1, WINDOW_DAYS)), amt, coin,
                   outgoing=rng.random() < 0.45,
                   exchange=rng.choice(["Kraken", "Coinbase", "Binance"]))
    # withdrawal of 0.28 BTC to a personal cold wallet (unattributed -> flags R7 + R10)
    add_crypto(c, day(20, hour=12), 0.28, "BTC", outgoing=True, exchange=None,
               external_unhosted=True)


def build_carlos(c: Customer):
    """Dormant for months, then a sudden burst of activity. Medium risk."""
    for d in (170, 155, 128, 101, 88):
        merchant, mcc, lo, hi = MERCHANTS[rng.randrange(len(MERCHANTS))]
        add_card(c, day(d), rng.uniform(lo, hi), merchant, mcc, card_present=True)
    # burst: 22 transactions within 5 days after ~80 quiet days
    for _ in range(16):
        merchant, mcc, lo, hi = MERCHANTS[rng.randrange(len(MERCHANTS))]
        add_card(c, day(rng.uniform(3, 8)), rng.uniform(lo, hi * 2), merchant, mcc,
                 card_present=rng.random() > 0.5)
    for _ in range(4):
        add_payment(c, day(rng.uniform(3, 8)), rng.uniform(1_200, 4_800), "EUR", "P2P",
                    outgoing=True, counterparty_iban=iban("PT"), counterparty_country="PT")
    add_payment(c, day(4, hour=16), rng.uniform(500, 900), "EUR", "P2P", outgoing=False,
                counterparty_iban=iban("PT"), counterparty_country="PT")


# ---------------------------------------------------------------- rule engine

def evaluate_rules(c: Customer) -> list[dict]:
    """Re-implements the risk_rules catalogue over the generated timeline."""
    assessments = []
    txs = sorted(c.txs, key=lambda t: t["at"])

    def fire(rule_key: str, t: dict):
        rule_id, weight = RULES[rule_key]
        assessments.append({
            "id": new_uuid(), "tx": t["id"], "rule": rule_id,
            "at": t["at"] + timedelta(minutes=rng.randint(1, 9)), "score": weight,
        })

    payments_sub10k: list[datetime] = []
    failed_cards: list[datetime] = []
    inbound_wires: list[tuple[datetime, float]] = []
    velocity_fires = 0

    for i, t in enumerate(txs):
        if t["type"] == "PAYMENT" and t.get("outgoing"):
            country = t["detail"]["receiver_bank_country"]
            if t["fiat"] >= 10_000 and country != c.country:
                fire("R1", t)
            if country in HIGH_RISK_COUNTRIES:
                fire("R2", t)
            if 9_000 <= t["fiat"] < 10_000:
                payments_sub10k.append(t["at"])
                recent = [x for x in payments_sub10k if t["at"] - x <= timedelta(days=7)]
                if len(recent) >= 3:
                    fire("R3", t)
        if t["type"] == "PAYMENT" and not t.get("outgoing") and t["fiat"] >= 20_000:
            inbound_wires.append((t["at"], t["fiat"]))

        if t["type"] == "CARD":
            d = t["detail"]
            if not d["card_present"] and t["fiat"] >= 2_000:
                fire("R4", t)
            if d["mcc_code"] in HIGH_RISK_MCCS:
                fire("R6", t)
            if t["status"] == "FAILED":
                failed_cards.append(t["at"])
                recent = [x for x in failed_cards if t["at"] - x <= timedelta(hours=24)]
                if len(recent) >= 3:
                    fire("R5", t)

        if t["type"] == "CRYPTO" and t.get("outgoing"):
            if t.get("external_unhosted"):
                fire("R7", t)
                if t["fiat"] >= 10_000:
                    fire("R10", t)
            if t["fiat"] >= 20_000 and any(
                    timedelta(0) <= t["at"] - w_at <= timedelta(hours=72) and w_amt >= 20_000
                    for w_at, w_amt in inbound_wires):
                fire("R8", t)

        # velocity (R9, matching its threshold_logic): >= 10 tx in the trailing 7 days AND
        # 7-day count > 5x the expected count from the 90-day daily average, max 3 alerts
        if velocity_fires < 3:
            c7 = sum(1 for x in txs[: i + 1] if t["at"] - x["at"] <= timedelta(days=7))
            c90 = sum(1 for x in txs[: i + 1] if t["at"] - x["at"] <= timedelta(days=90))
            daily_avg = c90 / 90.0
            if c7 >= 10 and c7 > 5 * daily_avg * 7:
                fire("R9", t)
                velocity_fires += 1

    return assessments


# ---------------------------------------------------------------- SQL output

def main():
    builders = [build_anna, build_marco, build_sophie, build_lukas,
                build_elena, build_james, build_yuki, build_carlos]
    for customer, build in zip(CUSTOMERS, builders):
        build(customer)

    out = ["-- Demo dataset. GENERATED by scripts/generate_seed_data.py — do not edit by hand.",
           "-- All persons, accounts, IBANs, wallet addresses and merchants are fictional.", ""]

    out.append("INSERT INTO customers (customer_id, customer_number, full_name, email, country, date_of_birth, kyc_level, onboarded_at) VALUES")
    rows = [f"  ('{c.id}', '{c.number}', {q(c.name)}, {q(c.email)}, '{c.country}', '{c.dob}', '{c.kyc}', '{c.onboarded} 08:00:00+00')"
            for c in CUSTOMERS]
    out.append(",\n".join(rows) + ";\n")

    all_txs, all_assessments = [], []
    for c in CUSTOMERS:
        all_txs.extend(c.txs)
        all_assessments.extend(evaluate_rules(c))
    all_txs.sort(key=lambda t: t["at"])

    for batch_start in range(0, len(all_txs), 50):
        batch = all_txs[batch_start:batch_start + 50]
        out.append("INSERT INTO transactions (transaction_id, customer_id, activity_type, amount, currency, status, created_at) VALUES")
        rows = [f"  ('{t['id']}', '{t['customer'].id}', '{t['type']}', {t['amount']:.2f}, '{t['currency']}', '{t['status']}', '{ts(t['at'])}')"
                for t in batch]
        out.append(",\n".join(rows) + ";\n")

    cards = [t for t in all_txs if t["type"] == "CARD"]
    out.append("INSERT INTO card_activity (transaction_id, card_pan, card_type, merchant_name, mcc_code, card_present, authorization_code, decline_reason) VALUES")
    rows = [f"  ('{t['id']}', '{d['card_pan']}', '{d['card_type']}', {q(d['merchant_name'])}, '{d['mcc_code']}', {str(d['card_present']).lower()}, {q(d['authorization_code'])}, {q(d['decline_reason'])})"
            for t in cards for d in [t["detail"]]]
    out.append(",\n".join(rows) + ";\n")

    payments = [t for t in all_txs if t["type"] == "PAYMENT"]
    out.append("INSERT INTO payment_activity (transaction_id, payment_method, sender_account, receiver_account, receiver_bank_country) VALUES")
    rows = [f"  ('{t['id']}', '{d['payment_method']}', {q(d['sender_account'])}, {q(d['receiver_account'])}, '{d['receiver_bank_country']}')"
            for t in payments for d in [t["detail"]]]
    out.append(",\n".join(rows) + ";\n")

    cryptos = [t for t in all_txs if t["type"] == "CRYPTO"]
    out.append("INSERT INTO crypto_activity (transaction_id, blockchain, wallet_address_from, wallet_address_to, tx_hash, exchange_name) VALUES")
    rows = [f"  ('{t['id']}', '{d['blockchain']}', '{d['wallet_address_from']}', '{d['wallet_address_to']}', '{d['tx_hash']}', {q(d['exchange_name'])})"
            for t in cryptos for d in [t["detail"]]]
    out.append(",\n".join(rows) + ";\n")

    out.append("INSERT INTO risk_assessments (assessment_id, transaction_id, rule_id, triggered_at, score_contribution) VALUES")
    rows = [f"  ('{a['id']}', '{a['tx']}', '{a['rule']}', '{ts(a['at'])}', {a['score']:.2f})"
            for a in sorted(all_assessments, key=lambda a: a["at"])]
    out.append(",\n".join(rows) + ";\n")

    target = Path(__file__).resolve().parent.parent / "backend/src/main/resources/db/migration/V3__seed_demo_data.sql"
    target.write_text("\n".join(out), encoding="utf-8")

    print(f"Wrote {target}")
    print(f"{len(all_txs)} transactions, {len(all_assessments)} risk assessments\n")
    by_customer = {}
    for a in all_assessments:
        by_customer.setdefault(next(t for t in all_txs if t["id"] == a["tx"])["customer"].number, 0)
        by_customer[next(t for t in all_txs if t["id"] == a["tx"])["customer"].number] += a["score"]
    for c in CUSTOMERS:
        print(f"  {c.number}  {c.name:22s} {len(c.txs):4d} txs   risk score {by_customer.get(c.number, 0):7.2f}")


if __name__ == "__main__":
    main()

-- Reference data: demo operators and the risk rule catalogue.
-- Operator passwords (BCrypt): alice / operator123, bob / supervisor123 — demo credentials,
-- documented in the README.

INSERT INTO operators (operator_id, username, display_name, password_hash, role, created_at) VALUES
  ('0a000000-0000-0000-0000-000000000001', 'alice', 'Alice Meier',
   '$2a$10$z7HxnLrpkuydko95f65QZOgNzRzb4rij1mwCLA./JHh6hbMuzdD9e', 'OPERATOR',   '2025-01-10 08:00:00+00'),
  ('0a000000-0000-0000-0000-000000000002', 'bob', 'Bob Fontaine',
   '$2a$10$/6azumua0Q6Q7rA5/L5YzOhHu8KSs1Jiuy8gewZ47q7OdV8zaXuJu', 'SUPERVISOR', '2025-01-10 08:00:00+00');

INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES
  ('0b000000-0000-0000-0000-000000000001', 'High-value cross-border payment', 'PAYMENT',
   'amount >= 10000 AND receiver_bank_country <> customer.country', 25.00),
  ('0b000000-0000-0000-0000-000000000002', 'Payment to high-risk jurisdiction', 'PAYMENT',
   'receiver_bank_country IN (FATF high-risk list: IR, KP, MM)', 40.00),
  ('0b000000-0000-0000-0000-000000000003', 'Structuring: repeated sub-threshold payments', 'PAYMENT',
   '>= 3 outgoing payments with 9000 <= amount < 10000 within any rolling 7-day window', 30.00),
  ('0b000000-0000-0000-0000-000000000004', 'High-value card-not-present purchase', 'CARD',
   'card_present = false AND amount >= 2000', 15.00),
  ('0b000000-0000-0000-0000-000000000005', 'Repeated card declines', 'CARD',
   '>= 3 FAILED card transactions within a rolling 24-hour window', 20.00),
  ('0b000000-0000-0000-0000-000000000006', 'High-risk merchant category', 'CARD',
   'mcc_code IN (7995 gambling, 6051 quasi-cash, 4829 money transfer)', 15.00),
  ('0b000000-0000-0000-0000-000000000007', 'Transfer to unhosted or mixer-associated wallet', 'CRYPTO',
   'outgoing transfer where destination wallet is not attributed to a registered VASP', 45.00),
  ('0b000000-0000-0000-0000-000000000008', 'Rapid fiat-to-crypto pass-through', 'CRYPTO',
   'crypto outflow >= 20000 (fiat equivalent) within 72h of incoming wire(s) of comparable value', 30.00),
  ('0b000000-0000-0000-0000-000000000009', 'Activity velocity spike', 'ALL',
   'transaction count in trailing 7 days > 5x the trailing 90-day daily average', 20.00),
  ('0b000000-0000-0000-0000-000000000010', 'High-value crypto withdrawal', 'CRYPTO',
   'outgoing transfer to external wallet >= 10000 (fiat equivalent)', 25.00);

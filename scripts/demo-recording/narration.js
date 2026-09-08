// Narration script for the recorded demo.
//
// The on-screen captions live in record.js; this file adds (a) spoken lines for the
// slides, which carry no caption bar, and (b) pronunciation-friendly rewrites for
// technical terms so the TTS model reads them naturally. Anything not listed in
// SPOKEN_OVERRIDES is spoken exactly as captioned, after the SUBSTITUTIONS below.

const SLIDES = {
  title:
    'Customer Activity Analytics — a customer-care console for reviewing card, payment and crypto activity, ' +
    'with a persisted, policy-grounded A-I risk analysis. This is a recorded walkthrough of the Swissquote ' +
    'platform-engineering assignment, by Christos Topaloudis.',
  agenda:
    'The demo follows the five specification points in order: operator login; searching a customer and reviewing ' +
    'card, payment and crypto activity from the relational database; the activity overview and the A-I-powered analysis ' +
    'with a risk level, findings and recommendations; retrieval-augmented generation over internal policy documents; ' +
    'and persisted analyses that can be reviewed later. After that: the architecture, the A-I pipeline, and how the ' +
    'application was built with A-I agents.',
  architecture:
    'The architecture is three containers bound by one contract. In the browser, a React nineteen and TypeScript single-page ' +
    'application served by engine-x. Behind it, a Spring Boot three-point-five A-P-I on Java seventeen, organised in four ' +
    'packages: auth, with B-crypt operators and self-issued J-W-Ts; customer and transaction, whose J-P-A entities mirror ' +
    'the schema from the assignment; rag, which turns policy documents into embedded chunks in P-G-vector; and analysis, ' +
    'which runs the digest, retrieval, prompt, L-L-M port, validation and persistence. Postgres sixteen with the P-G-vector ' +
    'extension holds everything, migrated by Flyway. The L-L-M port has two adapters: a deterministic stub, which is the ' +
    'default and runs offline, and an Anthropic adapter that activates with an A-P-I key. The A-P-I contract in the docs ' +
    'folder is the source of truth for both sides.',
  pipeline:
    'Here is what happens when an operator clicks Run A-I analysis. First, an activity digest: counts, volumes, the ' +
    'triggered rules and the most relevant transactions — pseudonymised, so the model sees the customer number, never a ' +
    'name, e-mail or date of birth. Second, retrieval: a query built from the rule signals is embedded and matched against ' +
    'the policy chunks in P-G-vector, and the top six excerpts are attached. Third, the prompt: a system prompt with a strict ' +
    'JSON schema and grounding rules, and a user prompt carrying the digest and the excerpts. Fourth, the L-L-M port — stub ' +
    'or Claude, same JSON contract. Fifth, validation and persistence: the risk level is checked against the enum, citations ' +
    'are accepted only if the chunk was actually retrieved, failed runs are stored as failed, and the prompt and raw output ' +
    'are kept for audit. Five policy documents are ingested at startup, giving twenty-six chunks.',
  methodology:
    'How it was built: direct, constrain, verify. The specification came first — the schema and the A-P-I contract were ' +
    'designed and committed before any code. Standing rules for the agent live in the CLAUDE dot M-D file: the contract is ' +
    'the source of truth, no real personal data anywhere, the application must run offline, secrets only through the ' +
    'environment. Deterministic data came next: a generator with eight behaviour archetypes re-implements the ten risk rules, ' +
    'so the seeded signals are consistent with the catalogue. The backend was built by the main agent and the frontend by a ' +
    'parallel sub-agent working only against the frozen contract. Twenty-one unit tests and a Testcontainers journey test ' +
    'gave fast feedback. Finally, an adversarial review: six reviewer agents, every finding independently double-checked, ' +
    'fourteen confirmed defects, all fixed before delivery. It was built with Claude Code, and the full account is in the ' +
    'A-I methodology document.',
  run:
    'To run it yourself: docker compose up, dash dash build, starts the frontend, the A-P-I and the database — no A-P-I keys ' +
    'required. M-V-N-W verify runs the twenty-one unit tests and the Testcontainers integration test. Set an Anthropic A-P-I ' +
    'key to switch the analysis to Claude. The two demo operators are alice and bob; the eight customers are fictional, ' +
    'and all data is generated. The README covers architecture, design decisions and assumptions.',
  closing:
    'Thank you for watching. The repository is on GitHub under topless, slash, customer-activity-analytics. ' +
    'Christos Topaloudis.',
};

// caption text (exact) -> spoken text; use when a caption reads badly aloud
const SPOKEN_OVERRIDES = {
  'For the demo the credentials are one click away.': 'For the demo, the credentials are one click away.',
  '“weber” narrows the list to Lukas Weber; the results are server-side paged.':
    'Typing weber narrows the list to Lukas Weber. The results are paged on the server side.',
  'A customer number works the same way.': 'A customer number works the same way.',
};

// substitutions applied to every spoken line (order matters)
const SUBSTITUTIONS = [
  [/\bCUST-(\d{5})\b/g, (m, n) => `customer ${n.split('').join(' ')}`],
  [/\bpgvector\b/gi, 'P-G-vector'],
  [/\bHS256\b/g, 'H-S-two-five-six'],
  [/\bBCrypt\b/g, 'B-crypt'],
  [/\bJWT\b/g, 'J-W-T'],
  [/\bJWTs\b/g, 'J-W-Ts'],
  [/\bAI\b/g, 'A-I'],
  [/\bAML\b/g, 'A-M-L'],
  [/\bKYC\b/g, 'K-Y-C'],
  [/\bFATF\b/g, 'F-A-T-F'],
  [/\bMROS\b/g, 'M-ROS'],
  [/\bRAG\b/g, 'rag'],
  [/\bUUID\b/g, 'U-U-I-D'],
  [/\bLLM\b/g, 'L-L-M'],
  [/\bJSON\b/g, 'JSON'],
  [/\bSWIFT\b/g, 'Swift'],
  [/\bBTC\b/g, 'bitcoin'],
  [/\bCHF\b/g, 'Swiss francs'],
  [/\bUAE\b/g, 'U-A-E'],
  [/\bSVG\b/g, 'S-V-G'],
  [/\bAPI\b/g, 'A-P-I'],
  [/\bCRITICAL\b/g, 'critical'],
  [/\bHIGH\b/g, 'high'],
  [/\bLOW\b/g, 'low'],
  [/\bFAILED\b/g, 'failed'],
  [/\bPAYMENT\b/g, 'payment'],
  [/\bCRYPTO\b/g, 'crypto'],
  [/\brisk_rules\b/g, 'risk rules'],
  [/\brisk_assessments\b/g, 'risk assessments'],
  [/\bpayment_activity\b/g, 'payment activity'],
  [/27’300/g, 'twenty-seven thousand three hundred'],
  [/25’000/g, 'twenty-five thousand'],
  [/\b565\b/g, 'five hundred sixty-five'],
  [/\s—\s/g, ', '],
  [/[“”]/g, ''],
  [/…/g, '...'],
];

function spoken(captionText) {
  let text = SPOKEN_OVERRIDES[captionText] ?? captionText;
  for (const [pattern, replacement] of SUBSTITUTIONS) {
    text = text.replace(pattern, replacement);
  }
  return text;
}

module.exports = { SLIDES, spoken };

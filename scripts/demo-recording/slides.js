// Inline "slide" pages shown between app segments of the recorded demo.
// Styled with the application's own design tokens so the video reads as one piece.

const CSS = `
  * { box-sizing: border-box; }
  html, body { margin: 0; height: 100%; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI Variable Text", "Segoe UI",
      system-ui, ui-sans-serif, Helvetica, Arial, sans-serif;
    color: #17263c; background: #f2f4f8; -webkit-font-smoothing: antialiased;
  }
  .mono { font-family: ui-monospace, "SF Mono", SFMono-Regular, Menlo, Consolas, monospace; }
  .slide { height: 100vh; display: flex; flex-direction: column; padding: 64px 96px 48px; }
  .slide.dark { background: #142033; color: #eef2f8; }
  .kicker { font-size: 13px; letter-spacing: .14em; text-transform: uppercase; color: #8494ab; font-weight: 600; }
  .dark .kicker { color: #93a3bc; }
  h1 { font-size: 46px; line-height: 1.1; margin: 14px 0 10px; letter-spacing: -0.02em; font-weight: 700; }
  h2 { font-size: 36px; line-height: 1.15; margin: 10px 0 28px; letter-spacing: -0.02em; font-weight: 700; }
  .lead { font-size: 22px; color: #51617a; max-width: 980px; line-height: 1.45; }
  .dark .lead { color: #c5cfdd; }
  .footer { margin-top: auto; display: flex; justify-content: space-between; font-size: 13px; color: #8494ab; }
  .dark .footer { color: #93a3bc; }
  ul.big { font-size: 22px; line-height: 1.5; padding-left: 26px; margin: 0; max-width: 1100px; }
  ul.big li { margin: 0 0 12px; }
  ul.big li b { color: #17263c; }
  .grid { display: grid; gap: 22px; }
  .box { background: #fff; border: 1px solid #e3e8f0; border-radius: 12px; padding: 18px 20px;
         box-shadow: 0 1px 2px rgba(23,38,60,.06); }
  .box h3 { margin: 0 0 8px; font-size: 17px; letter-spacing: .02em; text-transform: uppercase; color: #51617a; }
  .box p, .box li { font-size: 17px; line-height: 1.45; margin: 0; color: #17263c; }
  .box ul { margin: 6px 0 0; padding-left: 20px; }
  .arrow { align-self: center; font-size: 34px; color: #8494ab; text-align: center; }
  .chip { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 12px; font-weight: 700;
          letter-spacing: .06em; text-transform: uppercase; background: #e9effc; color: #1c419c; margin-right: 6px; }
  .chip.green { background: #e0f4e8; color: #14683e; }
  .chip.violet { background: #efe6f8; color: #5b2d8a; }
  .brand { display: flex; align-items: center; gap: 14px; margin-bottom: 30px; }
  .brand-mark { width: 44px; height: 44px; border-radius: 10px; background: #2554c7; display: grid; place-items: center; }
  .brand-mark i { display: block; width: 24px; height: 18px;
    background: linear-gradient(to top, #fff 0 34%, transparent 34%) 0 100%/6px 100% no-repeat,
                linear-gradient(to top, #fff 0 66%, transparent 66%) 9px 100%/6px 100% no-repeat,
                linear-gradient(to top, #fff 0 100%, transparent 100%) 18px 100%/6px 100% no-repeat; }
  .brand-name { font-size: 15px; letter-spacing: .12em; text-transform: uppercase; font-weight: 700; }
  .brand-name span { font-weight: 300; }
  .steps { display: flex; align-items: stretch; gap: 10px; }
  .step { flex: 1; background: #fff; border: 1px solid #e3e8f0; border-radius: 12px; padding: 16px 16px; }
  .step .n { font-size: 12px; font-weight: 700; color: #2554c7; letter-spacing: .1em; }
  .step h4 { margin: 6px 0 6px; font-size: 18px; }
  .step p { margin: 0; font-size: 15px; color: #51617a; line-height: 1.4; }
  code { font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace; font-size: .95em;
         background: #e9effc; color: #1c419c; padding: 1px 6px; border-radius: 5px; }
  pre { font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace; font-size: 19px; background: #142033;
        color: #eef2f8; padding: 20px 24px; border-radius: 12px; line-height: 1.55; margin: 0 0 18px; }
  pre b { color: #ffd479; font-weight: 600; }
  .two { display: grid; grid-template-columns: 1fr 1fr; gap: 36px; }
`;

const brand = `<div class="brand"><div class="brand-mark"><i></i></div>
  <div class="brand-name">Customer Activity <span>Analytics</span></div></div>`;

const footer = (right) =>
  `<div class="footer"><span>Customer Activity Analytics — Swissquote platform-engineering assignment</span><span>${right}</span></div>`;

function wrap(body, dark = false) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>${CSS}</style></head>
  <body><div class="slide${dark ? ' dark' : ''}">${body}</div></body></html>`;
}

const slides = {
  title: () => wrap(`
    ${brand}
    <div class="kicker">Swissquote · platform engineering assignment</div>
    <h1>Customer Activity Analytics</h1>
    <p class="lead">A customer-care console for reviewing card, payment and crypto activity —
      with a persisted, policy-grounded AI risk analysis.</p>
    <p class="lead" style="margin-top:34px">Christos Topaloudis · narrated walkthrough, ~15 minutes, captions included · includes a live Claude analysis</p>
    ${footer('Spring Boot 3.5 · React 19 · PostgreSQL + pgvector')}`, true),

  agenda: () => wrap(`
    ${brand}
    <div class="kicker">What this demo covers</div>
    <h2>The five specification points, in order</h2>
    <ul class="big">
      <li><b>Operator login</b> — different operators, BCrypt + stateless JWT <span class="chip">spec 3</span></li>
      <li><b>Search by customer ID</b> and review card / payment / crypto activity from the relational DB <span class="chip">spec 1</span></li>
      <li><b>Activity overview</b> dashboard, then an <b>AI-powered analysis</b>: risk level, findings, recommendations <span class="chip">spec 2</span></li>
      <li><b>RAG</b> — the analysis cites internal policy documents retrieved from a vector store <span class="chip">spec 4</span></li>
      <li><b>Persisted analyses</b> — every run stored with requester, model, prompt and raw output; reviewable later <span class="chip">spec 5</span></li>
    </ul>
    <p class="lead" style="margin-top:28px">Then: architecture, the AI pipeline, and how the application was built with AI agents.</p>
    ${footer('1 / 4')}`),

  architecture: () => wrap(`
    ${brand}
    <div class="kicker">Architecture</div>
    <h2>Three containers, one contract</h2>
    <div class="grid" style="grid-template-columns: 1fr 48px 1.25fr 48px 1fr; align-items: stretch">
      <div class="box"><h3>Browser</h3>
        <p><b>React 19 + TypeScript</b> SPA, hand-rolled CSS/SVG, served by nginx on <span class="mono">:3000</span>.</p>
        <ul><li>Login, customer search, dashboard</li><li>Transactions with type-specific details</li><li>AI analysis panel + history</li></ul>
        <p style="margin-top:10px"><span class="chip">docs/api-contract.md</span> binds both sides</p></div>
      <div class="arrow">→</div>
      <div class="box"><h3>API — Spring Boot 3.5 / Java 17</h3>
        <ul>
          <li><b>auth</b> — BCrypt operators, self-issued HS256 JWT, rate-limited login</li>
          <li><b>customer / transaction</b> — JPA entities mirror the handout schema; SQL aggregates for the overview</li>
          <li><b>rag</b> — policy markdown → chunks → embeddings → pgvector; cosine top-k retrieval</li>
          <li><b>analysis</b> — digest → retrieval → prompt → <code>LlmClient</code> port → validation → persistence</li>
        </ul>
        <p style="margin-top:10px"><span class="chip green">StubLlmClient (default, offline)</span><span class="chip violet">AnthropicLlmClient (API key)</span></p></div>
      <div class="arrow">→</div>
      <div class="box"><h3>Data — PostgreSQL 16 + pgvector</h3>
        <p>Flyway migrations: schema, rule catalogue, deterministic demo dataset.</p>
        <ul><li>transactions + card / payment / crypto detail tables</li><li>risk_rules, risk_assessments</li>
            <li>operators</li><li>policy_documents, policy_chunks <span class="mono">vector(384)</span></li><li>ai_analyses (JSONB findings, prompt, raw response)</li></ul></div>
    </div>
    ${footer('2 / 4')}`),

  pipeline: () => wrap(`
    ${brand}
    <div class="kicker">AI analysis pipeline</div>
    <h2>What happens when an operator clicks “Run AI analysis”</h2>
    <div class="steps">
      <div class="step"><div class="n">01</div><h4>Activity digest</h4><p>Counts, volumes, triggered rules and the most relevant transactions — <b>pseudonymised</b>: customer number only, never name, e-mail or date of birth.</p></div>
      <div class="step"><div class="n">02</div><h4>Policy retrieval (RAG)</h4><p>A query built from the rule signals is embedded and matched against policy chunks in pgvector; top-6 excerpts are attached.</p></div>
      <div class="step"><div class="n">03</div><h4>Prompt</h4><p>System prompt with a strict JSON schema and grounding rules; user prompt with the digest and the cited excerpts.</p></div>
      <div class="step"><div class="n">04</div><h4>LLM port</h4><p><code>LlmClient</code>: deterministic stub by default; Anthropic Claude when <code>ANTHROPIC_API_KEY</code> is set — same JSON contract.</p></div>
      <div class="step"><div class="n">05</div><h4>Validate &amp; persist</h4><p>Risk level checked against the enum, citations accepted only if actually retrieved, failed runs stored as FAILED. Prompt + raw output kept for audit.</p></div>
    </div>
    <p class="lead" style="margin-top:30px">Five policy documents (AML monitoring, card fraud, crypto assets, high-risk jurisdictions, escalation &amp; CDD) are ingested at startup — 26 chunks.</p>
    ${footer('3 / 4')}`),

  methodology: () => wrap(`
    ${brand}
    <div class="kicker">How it was built — AI-assisted methodology</div>
    <h2>Direct, constrain, verify</h2>
    <div class="two">
      <ul class="big">
        <li><b>Spec first.</b> Schema and <code>docs/api-contract.md</code> designed and committed before any code.</li>
        <li><b>Standing agent rules</b> in <code>CLAUDE.md</code>: contract is the source of truth, no real PII, must run offline, secrets via env only.</li>
        <li><b>Deterministic data first.</b> A generator with 8 behaviour archetypes re-implements the 10 risk rules, so the seeded signals are consistent.</li>
      </ul>
      <ul class="big">
        <li><b>Parallel agents.</b> Backend by the main agent; frontend by a subagent working only against the frozen contract.</li>
        <li><b>Tests as feedback.</b> 21 unit tests + a Testcontainers journey test (login → search → analysis → history).</li>
        <li><b>Adversarial review.</b> Six reviewer agents, every finding double-verified: 14 confirmed defects, all fixed before delivery.</li>
      </ul>
    </div>
    <p class="lead" style="margin-top:26px">Built with Claude Code (Claude Fable 5). Full account in <code>docs/ai-methodology.md</code>.</p>
    ${footer('4 / 4')}`),

  run: () => wrap(`
    ${brand}
    <div class="kicker">Run it yourself</div>
    <h2>One command, no API keys required</h2>
    <pre><b>docker compose up --build</b>      # frontend :3000 · API :8080 · pgvector :5433
<b>./mvnw verify</b>                   # 21 unit tests + Testcontainers integration test
<b>ANTHROPIC_API_KEY=… docker compose up</b>   # switch the analysis to Claude</pre>
    <ul class="big">
      <li>Demo operators: <code>alice / operator123</code> (operator), <code>bob / supervisor123</code> (supervisor)</li>
      <li>Eight fictional customers with distinct risk profiles; all data is generated, none is real</li>
      <li>README covers architecture, design decisions and assumptions; <code>docs/</code> has the contract, architecture and methodology notes</li>
    </ul>
    ${footer('github.com/topless/customer-activity-analytics')}`),

  live: () => wrap(`
    ${brand}
    <div class="kicker">Same pipeline, real model</div>
    <h2>Switching the analysis to Claude — one variable, no code change</h2>
    <pre><b>ANTHROPIC_API_KEY=sk-ant-…</b> docker compose up -d backend</pre>
    <ul class="big">
      <li><code>LlmClientConfig</code> selects <code>AnthropicLlmClient</code> whenever a key is configured; the stub stays the offline default.</li>
      <li>Same digest, same policy retrieval, same prompt and JSON contract, same validation and persistence — only the port's adapter changes.</li>
      <li>The backend is restarting with the key right now; next, the same operator re-runs the same customer.</li>
    </ul>
    <p class="lead" style="margin-top:26px">Model: <code>claude-sonnet-5</code> via the Anthropic Messages API; prompts and raw responses are stored with every run for audit.</p>
    ${footer('live model')}`),

  closing: () => wrap(`
    ${brand}
    <div class="kicker">Thank you</div>
    <h1>Customer Activity Analytics</h1>
    <p class="lead">Repository: <span class="mono">github.com/topless/customer-activity-analytics</span></p>
    <p class="lead" style="margin-top:22px">Christos Topaloudis · topless@gmail.com</p>
    ${footer('Spring Boot 3.5 · React 19 · PostgreSQL + pgvector · Claude')}`, true),
};

module.exports = { slides };

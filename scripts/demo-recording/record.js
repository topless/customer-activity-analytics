// Records the captioned demo walkthrough of the running application (default
// http://localhost:3000) with Playwright. Produces out/demo.webm and out/demo.srt.
//
//   node record.js            # full walkthrough (~12 min)
//   APP_URL=... node record.js
//
// The page gets an injected caption bar and a visible animated cursor so the
// recording is self-explanatory without narration.

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { slides } = require('./slides');

const APP = process.env.APP_URL || 'http://localhost:3000';
const OUT = path.join(__dirname, 'out');
const W = 1440;
const H = 900;
const SPEED = Number(process.env.SPEED || 1); // >1 = faster (for dry runs)

const LUKAS = 'CUST-10004';
const ELENA = 'CUST-10005';
const ANNA = 'CUST-10001';

// ---------------------------------------------------------------- overlay

const OVERLAY_SCRIPT = `
(() => {
  const css = \`
    #__cap { position: fixed; left: 50%; bottom: 30px; transform: translateX(-50%);
      max-width: 1120px; width: max-content; padding: 15px 24px 16px; border-radius: 14px;
      background: rgba(20, 32, 51, 0.94); color: #f4f7fb; font: 500 21px/1.4 -apple-system,
      BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif; letter-spacing: -0.005em;
      box-shadow: 0 10px 30px rgba(0,0,0,.28); z-index: 2147483646; pointer-events: none;
      opacity: 0; transition: opacity .25s ease; text-align: center; }
    #__cap.show { opacity: 1; }
    #__cap .k { display: block; font-size: 12px; font-weight: 700; letter-spacing: .14em;
      text-transform: uppercase; color: #9fb3d1; margin-bottom: 5px; }
    #__cur { position: fixed; left: 0; top: 0; width: 26px; height: 26px; z-index: 2147483647;
      pointer-events: none; transform: translate(-100px, -100px); transition: transform .04s linear;
      filter: drop-shadow(0 1px 2px rgba(0,0,0,.45)); }
    #__rip { position: fixed; width: 34px; height: 34px; border-radius: 50%; border: 3px solid #2554c7;
      z-index: 2147483645; pointer-events: none; opacity: 0; transform: translate(-50%, -50%) scale(.3); }
    #__rip.go { animation: __ripple .5s ease-out; }
    @keyframes __ripple { 0% { opacity: .9; transform: translate(-50%,-50%) scale(.3); }
      100% { opacity: 0; transform: translate(-50%,-50%) scale(1.4); } }
  \`;
  function install() {
    if (document.getElementById('__cap')) return;
    const style = document.createElement('style'); style.textContent = css;
    document.head.appendChild(style);
    const cap = document.createElement('div'); cap.id = '__cap';
    const cur = document.createElement('div'); cur.id = '__cur';
    cur.innerHTML = '<svg width="26" height="26" viewBox="0 0 24 24"><path d="M5.5 3.2 19 12.6l-6.3 1.1 3.5 6.2-2.5 1.3-3.4-6.2-4.8 4.2z" fill="#fff" stroke="#17263c" stroke-width="1.4" stroke-linejoin="round"/></svg>';
    const rip = document.createElement('div'); rip.id = '__rip';
    document.body.append(cap, cur, rip);
    window.addEventListener('mousemove', (e) => {
      cur.style.transform = 'translate(' + e.clientX + 'px,' + e.clientY + 'px)';
    }, true);
    window.addEventListener('mousedown', (e) => {
      rip.style.left = e.clientX + 'px'; rip.style.top = e.clientY + 'px';
      rip.classList.remove('go'); void rip.offsetWidth; rip.classList.add('go');
    }, true);
  }
  window.__caption = (text, kicker) => {
    install();
    const cap = document.getElementById('__cap');
    if (!text) { cap.classList.remove('show'); return; }
    cap.innerHTML = (kicker ? '<span class="k"></span>' : '');
    if (kicker) cap.querySelector('.k').textContent = kicker;
    cap.appendChild(document.createTextNode(text));
    cap.classList.add('show');
  };
  window.__cursorHome = () => { install(); document.getElementById('__cur').style.transform = 'translate(-100px,-100px)'; };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', install);
  else install();
})();
`;

// ---------------------------------------------------------------- helpers

const timeline = [];
let t0 = 0;
let page;
let stepNo = 0;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms / SPEED));
const now = () => (Date.now() - t0) / 1000;

async function caption(text, { kicker, hold } = {}) {
  const start = now();
  if (timeline.length) timeline[timeline.length - 1].end = start - 0.1;
  timeline.push({ start, text, end: null });
  console.log(`[${start.toFixed(1).padStart(6)}s] ${kicker ? kicker + ' — ' : ''}${text.slice(0, 90)}`);
  await page.evaluate(([t, k]) => window.__caption(t, k), [text, kicker || null]);
  const ms = hold ?? Math.max(4600, 2900 + text.length * 76);
  await sleep(ms);
}

async function clearCaption() {
  if (timeline.length && timeline[timeline.length - 1].end === null) {
    timeline[timeline.length - 1].end = now();
  }
  await page.evaluate(() => window.__caption(''));
}

async function moveTo(target, { dx = 0, dy = 0, steps = 28 } = {}) {
  let x;
  let y;
  if (typeof target === 'object' && 'x' in target) {
    ({ x, y } = target);
  } else {
    const box = await target.boundingBox();
    if (!box) throw new Error('element not visible for moveTo');
    x = box.x + box.width / 2;
    y = box.y + box.height / 2;
  }
  await page.mouse.move(x + dx, y + dy, { steps });
  await sleep(260);
}

async function click(target, opts = {}) {
  await moveTo(target, opts);
  // the page may have re-rendered while the cursor travelled: re-measure before pressing
  if (typeof target === 'object' && !('x' in target)) {
    const box = await target.boundingBox();
    if (box) await page.mouse.move(box.x + box.width / 2 + (opts.dx || 0), box.y + box.height / 2 + (opts.dy || 0), { steps: 4 });
  }
  await page.mouse.down();
  await sleep(90);
  await page.mouse.up();
  await sleep(opts.after ?? 700);
}

async function typeSlowly(locator, text) {
  await click(locator, { after: 250 });
  await page.keyboard.type(text, { delay: 95 / SPEED });
  await sleep(900);
}

async function clearInput(locator) {
  await click(locator, { after: 150 });
  await page.keyboard.press('ControlOrMeta+A');
  await page.keyboard.press('Backspace');
  await sleep(700);
}

async function scrollTo(locator, block = 'start') {
  await locator.evaluate((el, b) => el.scrollIntoView({ behavior: 'smooth', block: b }), block);
  await sleep(1000);
}

async function scrollToTop() {
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
  await sleep(900);
}

async function showSlide(name, hold) {
  await page.setContent(slides[name]());
  await page.evaluate(() => window.__cursorHome && window.__cursorHome());
  timeline.push({ start: now(), text: `[slide: ${name}]`, end: null, slide: true });
  await sleep(hold);
  timeline[timeline.length - 1].end = now();
}

async function gotoApp(pathname = '/') {
  await page.goto(APP + pathname, { waitUntil: 'networkidle' });
  await sleep(600);
}

function section(title) {
  stepNo += 1;
  return `${String(stepNo).padStart(2, '0')} · ${title}`;
}

// ---------------------------------------------------------------- app steps

async function login(username, kicker) {
  await page.waitForSelector('#login-username');
  await caption('Operators sign in with their own credentials. Passwords are BCrypt-hashed in the operators table; a successful login returns a stateless HS256 JWT that every other endpoint requires.', { kicker });
  const cred = page.locator('.login-cred', { hasText: username });
  await caption('For the demo the credentials are one click away.', { kicker, hold: 3800 });
  await click(cred);
  await click(page.locator('button.btn-primary', { hasText: 'Sign in' }), { after: 1400 });
  await page.waitForSelector('input[aria-label="Search customers"]');
}

async function searchAndOpen(query, customerNumber) {
  const input = page.locator('input[aria-label="Search customers"]');
  await clearInput(input);
  await typeSlowly(input, query);
  // let the debounced search settle on exactly the one matching customer
  await page.waitForFunction(() => document.querySelectorAll('tr.row-link').length === 1, null, { timeout: 15000 });
  await sleep(500);
  const row = page.locator('tr.row-link', { hasText: customerNumber });
  await row.waitFor();
  await click(row, { after: 1200 });
  await page.waitForSelector('.risk-hero-value');
  await page.waitForSelector('section[aria-label="AI risk analysis"]');
  await sleep(600);
}

async function runAnalysis() {
  const ai = page.locator('section[aria-label="AI risk analysis"]');
  const before = await ai.locator('.history-row').count();
  const button = ai.locator('button', { hasText: 'Run AI analysis' });
  await click(button, { after: 300 });
  // a new run is done when it shows up in the history list (a banner may already exist)
  await page.waitForFunction(
    ([sel, n]) => document.querySelectorAll(sel).length > n,
    ['section[aria-label="AI risk analysis"] .history-row', before],
    { timeout: 120000 },
  );
  await page.waitForSelector('.analysis-banner-title', { timeout: 120000 });
  await sleep(900);
}

async function walkthrough() {
  const k = section('Sign in');
  await gotoApp('/login');
  await login('alice', k);

  // ---- customer search
  const kSearch = section('Customer search');
  await caption('Signed in as Alice, an operator. The landing page lists the customer base with a live risk score per customer, summed from the rule-based risk assessments in the database.', { kicker: kSearch });
  const input = page.locator('input[aria-label="Search customers"]');
  await caption('Search works by customer number, name, or the raw customer UUID — as a prefix or a name fragment.', { kicker: kSearch, hold: 4000 });
  await typeSlowly(input, 'weber');
  await caption('“weber” narrows the list to Lukas Weber; the results are server-side paged.', { kicker: kSearch, hold: 4200 });
  await clearInput(input);
  await typeSlowly(input, 'CUST-10005');
  await caption('A customer number works the same way.', { kicker: kSearch, hold: 4200 });
  await clearInput(input);
  await caption('Back to the full list. Risk badges: green is routine, amber and orange need attention, red means high risk — Lukas Weber, at 565, is where we go next.', { kicker: kSearch });
  const lukasRow = page.locator('tr.row-link', { hasText: LUKAS });
  await moveTo(lukasRow);
  await sleep(500);
  await click(lukasRow, { after: 1300 });
  await page.waitForSelector('.risk-hero-value');

  // ---- dashboard
  const kDash = section('Activity dashboard');
  await caption('The customer dashboard. Profile facts and KYC level on the left; the risk score hero on the right.', { kicker: kDash });
  await caption('Overview cards break the six-month window down per activity type: counts, failures, and volumes per currency — nothing is converted between currencies.', { kicker: kDash });
  await sleep(1500);
  const chart = page.locator('section[aria-label="Monthly activity"]');
  await scrollTo(chart, 'start');
  await caption('Monthly activity mix — card, payment and crypto counts. The chart is hand-rolled SVG; hovering a month shows its figures.', { kicker: kDash, hold: 3800 });
  const svg = chart.locator('svg');
  const box = await svg.boundingBox();
  if (box) {
    const y = box.y + box.height * 0.5;
    for (const frac of [0.14, 0.3, 0.47, 0.62, 0.78, 0.9]) {
      await page.mouse.move(box.x + box.width * frac, y, { steps: 14 });
      await sleep(650);
    }
  }
  const rules = page.locator('section[aria-label="Triggered risk rules"]');
  await moveTo(rules, { dy: -60 });
  await caption('Triggered risk rules — the deterministic rule layer from the risk_rules and risk_assessments tables: structuring, transfers to an unhosted wallet, high-value cross-border payments, a payment to a FATF-listed jurisdiction.', { kicker: kDash });
  await rules.evaluate((el) => {
    const list = el.querySelector('.rules-list');
    if (list) list.scrollTo({ top: 400, behavior: 'smooth' });
  });
  await sleep(1600);

  // ---- transactions
  const kTx = section('Transactions');
  const txSection = page.locator('section[aria-label="Transactions"]');
  await scrollTo(txSection, 'start');
  await caption('All activity in one table, newest first, with the rule annotations per transaction. Filters go to the API; the page is server-side paged.', { kicker: kTx });
  const typeSelect = page.locator('#tx-type');
  await moveTo(typeSelect);
  await typeSelect.selectOption('PAYMENT');
  await sleep(1200);
  await caption('Payments only. Expanding a row shows the payment-specific details from the payment_activity table.', { kicker: kTx, hold: 3800 });
  const swiftRow = page.locator('tbody tr', { hasText: /27.300\.00/ }).first();
  await swiftRow.waitFor();
  await click(swiftRow.locator('.tx-expand-btn'), { after: 900 });
  await scrollTo(swiftRow, 'center');
  await caption('A 27’300 CHF SWIFT transfer to the UAE: accounts, method, receiver bank country, and the rules it triggered — a high-value cross-border payment.', { kicker: kTx });
  await clearCaption();
  const statusSelect = page.locator('#tx-status');
  await moveTo(statusSelect);
  await statusSelect.selectOption('FAILED');
  await sleep(1200);
  const failedRow = page.locator('tbody tr', { hasText: 'MM' }).first();
  await failedRow.waitFor();
  await click(failedRow.locator('.tx-expand-btn'), { after: 900 });
  await caption('Filtering by status FAILED surfaces the blocked 25’000 CHF attempt to a high-risk jurisdiction — the single highest-weighted signal in the catalogue.', { kicker: kTx });
  await clearCaption();
  await moveTo(statusSelect);
  await statusSelect.selectOption('');
  await moveTo(typeSelect);
  await typeSelect.selectOption('CRYPTO');
  await sleep(1200);
  const cryptoRow = page.locator('tbody tr').first();
  await click(cryptoRow.locator('.tx-expand-btn'), { after: 900 });
  await caption('Crypto activity carries chain, wallets and transaction hash. This BTC transfer goes to a wallet with no exchange attribution — an unhosted counterparty, reused across several transfers.', { kicker: kTx });
  await moveTo(typeSelect);
  await typeSelect.selectOption('');
  await sleep(800);

  // ---- AI analysis
  const kAi = section('AI risk analysis');
  const ai = page.locator('section[aria-label="AI risk analysis"]');
  await scrollTo(ai, 'start');
  await caption('Now the AI analysis. Nothing has been run for this customer yet, so the history is empty.', { kicker: kAi, hold: 4000 });
  await caption('Clicking “Run AI analysis” builds a pseudonymised activity digest, retrieves the relevant policy chunks from pgvector, prompts the model through the LLM port, validates the JSON and persists the result.', { kicker: kAi });
  await runAnalysis();
  await caption('CRITICAL. The banner carries the level, timestamp, transaction count, the model that produced it, and who requested it.', { kicker: kAi });
  await caption('The summary is a short narrative for the operator; findings are quantified and each links to the concrete transactions behind it.', { kicker: kAi });
  const findings = ai.locator('.finding-detail').first();
  await scrollTo(findings, 'center');
  await sleep(3500);
  const recs = ai.locator('.recommendation-list');
  await scrollTo(recs, 'center');
  await caption('Recommendations follow the escalation policy: 24-hour AML escalation, restrictions pending review, a case file for a possible MROS report.', { kicker: kAi });
  const cited = ai.locator('.policy-excerpt').first();
  await scrollTo(cited, 'center');
  await caption('And the RAG grounding: the policy excerpts the analysis cites, retrieved from the vector store for this case. Citations are accepted only if the chunk was actually retrieved.', { kicker: kAi });
  await scrollTo(ai, 'start');
  await sleep(600);

  // ---- differentiation: Elena, Anna
  const kDiff = section('Different customers, different answers');
  await scrollToTop();
  await click(page.locator('.back-link'), { after: 1200 });
  await caption('Two more customers to show the analysis differentiates. Elena has gambling-heavy card use and a burst of declines.', { kicker: kDiff, hold: 4200 });
  await searchAndOpen(ELENA, ELENA);
  await scrollTo(page.locator('section[aria-label="AI risk analysis"]'), 'start');
  await runAnalysis();
  await caption('HIGH — driven by the high-risk merchant category and repeated declines, with matching recommendations: affordability checks, a card-limit conversation.', { kicker: kDiff });
  await scrollToTop();
  await click(page.locator('.back-link'), { after: 1200 });
  await caption('Anna is a routine retail customer.', { kicker: kDiff, hold: 4200 });
  await searchAndOpen(ANNA, ANNA);
  await scrollTo(page.locator('section[aria-label="AI risk analysis"]'), 'start');
  await runAnalysis();
  await caption('LOW — one minor card-not-present signal; no action beyond routine monitoring.', { kicker: kDiff });

  // ---- second operator + history
  const kHist = section('Persisted analyses & a second operator');
  await scrollToTop();
  await click(page.locator('.back-link'), { after: 1200 });
  await caption('Analyses are persisted and reviewable later. Let’s sign out and come back as a different operator.', { kicker: kHist, hold: 4200 });
  await click(page.locator('.btn-logout'), { after: 1200 });
  await page.waitForSelector('#login-username');
  await click(page.locator('.login-cred', { hasText: 'bob' }));
  await click(page.locator('button.btn-primary', { hasText: 'Sign in' }), { after: 1400 });
  await page.waitForSelector('.topbar-name');
  await caption('Bob is a supervisor — the role shows in the header.', { kicker: kHist, hold: 3600 });
  // the login flow returns to the page we signed out from; go to the customer list via the brand link
  await click(page.locator('.brand'), { after: 900 });
  await page.waitForSelector('input[aria-label="Search customers"]');
  await searchAndOpen('weber', LUKAS);
  const ai2 = page.locator('section[aria-label="AI risk analysis"]');
  await scrollTo(ai2, 'start');
  await caption('Lukas Weber again. The panel opens on the latest analysis — Alice’s run from a few minutes ago — and the history lists every run.', { kicker: kHist });
  await runAnalysis();
  await caption('Bob’s own run is now on top. Each entry records who requested it, when, the model and the outcome; failed runs are stored and shown too.', { kicker: kHist });
  const older = ai2.locator('.history-row').last();
  await click(older, { after: 1200 });
  await caption('Selecting an older entry reloads that analysis exactly as it was produced — the prompt and raw model output are kept alongside it for audit.', { kicker: kHist });
  await clearCaption();
  await sleep(600);
}

// ---------------------------------------------------------------- main

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: W, height: H },
    deviceScaleFactor: 1,
    recordVideo: { dir: OUT, size: { width: W, height: H } },
    colorScheme: 'light',
  });
  await context.addInitScript(OVERLAY_SCRIPT);
  page = await context.newPage();
  t0 = Date.now();

  await showSlide('title', 8000);
  await showSlide('agenda', 18000);
  await showSlide('architecture', 40000);

  await walkthrough();

  await showSlide('pipeline', 40000);
  await showSlide('methodology', 42000);
  await showSlide('run', 20000);
  await showSlide('closing', 8000);

  const video = page.video();
  await context.close();
  await browser.close();

  const tmp = await video.path();
  const webm = path.join(OUT, 'demo.webm');
  fs.renameSync(tmp, webm);

  // captions as SRT (burned-in captions remain the primary channel)
  const srt = timeline
    .filter((c) => !c.slide)
    .map((c, i) => `${i + 1}\n${ts(c.start)} --> ${ts(c.end ?? c.start + 4)}\n${c.text}\n`)
    .join('\n');
  fs.writeFileSync(path.join(OUT, 'demo.srt'), srt);
  fs.writeFileSync(path.join(OUT, 'timeline.json'), JSON.stringify(timeline, null, 1));
  console.log(`recorded ${webm} (${now().toFixed(0)} s), ${timeline.length} timeline entries`);
}

function ts(seconds) {
  const s = Math.max(0, seconds);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = Math.floor(s % 60);
  const ms = Math.round((s - Math.floor(s)) * 1000);
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')},${String(ms).padStart(3, '0')}`;
}

main().catch(async (e) => {
  console.error(e);
  try {
    if (page) await page.screenshot({ path: path.join(OUT, 'failure.png') });
  } catch (_) { /* best effort */ }
  process.exit(1);
});

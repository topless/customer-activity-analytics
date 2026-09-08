// Regenerates the README screenshots from the running application (default
// http://localhost:3000) into docs/screenshots. Run after a demo recording so the
// analysis history contains both stub and live-model runs.
//
//   node screenshots.js

const { chromium } = require('playwright');
const path = require('path');

const APP = process.env.APP_URL || 'http://localhost:3000';
const OUT = path.resolve(__dirname, '..', '..', 'docs', 'screenshots');
const LUKAS = 'c0000000-0000-0000-0000-000000000004';

async function main() {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  // park the cursor so no row carries a hover highlight
  const shot = async (name, opts = {}) => {
    await page.mouse.move(1439, 899);
    await page.waitForTimeout(150);
    await page.screenshot({ path: path.join(OUT, name), ...opts });
  };

  // 1. login
  await page.goto(APP + '/login', { waitUntil: 'networkidle' });
  await page.locator('.login-cred', { hasText: 'alice' }).click();
  await shot('login.png');
  await page.locator('button.btn-primary', { hasText: 'Sign in' }).click();
  await page.waitForSelector('input[aria-label="Search customers"]');

  // 2. customer list
  await page.waitForSelector('tr.row-link');
  await page.waitForTimeout(400);
  await shot('customers.png');

  // 3. dashboard
  await page.goto(APP + '/customers/' + LUKAS, { waitUntil: 'networkidle' });
  await page.waitForSelector('.risk-hero-value');
  await page.waitForSelector('section[aria-label="Monthly activity"] svg rect');
  await page.waitForTimeout(900); // bar animation
  await shot('dashboard.png');

  // 4. transactions: payments, one row expanded
  await page.locator('#tx-type').selectOption('PAYMENT');
  const row = page.locator('tbody tr', { hasText: /27.300\.00/ }).first();
  await row.waitFor();
  await row.locator('.tx-expand-btn').click();
  await page.locator('section[aria-label="Transactions"]').evaluate((el) => el.scrollIntoView({ block: 'start' }));
  await page.waitForTimeout(500);
  await shot('transactions.png');

  // 5. AI analysis — latest run (live model) with the history alongside
  const ai = page.locator('section[aria-label="AI risk analysis"]');
  await ai.evaluate((el) => el.scrollIntoView({ block: 'start' }));
  await page.waitForSelector('.analysis-banner-title');
  await page.waitForTimeout(500);
  await shot('analysis-claude.png');

  // 6. AI analysis — an older stub run selected from the history
  const stubRow = ai.locator('.history-row', { hasText: 'stub-analyst' }).first();
  if (await stubRow.count()) {
    await stubRow.click();
    await page.waitForTimeout(800);
    await ai.evaluate((el) => el.scrollIntoView({ block: 'start' }));
    await page.waitForTimeout(300);
    await shot('analysis-stub.png');
  }

  await browser.close();
  console.log('screenshots written to ' + OUT);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

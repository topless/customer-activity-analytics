# Demo recording

Produces the captioned walkthrough video of the running application with Playwright:
a scripted operator session (login, search, dashboard, transactions, AI analyses,
second operator, history) with an injected caption bar and a visible cursor,
book-ended by rendered slides (architecture, AI pipeline, methodology).

```bash
docker compose up --build -d      # from the repo root; fresh data recommended: `docker compose down -v` first
cd scripts/demo-recording
npm install && npx playwright install chromium
npm run demo                      # -> out/demo.mp4, out/demo.srt, out/contact-sheet.png
```

`APP_URL` overrides the target (default `http://localhost:3000`); `SPEED=4` gives a fast
dry run. The recording runs headless and takes about 12 minutes at normal speed.

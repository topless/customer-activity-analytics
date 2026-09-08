# Demo recording

Produces the walkthrough video of the running application with Playwright: a scripted
operator session (login, search, dashboard, transactions, AI analyses, second operator,
history) with an injected caption bar and a visible cursor, book-ended by rendered slides
(architecture, AI pipeline, methodology). Optionally narrated with a local text-to-speech
model.

```bash
docker compose up --build -d      # from the repo root; fresh data recommended: `docker compose down -v` first
cd scripts/demo-recording
npm install && npx playwright install chromium
npm run demo                      # captioned only -> out/demo.mp4, out/demo.srt, out/contact-sheet.png
```

## Narrated version

Narration uses [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (Apache-2.0),
which runs locally on CPU — no account or API key. One-time setup:

```bash
python3.12 -m venv .venv-tts && .venv-tts/bin/pip install kokoro soundfile
# the bundled espeak-ng loader looks for its data one directory too high; expose it:
L=.venv-tts/lib/python3.12/site-packages/espeakng_loader; for f in "$L"/espeak-ng-data/*; do ln -sfn "$f" "$L/$(basename "$f")"; done
```

Then `VOICE=af_heart npm run demo:narrated` (voices: `af_heart`, `af_bella`, `bf_emma`,
`am_michael`, `bm_george`, ...). Pipeline: `lines.js` collects every caption plus the slide
narrations from `narration.js` (with pronunciation-friendly rewrites of technical terms);
`tts.py` synthesizes one clip per line and records their durations; `record.js` with
`NARRATE=1` holds each caption and slide at least as long as its clip and logs the
timeline; `mix.py` lays the clips onto that timeline and muxes them into
`out/demo-narrated.mp4`. The recorder's clock and the video clock were measured to agree
within 30 ms, so no offset is applied.

`APP_URL` overrides the target (default `http://localhost:3000`); `SPEED=4` gives a fast
dry run of the captioned version. A narrated recording takes about 15 minutes end to end.

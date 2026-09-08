// Builds out/narration/lines.json — every on-screen caption in record.js plus the slide
// narrations — as the input for tts.py. Captions are keyed by their exact text so the
// recorder can look up clip durations at run time.
//
//   node lines.js

const fs = require('fs');
const path = require('path');
const { SLIDES, DYNAMIC, spoken } = require('./narration');

const OUT = path.join(__dirname, 'out', 'narration');
fs.mkdirSync(OUT, { recursive: true });

const source = fs.readFileSync(path.join(__dirname, 'record.js'), 'utf8');
const captions = [...source.matchAll(/caption\(\s*'([^']+)'/g)].map((m) => m[1]);

const lines = [];
const seen = new Set();
captions.forEach((text) => {
  if (seen.has(text)) return;
  seen.add(text);
  const id = `c${String(lines.length + 1).padStart(2, '0')}`;
  lines.push({ id, caption: text, text: spoken(text) });
});
for (const [name, text] of Object.entries(SLIDES)) {
  lines.push({ id: `slide-${name}`, slide: name, text });
}
for (const [id, text] of Object.entries(DYNAMIC)) {
  lines.push({ id, dynamic: true, text });
}

fs.writeFileSync(path.join(OUT, 'lines.json'), JSON.stringify(lines, null, 1));
const chars = lines.reduce((n, l) => n + l.text.length, 0);
console.log(`${lines.length} lines (${captions.length} captions, ${Object.keys(SLIDES).length} slides), ${chars} characters -> ${path.join(OUT, 'lines.json')}`);

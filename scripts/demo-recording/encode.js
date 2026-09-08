// Converts out/demo.webm (Playwright, VP8) to an H.264 MP4 that plays everywhere,
// and writes a contact sheet of frames for a quick visual check.
//
//   node encode.js [output.mp4]

const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const ffmpeg = require('ffmpeg-static');

const OUT = path.join(__dirname, 'out');
const input = path.join(OUT, 'demo.webm');
const output = process.argv[2] || path.join(OUT, 'demo.mp4');

if (!fs.existsSync(input)) {
  console.error(`missing ${input} — run record.js first`);
  process.exit(1);
}

// end 2.5 s after the last timeline entry: Playwright keeps recording the frozen final frame
// until the context closes, which would otherwise leave a long static tail
const timelinePath = path.join(OUT, 'timeline.json');
const cut = fs.existsSync(timelinePath)
  ? JSON.parse(fs.readFileSync(timelinePath, 'utf8')).reduce((m, e) => Math.max(m, e.end || e.start || 0), 0) + 2.5
  : null;
// TRIM_START=<seconds> drops leading frames recorded before the timeline clock started
const trimStart = Number(process.env.TRIM_START || 0);
execFileSync(ffmpeg, [
  '-y', ...(trimStart > 0 ? ['-ss', trimStart.toFixed(2)] : []), '-i', input,
  ...(cut ? ['-t', cut.toFixed(2)] : []),
  '-c:v', 'libx264', '-preset', 'slow', '-crf', '21', '-pix_fmt', 'yuv420p',
  '-r', '25', '-movflags', '+faststart', '-an',
  output,
], { stdio: 'inherit' });

// one frame every 20 s, tiled 5 per row — for eyeballing captions and layout
const sheet = path.join(OUT, 'contact-sheet.png');
execFileSync(ffmpeg, [
  '-y', '-i', output,
  '-vf', 'fps=1/20,scale=480:-1,tile=5x8',
  '-frames:v', '1', sheet,
], { stdio: 'ignore' });

const size = (fs.statSync(output).size / 1024 / 1024).toFixed(1);
console.log(`wrote ${output} (${size} MB) and ${sheet}`);

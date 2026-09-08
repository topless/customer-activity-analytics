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

execFileSync(ffmpeg, [
  '-y', '-i', input,
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

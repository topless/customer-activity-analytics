#!/usr/bin/env python
"""Lays the narration clips onto the recorded timeline and muxes them into the video.

Usage:
    mix.py out/timeline.json out/narration out/demo.mp4 out/demo-narrated.mp4 [--offset 0.0]

timeline.json entries carry an `id` (clip) and `start` (seconds on the recorder's clock);
--offset shifts every clip by the measured difference between that clock and the video.
"""

import json
import subprocess
import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 24000
LEAD_IN = 0.25  # seconds between a caption appearing and the voice starting


def main():
    args = sys.argv[1:]
    timeline = json.loads(Path(args[0]).read_text())
    clips_dir = Path(args[1])
    video_in, video_out = args[2], args[3]
    offset = float(args[args.index("--offset") + 1]) if "--offset" in args else 0.0

    duration = probe_duration(video_in)
    track = np.zeros(int((duration + 1.0) * SAMPLE_RATE), dtype=np.float32)

    placed = 0
    last_end = 0.0
    for entry in timeline:
        clip_id = entry.get("id")
        if not clip_id:
            continue
        wav_path = clips_dir / f"{clip_id}.wav"
        if not wav_path.exists():
            print(f"missing clip {clip_id}", file=sys.stderr)
            continue
        wav, rate = sf.read(wav_path, dtype="float32")
        assert rate == SAMPLE_RATE, f"{wav_path}: {rate} Hz"
        start = max(entry["start"] + offset + LEAD_IN, last_end + 0.15)
        i0 = int(start * SAMPLE_RATE)
        i1 = min(i0 + len(wav), len(track))
        track[i0:i1] += wav[: i1 - i0]
        last_end = i1 / SAMPLE_RATE
        placed += 1

    np.clip(track, -1.0, 1.0, out=track)
    narration_wav = Path(video_out).with_suffix(".narration.wav")
    sf.write(narration_wav, track, SAMPLE_RATE)

    ffmpeg = subprocess.check_output(
        ["node", "-e", "console.log(require('ffmpeg-static'))"], cwd=Path(__file__).parent, text=True
    ).strip()
    subprocess.check_call([
        ffmpeg, "-y", "-i", video_in, "-i", str(narration_wav),
        "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac", "-b:a", "128k",
        "-movflags", "+faststart", video_out,
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    write_subtitles(timeline, clips_dir, Path(video_out).with_suffix(".srt"), offset)
    print(f"placed {placed} clips, last voice ends at {last_end:.1f}s of {duration:.1f}s -> {video_out}")


def write_subtitles(timeline, clips_dir, srt_path, offset):
    """Captions plus the spoken slide narration, so the subtitle track covers the whole video."""
    lines = {l["id"]: l for l in json.loads((clips_dir / "lines.json").read_text())}
    cues = []
    for entry in timeline:
        text = entry["text"]
        if entry.get("slide"):
            line = lines.get(entry.get("id"))
            if not line:
                continue
            text = line["text"]
        start = entry["start"] + offset + (LEAD_IN if entry.get("id") else 0)
        end = (entry["end"] if entry.get("end") is not None else start + 4) + offset
        cues.append((start, max(end, start + 1.0), text))
    def ts(sec):
        sec = max(0.0, sec); h = int(sec // 3600); mi = int(sec % 3600 // 60); s = sec % 60
        return f"{h:02d}:{mi:02d}:{int(s):02d},{int(round((s - int(s)) * 1000)):03d}"
    srt_path.write_text("".join(f"{i}\n{ts(a)} --> {ts(b)}\n{txt}\n\n" for i, (a, b, txt) in enumerate(cues, 1)))


def probe_duration(video):
    ffmpeg = subprocess.check_output(
        ["node", "-e", "console.log(require('ffmpeg-static'))"], cwd=Path(__file__).parent, text=True
    ).strip()
    result = subprocess.run([ffmpeg, "-i", video], capture_output=True, text=True)
    for line in result.stderr.splitlines():
        if "Duration:" in line:
            h, m, s = line.split("Duration:")[1].split(",")[0].strip().split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    raise SystemExit(f"could not read duration of {video}")


if __name__ == "__main__":
    main()

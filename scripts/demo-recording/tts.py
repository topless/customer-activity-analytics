#!/usr/bin/env python
"""Synthesises narration clips with Kokoro-82M (local, Apache-2.0).

Usage:
    tts.py lines.json out_dir [--voice af_heart] [--speed 1.0]

lines.json: [{"id": "...", "text": "..."}, ...]
Writes out_dir/<id>.wav (24 kHz mono) and out_dir/durations.json {id: seconds}.
"""

import json
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from kokoro import KPipeline

SAMPLE_RATE = 24000
GAP = np.zeros(int(0.18 * SAMPLE_RATE), dtype=np.float32)  # between sentences/segments


def main():
    args = sys.argv[1:]
    lines_path, out_dir = Path(args[0]), Path(args[1])
    voice = args[args.index("--voice") + 1] if "--voice" in args else "af_heart"
    speed = float(args[args.index("--speed") + 1]) if "--speed" in args else 1.0
    lang = "b" if voice.startswith("b") else "a"  # British vs American English voices
    out_dir.mkdir(parents=True, exist_ok=True)

    pipeline = KPipeline(lang_code=lang, repo_id="hexgrad/Kokoro-82M")
    lines = json.loads(lines_path.read_text())
    durations = {}
    for line in lines:
        chunks = []
        for _, _, audio in pipeline(line["text"], voice=voice, speed=speed):
            chunks.append(np.asarray(audio, dtype=np.float32))
            chunks.append(GAP)
        wav = np.concatenate(chunks) if chunks else np.zeros(SAMPLE_RATE // 4, dtype=np.float32)
        # trim the trailing gap, keep a short natural tail
        wav = wav[: max(len(wav) - len(GAP), 0)]
        peak = float(np.max(np.abs(wav))) if len(wav) else 0.0
        if peak > 0:
            wav = wav * (0.89 / peak)
        sf.write(out_dir / f"{line['id']}.wav", wav, SAMPLE_RATE)
        durations[line["id"]] = round(len(wav) / SAMPLE_RATE, 3)
        print(f"{line['id']:>32}  {durations[line['id']]:6.2f}s", flush=True)

    (out_dir / "durations.json").write_text(json.dumps(durations, indent=1))
    total = sum(durations.values())
    print(f"{len(lines)} clips, {total/60:.1f} min of speech, voice={voice}")


if __name__ == "__main__":
    main()

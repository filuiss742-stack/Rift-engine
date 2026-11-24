#!/usr/bin/env python3
"""Create original, deterministic WAV score sketches for Rift Engine worlds.

Uses only Python's standard library. Output is mono PCM WAV so both Android
MediaPlayer and the desktop Java Sound backend can decode it without codecs.
"""
from __future__ import annotations

import argparse
import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
DURATION = 38.0
TAU = math.tau


def add_tone(buffer: list[float], start: float, duration: float, frequency: float,
             amplitude: float, waveform: str = "sine", attack: float = 0.08,
             release: float = 0.35, vibrato: float = 0.0, seed: int = 0) -> None:
    first = max(0, int(start * SAMPLE_RATE))
    count = min(len(buffer) - first, int(duration * SAMPLE_RATE))
    if count <= 0:
        return
    phase = 0.0
    rng = random.Random(seed)
    noise_state = 0.0
    for offset in range(count):
        t = offset / SAMPLE_RATE
        remaining = duration - t
        env = min(1.0, t / max(attack, 0.001), remaining / max(release, 0.001))
        env = max(0.0, env)
        wobble = 1.0 + vibrato * math.sin(TAU * 4.6 * t)
        phase += TAU * frequency * wobble / SAMPLE_RATE
        if waveform == "triangle":
            cycle = (phase / TAU) % 1.0
            sample = 1.0 - 4.0 * abs(cycle - 0.5)
        elif waveform == "soft-square":
            sample = math.tanh(2.2 * math.sin(phase))
        elif waveform == "noise":
            noise_state = 0.72 * noise_state + 0.28 * rng.uniform(-1.0, 1.0)
            sample = noise_state
        else:
            sample = math.sin(phase)
        buffer[first + offset] += amplitude * env * sample


def chord(buffer: list[float], start: float, duration: float, notes: tuple[float, ...],
          amplitude: float, waveform: str = "sine", attack: float = 0.8,
          release: float = 1.4, vibrato: float = 0.0, seed: int = 0) -> None:
    for index, frequency in enumerate(notes):
        add_tone(buffer, start, duration, frequency, amplitude, waveform,
                 attack, release, vibrato, seed + index * 17)


def make_earth() -> list[float]:
    out = [0.0] * int(DURATION * SAMPLE_RATE)
    progressions = [
        (220.00, 261.63, 329.63), (196.00, 246.94, 293.66),
        (174.61, 220.00, 261.63), (196.00, 246.94, 293.66),
    ]
    for bar, notes in enumerate(progressions * 3):
        start = bar * 4.0
        chord(out, start, 4.5, notes, 0.075, "sine", 1.0, 1.6)
        add_tone(out, start, 3.6, notes[0] / 2, 0.10, "triangle", 0.2, 1.0)
        for beat in range(4):
            note = notes[(beat + 1) % 3] * (2 if beat in (1, 3) else 1)
            add_tone(out, start + beat * 0.92, 0.52, note, 0.045,
                     "sine", 0.015, 0.42, seed=bar * 10 + beat)
    melody = [523.25, 493.88, 440.00, 392.00, 440.00, 493.88, 587.33, 523.25,
              493.88, 440.00, 392.00, 349.23, 392.00, 440.00, 493.88, 440.00]
    for i, note in enumerate(melody):
        add_tone(out, 1.2 + i * 2.1, 1.25, note, 0.045, "sine", 0.025, 0.8,
                 vibrato=0.002, seed=i)
    return out


def make_mira() -> list[float]:
    out = [0.0] * int(DURATION * SAMPLE_RATE)
    chords = [
        (146.83, 174.61, 220.00), (130.81, 164.81, 196.00),
        (110.00, 146.83, 174.61), (130.81, 164.81, 196.00),
    ]
    for bar, notes in enumerate(chords * 3):
        start = bar * 4.0
        chord(out, start, 5.0, notes, 0.095, "triangle", 1.5, 2.0, 0.003)
        add_tone(out, start, 4.0, notes[0] / 2, 0.09, "sine", 1.2, 1.4)
        if bar % 2 == 0:
            add_tone(out, start + 2.0, 1.1, 880.0, 0.018, "sine", 0.2, 0.8, 0.004)
    flute = [440.00, 523.25, 493.88, 392.00, 349.23, 392.00, 440.00, 0,
             392.00, 440.00, 523.25, 587.33, 523.25, 440.00, 392.00, 0]
    for i, note in enumerate(flute):
        if note:
            add_tone(out, 0.8 + i * 2.15, 1.65, note, 0.055, "sine",
                     0.12, 1.1, 0.012, i + 90)
    for hit in range(9):
        add_tone(out, 0.4 + hit * 4.0, 0.22, 62.0, 0.075, "noise",
                 0.002, 0.20, seed=hit + 200)
    return out


def make_thea() -> list[float]:
    out = [0.0] * int(DURATION * SAMPLE_RATE)
    chord(out, 0.0, DURATION, (55.0, 58.27, 82.41), 0.10,
          "soft-square", 2.0, 3.0, 0.008, 310)
    chord(out, 0.0, DURATION, (110.0, 116.54), 0.035,
          "triangle", 3.0, 3.0, 0.014, 410)
    for pulse in range(25):
        start = pulse * 1.5 + (0.18 if pulse % 3 == 1 else 0.0)
        freq = (41.2, 43.65, 55.0, 61.74)[pulse % 4]
        add_tone(out, start, 0.72, freq, 0.13, "soft-square",
                 0.015, 0.55, 0.025, pulse + 500)
        if pulse % 3 == 0:
            add_tone(out, start + 0.28, 0.38, 880.0 + 13 * pulse, 0.018,
                     "noise", 0.005, 0.32, seed=pulse + 600)
    return out


def make_warfront() -> list[float]:
    """Retro-industrial RTS march for the fourth world; original composition."""
    out = [0.0] * int(DURATION * SAMPLE_RATE)
    bpm = 96.0
    beat = 60.0 / bpm
    bar = beat * 4.0
    bass_notes = (55.0, 65.41, 73.42, 61.74)
    chords = (
        (146.83, 174.61, 220.00),
        (130.81, 164.81, 196.00),
        (110.00, 146.83, 174.61),
        (123.47, 155.56, 185.00),
    )
    measure = 0
    start = 0.0
    while start < DURATION:
        root = bass_notes[measure % len(bass_notes)]
        add_tone(out, start, bar * 0.92, root, 0.17, "soft-square",
                 0.012, 0.32, 0.003, 700 + measure)
        add_tone(out, start + beat * 2.0, beat * 0.78, root * 1.5, 0.065,
                 "triangle", 0.008, 0.24, seed=720 + measure)
        chord(out, start, bar * 0.9, chords[measure % len(chords)], 0.035,
              "soft-square", 0.12, 0.72, 0.001, 740 + measure)
        for step in range(4):
            hit = start + step * beat
            add_tone(out, hit, 0.16, 58.0 if step in (0, 2) else 46.0,
                     0.20 if step == 0 else 0.12, "noise", 0.002, 0.14,
                     seed=800 + measure * 8 + step)
            if step in (1, 3):
                add_tone(out, hit, 0.19, 190.0, 0.075, "noise", 0.002,
                         0.17, seed=900 + measure * 8 + step)
                add_tone(out, hit, 0.12, 132.0, 0.045, "triangle", 0.002,
                         0.11, seed=920 + measure * 8 + step)
            add_tone(out, hit + beat * 0.5, 0.045, 2400.0, 0.018,
                     "noise", 0.001, 0.04, seed=940 + measure * 8 + step)
        measure += 1
        start += bar

    motif = (293.66, 261.63, 220.00, 196.00, 220.00, 261.63, 246.94, 220.00)
    for index, note in enumerate(motif):
        time = 1.0 + index * beat * 2.0
        add_tone(out, time, beat * 1.35, note, 0.052, "triangle",
                 0.025, 0.38, 0.002, 1000 + index)
        add_tone(out, time, beat * 1.1, note * 0.5, 0.028, "sine",
                 0.02, 0.32, seed=1020 + index)

    warning = 0
    time = bar * 3.0
    while time < DURATION:
        add_tone(out, time, 0.72, 392.0 if warning % 2 == 0 else 349.23,
                 0.022, "sine", 0.08, 0.58, 0.004, 1100 + warning)
        warning += 1
        time += bar * 4.0
    return out


def write_wav(path: Path, samples: list[float]) -> None:
    peak = max((abs(value) for value in samples), default=0.0)
    gain = 0.82 / max(peak, 0.82)
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for value in samples:
            # Soft limiting keeps overlapping layers from clipping.
            limited = math.tanh(value * gain * 1.35) / math.tanh(1.35)
            frames.extend(struct.pack("<h", int(max(-1.0, min(1.0, limited)) * 32767)))
        output.writeframes(frames)


def generate(output_dir: Path, duration: float = DURATION) -> list[Path]:
    global DURATION
    if duration <= 0 or duration > 180:
        raise ValueError("duration must be between 0 and 180 seconds")
    DURATION = duration
    output_dir.mkdir(parents=True, exist_ok=True)
    tracks = [
        ("01_earth_everyday.wav", make_earth),
        ("02_mira_ruined_fantasy.wav", make_mira),
        ("03_thea_distortion.wav", make_thea),
        ("04_warfront_rts.wav", make_warfront),
    ]
    result = []
    for filename, render in tracks:
        path = output_dir / filename
        write_wav(path, render())
        result.append(path)
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--duration", type=float, default=DURATION)
    args = parser.parse_args()
    for path in generate(args.output, args.duration):
        print(f"Generated {path} ({path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()

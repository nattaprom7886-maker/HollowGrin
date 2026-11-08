#!/usr/bin/env python3
"""Synthesises all sounds of Mr. Hollowgrin (mono, 44.1 kHz -> .ogg via ffmpeg).
The voice lines (gotcha, laugh) use a formant synthesiser. They are intentionally easy to replace:
drop your own recording with the SAME file name into assets/hollowgrin/sounds/ (mono .ogg)."""
import os, subprocess, wave
import numpy as np
from scipy.signal import lfilter, butter, resample

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "hollowgrin", "sounds")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(666)

def t_(sec): return np.arange(int(sec * SR)) / SR
def bp(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band"); return lfilter(b, a, x)
def lp(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low"); return lfilter(b, a, x)
def hp(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="high"); return lfilter(b, a, x)
def env_exp(n, tau): return np.exp(-np.arange(n) / (tau * SR))
def resonator(x, f, bw):
    r = np.exp(-np.pi * bw / SR); th = 2 * np.pi * f / SR
    return lfilter([1 - r], [1, -2 * r * np.cos(th), r * r], x)

def reverb(x, sec=1.4, mix=0.28):
    n = int(sec * SR); ir = rng.normal(size=n) * np.exp(-np.arange(n) / (0.35 * SR))
    ir = lp(ir, 3500); ir /= np.abs(ir).max()
    wet = np.convolve(x, ir)[: len(x) + n // 2] * 0.12
    out = np.zeros(len(wet)); out[: len(x)] += x * (1 - mix); out += wet * mix * 3
    return out

def glottal(f0_curve, breath=0.08, rough=0.0):
    """Source: band-limited sawtooth-ish pulse train with jitter."""
    n = len(f0_curve)
    jitter = 1 + rough * lp(rng.normal(size=n), 60) * 4
    phase = np.cumsum(f0_curve * jitter / SR)
    saw = 2 * (phase % 1.0) - 1
    src = saw - np.roll(saw, 1)                      # derivative-ish -> buzzier
    src += np.sin(2 * np.pi * phase * 0.5) * 0.4     # sub-octave growl
    src += breath * rng.normal(size=n)
    return src

def vowel(f0_curve, formants, amp_env, breath=0.08, rough=0.15):
    """formants: list of (freq_array_or_scalar, bandwidth, gain)"""
    src = glottal(f0_curve, breath, rough)
    out = np.zeros(len(src))
    for f, bw, g in formants:
        if np.isscalar(f):
            out += g * resonator(src, f, bw)
        else:  # time-varying formant: crossfade through blocks
            blocks = 24; edges = np.linspace(0, len(src), blocks + 1).astype(int)
            for i in range(blocks):
                seg = resonator(src, float(f[edges[i]]), bw)
                w = np.zeros(len(src)); w[edges[i]:edges[i + 1]] = 1
                w = lp(w, 80, 1)
                out += g * seg * w
    return out * amp_env

def normalize(x, peak=0.89):
    x = np.asarray(x, dtype=np.float64); m = np.abs(x).max()
    return x / m * peak if m > 0 else x

def fade(x, a=0.004, b=0.04):
    n = len(x); fa = int(a * SR); fb = int(b * SR)
    x = x.copy(); x[:fa] *= np.linspace(0, 1, fa); x[-fb:] *= np.linspace(1, 0, fb); return x

def save(name, x):
    x = fade(normalize(x))
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())
    ogg = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                    "-ac", "1", ogg], check=True)
    os.remove(wav)
    print("wrote", name + ".ogg", round(len(x) / SR, 2), "s")

def pitchshift(x, semis):
    r = 2 ** (semis / 12.0); y = resample(x, int(len(x) / r)); return y

def mix(*arrs):
    n = max(len(a) for a in arrs); out = np.zeros(n)
    for a in arrs: out[: len(a)] += a
    return out

# ============== GOTCHA! ==============
def make_gotcha():
    # syllable 1 "Got" : g-o-t   syllable 2 "cha!" : tʃ-a
    sil = np.zeros(int(0.03 * SR))
    # g burst + voice bar
    g_dur = 0.07
    g = lp(rng.normal(size=int(g_dur * SR)), 900) * env_exp(int(g_dur * SR), 0.03) * 0.8
    g += 0.6 * np.sin(2 * np.pi * 85 * t_(g_dur)) * np.linspace(0.2, 1, int(g_dur * SR))
    # "o" as in "got" -> open back vowel
    o_dur = 0.20; n = int(o_dur * SR)
    f0 = np.linspace(105, 92, n)
    o = vowel(f0, [(np.linspace(620, 720, n), 90, 1.0), (np.linspace(1000, 1100, n), 110, 0.7), (2550, 160, 0.3)],
              np.hanning(n) ** 0.6, breath=0.05, rough=0.25)
    # t closure then burst
    clos = np.zeros(int(0.07 * SR))
    t_b = hp(rng.normal(size=int(0.02 * SR)), 3500) * env_exp(int(0.02 * SR), 0.008) * 0.9
    # "ch" affricate
    ch_dur = 0.11; nc = int(ch_dur * SR)
    ch = bp(rng.normal(size=nc), 2500, 7000) * (np.hanning(nc) ** 0.5) * 1.1
    # "a" as in "cha" -> rising, then low drawn-out menacing fall
    a_dur = 0.62; na = int(a_dur * SR)
    f0a = np.concatenate([np.linspace(98, 135, int(na * 0.3)), np.linspace(135, 70, na - int(na * 0.3))])
    amp = np.hanning(na) ** 0.45 * np.concatenate([np.ones(int(na * 0.55)), np.linspace(1, 0.55, na - int(na * 0.55))])
    a = vowel(f0a, [(np.linspace(760, 820, na), 100, 1.0), (np.linspace(1200, 1300, na), 120, 0.8), (2700, 170, 0.35)],
              amp, breath=0.10, rough=0.35)
    # breathy exhale tail
    tail = bp(rng.normal(size=int(0.35 * SR)), 600, 2500) * env_exp(int(0.35 * SR), 0.12) * 0.25
    voice = np.concatenate([g, o, clos, t_b, sil, ch, a, tail])
    voice = np.tanh(voice * 2.2)
    deep = pitchshift(voice, -5)
    voice = mix(voice * 0.75, deep * 0.55)
    voice = voice + 0.4 * np.roll(voice, int(0.011 * SR))
    voice = hp(voice, 60)
    save("gotcha", reverb(voice, 1.2, 0.22))

# ============== LAUGH ==============
def make_laugh():
    parts = []
    base = 150
    for i in range(6):
        pitch = base * (0.93 ** i)
        h_n = int(0.07 * SR)
        h = bp(rng.normal(size=h_n), 800, 5000) * np.hanning(h_n) * 0.5
        a_n = int((0.15 + 0.01 * i) * SR)
        f0 = np.linspace(pitch * 1.1, pitch * 0.8, a_n)
        a = vowel(f0, [(800, 100, 1.0), (1250, 120, 0.7), (2650, 180, 0.3)], np.hanning(a_n) ** 0.7,
                  breath=0.15, rough=0.4)
        gap = np.zeros(int((0.08 + 0.015 * i) * SR))
        parts += [h, a, gap]
    x = np.concatenate(parts)
    x = np.tanh(x * 1.8)
    x = mix(x * 0.7, pitchshift(x, -7) * 0.6)
    save("laugh", reverb(x, 1.6, 0.3))

# ============== MUSIC BOX HUM (the 'tell') ==============
def make_hum():
    notes = [440.0, 523.25, 659.25, 587.33, 523.25, 493.88, 440.0, 415.30,
             493.88, 659.25, 622.25, 587.33, 523.25, 493.88, 440.0, 392.0]
    step = 0.42; total = step * len(notes) + 2.2
    out = np.zeros(int(total * SR))
    for i, f in enumerate(notes):
        f *= 2 ** (rng.uniform(-14, 14) / 1200.0)    # slightly out of tune
        dur = 1.6; n = int(dur * SR); tt = t_(dur)
        tone = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 9)
                + 0.25 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt * 14)) * np.exp(-tt * 2.6)
        s = int(i * step * SR); out[s:s + n] += tone[: len(out) - s] * (0.9 if i % 4 else 1.15)
    # tape wow / flutter and a dying last note
    tt = t_(total); wow = 1 + 0.012 * np.sin(2 * np.pi * 0.7 * tt) + 0.004 * np.sin(2 * np.pi * 5.1 * tt)
    idx = np.cumsum(wow); idx = idx / idx[-1] * (len(out) - 1)
    out = np.interp(idx, np.arange(len(out)), out)
    out = lp(out, 4200) + 0.015 * lp(rng.normal(size=len(out)), 800)
    # a faint low human hum underneath
    hn = len(out); f0 = 110 + 4 * np.sin(2 * np.pi * 0.35 * t_(total))[:hn]
    hum = vowel(f0, [(320, 80, 1.0), (900, 100, 0.4)], 0.5 + 0.5 * np.sin(2 * np.pi * 0.18 * t_(total))[:hn] ** 2,
                breath=0.02, rough=0.1)
    out = out / np.abs(out).max() + 0.35 * hum / (np.abs(hum).max() + 1e-9)
    save("hum", reverb(out, 1.8, 0.35))

# ============== HEARTBEAT ==============
def make_heartbeat():
    out = np.zeros(int(1.0 * SR))
    for start, amp in ((0.0, 1.0), (0.21, 0.75)):
        n = int(0.22 * SR); tt = t_(0.22)
        beat = np.sin(2 * np.pi * (58 - 25 * tt / 0.22) * tt) * np.exp(-tt * 22) * amp
        beat += lp(rng.normal(size=n), 220) * np.exp(-tt * 40) * 0.5 * amp
        s = int(start * SR); out[s:s + n] += beat
    save("heartbeat", out)

# ============== REVEAL (flesh/bone cracking open) ==============
def make_reveal():
    dur = 1.9; n = int(dur * SR); out = np.zeros(n)
    tear = bp(rng.normal(size=n), 150, 1800) * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t_(dur)) ** 2) * np.hanning(n) ** 0.5
    out += tear * 0.55
    for i in range(34):
        pos = (i / 34.0) ** 1.5 * (dur - 0.3)
        s = int(pos * SR); k = int(0.012 * SR)
        click = bp(rng.normal(size=k), 900, 4500) * env_exp(k, 0.003) * rng.uniform(0.6, 1.4)
        out[s:s + k] += click[: n - s] * 1.5
    out += np.sin(2 * np.pi * 48 * t_(dur)) * env_exp(n, 0.5) * 0.8
    save("reveal", reverb(out, 1.0, 0.2))

# ============== JAW (wet creaking mouth opening) ==============
def make_jaw():
    dur = 1.7; n = int(dur * SR); tt = t_(dur)
    noise = rng.normal(size=n)
    center = 180 + 700 * (tt / dur) ** 1.3
    sweep = np.zeros(n); blocks = 40; edges = np.linspace(0, n, blocks + 1).astype(int)
    for i in range(blocks):
        seg = resonator(noise, float(center[edges[i]]), 70)
        w = np.zeros(n); w[edges[i]:edges[i + 1]] = 1; w = lp(w, 50, 1); sweep += seg * w
    sweep *= (0.6 + 0.4 * np.sin(2 * np.pi * 17 * tt) ** 2) * np.hanning(n) ** 0.7
    out = sweep * 2.2
    for i in range(14):
        s = int(rng.uniform(0.1, dur - 0.1) * SR); k = int(0.02 * SR)
        pop = lp(rng.normal(size=k), 1800) * env_exp(k, 0.006) * rng.uniform(0.5, 1.2)
        out[s:s + k] += pop[: n - s] * 1.6
    out += np.sin(2 * np.pi * 42 * tt) * np.hanning(n) * 0.35
    save("jaw", reverb(out, 1.2, 0.25))

# ============== SQUEEZE (bones creaking) ==============
def make_squeeze():
    dur = 2.3; n = int(dur * SR); tt = t_(dur); out = np.zeros(n)
    f0 = 62 + 30 * (tt / dur) + 6 * lp(rng.normal(size=n), 8) * 8
    groan = vowel(np.clip(f0, 40, 140), [(300, 90, 1.0), (700, 130, 0.6), (1700, 200, 0.2)],
                  (0.4 + 0.6 * (tt / dur)) * np.hanning(n) ** 0.6, breath=0.12, rough=0.6)
    out += groan * 0.9
    for i in range(46):
        pos = (i / 46.0) ** 0.8 * (dur - 0.2)
        s = int(pos * SR); k = int(rng.uniform(0.008, 0.02) * SR)
        crack = bp(rng.normal(size=k), 700, 5000) * env_exp(k, 0.003) * rng.uniform(0.5, 1.5)
        out[s:s + k] += crack[: n - s] * (0.5 + 1.8 * pos / dur)
    out += bp(rng.normal(size=n), 2000, 6000) * 0.05 * (tt / dur)
    save("squeeze", reverb(out, 0.9, 0.18))

if __name__ == "__main__":
    make_gotcha(); make_laugh(); make_hum(); make_heartbeat(); make_reveal(); make_jaw(); make_squeeze()

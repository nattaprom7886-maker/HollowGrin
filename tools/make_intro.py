#!/usr/bin/env python3
"""Synthesises the sounds of the Hollowgrin world-entry intro:
  intro_voice.ogg  - Thai speech from a hand-written formant synthesiser (robotic on purpose)
  intro_music.ogg  - slow, slightly out-of-tune music box that winds down at the end
  intro_laugh.ogg  - the long laugh that is left after the eyes vanish
Run:  python3 tools/make_intro.py     (needs numpy, scipy, ffmpeg)
Prints the timing marks that go into IntroTimeline.java.
Replace any of the three .ogg files with your own recording (same name, mono .ogg) if you want real voice acting;
if the length changes, update the numbers in IntroTimeline.java."""
import json, sys
import numpy as np
import make_sounds as ms

SR = ms.SR
rng = ms.rng

# ------------------------------------------------------------------ Thai formant voice
V = {'a': (730, 1090, 2440), 'i': (290, 2250, 2980), 'u': (320, 850, 2250), 'e': (450, 2100, 2700),
     'E': (650, 1750, 2450), 'o': (450, 800, 2350), 'O': (590, 860, 2450), 'M': (320, 1350, 2300),
     'Q': (500, 1300, 2400)}
BASE = 100.0
TONE = {'m': (1.00, 1.00), 'l': (0.93, 0.84), 'f': (1.18, 0.84), 'h': (1.00, 1.22), 'r': (0.88, 1.22)}
SLOW = 1.45          # global slow-down: creepy, patient delivery


def vowel_seg(vs, dur, f0c):
    """vs: list of vowel keys (1 = monophthong, 2-3 = glide through them), f0c: pitch curve array."""
    n = len(f0c)
    keys = np.linspace(0, n - 1, len(vs))
    tracks = []
    for k in range(3):
        pts = [V[v][k] for v in vs]
        tracks.append(np.interp(np.arange(n), keys, pts) if len(vs) > 1 else float(pts[0]))
    forms = [(tracks[k], ms.BW[k][0], ms.BW[k][1]) for k in range(3)]
    return ms.vowel(f0c, forms, ms._env(n, 0.02, 0.035), breath=0.05, rough=0.3)


def asp(dur=0.05, amp=0.25):
    return ms._noise(dur, 800, 4500, amp)


def cons(c, f0):
    if c == 's': return ms._noise(0.11, 4200, 9500, 0.55)
    if c == 'h': return ms.ph_segment('HH', f0, f0)
    if c == 'q': return ms._sil(0.03)
    if c == 'k': return ms.ph_segment('K', f0, f0)
    if c == 'kh': return np.concatenate([ms.ph_segment('K', f0, f0), asp()])
    if c == 't': return ms.ph_segment('T', f0, f0)
    if c == 'th': return np.concatenate([ms.ph_segment('T', f0, f0), asp()])
    if c == 'd': return ms.ph_segment('D', f0, f0)
    if c in ('c', 'ch'): return ms.ph_segment('CH', f0, f0)
    if c in ('p', 'ph'):
        burst = ms.lp(rng.normal(size=int(0.02 * SR)), 1400) * ms.env_exp(int(0.02 * SR), 0.008) * 0.9
        out = np.concatenate([ms._sil(0.05), burst, ms._noise(0.02, 300, 2000, 0.15)])
        return np.concatenate([out, asp()]) if c == 'ph' else out
    if c == 'm': return ms.ph_segment('M', f0, f0)
    if c == 'n': return ms.ph_segment('N', f0, f0)
    if c == 'ng': return ms.ph_segment('NG', f0, f0)
    if c == 'l': return ms.ph_segment('L', f0, f0)
    if c == 'r': return ms._voiced(ms.GLI['L'], ms.GLI['L'], 0.05, f0, f0, 0.8)   # quick tap
    if c == 'y': return ms.ph_segment('Y', f0, f0)
    if c == 'w': return ms.ph_segment('W', f0, f0)
    if c == '': return np.zeros(0)
    raise ValueError(c)


def final(c, f0):
    if c == '': return np.zeros(0)
    if c in ('n', 'm', 'ng'):
        return ms._voiced(ms.NAS[c.upper()], ms.NAS[c.upper()], 0.14, f0, f0 * 0.96, 0.7)
    if c in ('k', 't', 'p'):   # unreleased stop: short silence + tiny click
        return np.concatenate([ms._sil(0.07), ms._noise(0.008, 1500, 5000, 0.18)])
    raise ValueError(c)


def syl(init, vs, fin, tone, long_=True, stretch=1.0):
    a, b = TONE[tone]
    f0a, f0b = BASE * a, BASE * b
    dur = (0.30 if long_ else 0.16) * SLOW * stretch
    n = int(dur * SR)
    f0c = np.linspace(f0a, f0b, n)
    parts = [cons(init, f0a), vowel_seg(vs, dur, f0c), final(fin, f0b)]
    return np.concatenate([p for p in parts if len(p)])


def hum(dur):
    """'uuum' - long drawn-out hum with a slow wobble and a falling end."""
    n = int(dur * SR); tt = np.arange(n) / SR
    f0c = (BASE * 0.98 - 14 * tt / dur) + 5 * np.sin(2 * np.pi * 4.6 * tt) * np.minimum(1, tt * 1.5)
    ee = np.hanning(n) ** 0.55
    forms = [(np.interp(np.arange(n), [0, n * 0.55, n - 1], [V['M'][k], V['M'][k], 250 if k == 0 else (1100 if k == 1 else 2200)]),
              ms.BW[k][0], ms.BW[k][1]) for k in range(3)]
    return ms.vowel(f0c, forms, ee, breath=0.06, rough=0.35)


def hehehe():
    parts = []
    base = 140
    for i in range(3):
        pitch = base * (0.88 ** i)
        h = ms._noise(0.07, 800, 5000, 0.5)
        a_n = int(0.17 * SR)
        a = ms.vowel(np.linspace(pitch * 1.1, pitch * 0.8, a_n),
                     [(530, 100, 1.0), (1840, 120, 0.7), (2480, 180, 0.3)],
                     np.hanning(a_n) ** 0.7, breath=0.15, rough=0.4)
        parts += [h, a, ms._sil(0.10)]
    return np.concatenate(parts)


def word(*sylls):
    out = []
    for i, s in enumerate(sylls):
        out.append(s)
        if i < len(sylls) - 1:
            out.append(ms._sil(0.03))
    return np.concatenate(out)


def gap(sec):
    return ms._sil(sec)


# --- Thai words (initial, vowels, final, tone, long)
def sawatdee(): return word(syl('s', ['a'], '', 'l', False), syl('w', ['a'], 't', 'l', False), syl('d', ['i'], '', 'm'))
def khun(): return syl('kh', ['u'], 'n', 'm', False)
def mai_f(): return syl('m', ['a', 'i'], '', 'f')
def mai_l(): return syl('m', ['a', 'i'], '', 'l')
def khun_h(): return syl('kh', ['u'], 'n', 'h', False)
def naa(): return syl('n', ['a'], '', 'f')
def loei(): return syl('l', ['Q', 'i'], '', 'm')
def ca(): return syl('c', ['a'], '', 'l', False)
def pen(): return syl('p', ['e'], 'n', 'm')
def phuean(): return syl('ph', ['M', 'a'], 'n', 'f')
def si(): return syl('s', ['i'], '', 'l', False)
def na_h(): return syl('n', ['a'], '', 'h', False)
def iik(): return syl('q', ['i'], 'k', 'l')
def naan(): return syl('n', ['a'], 'n', 'm')
def rao(): return syl('r', ['a', 'u'], '', 'm')
def dai(): return syl('d', ['a', 'i'], '', 'f')
def len_(): return syl('l', ['e'], 'n', 'f')
def kem(): return syl('k', ['e'], 'm', 'm')
def duai(): return syl('d', ['u', 'a', 'i'], '', 'f')
def kan(): return syl('k', ['a'], 'n', 'm', False)
def tee(): return syl('t', ['E'], '', 'l')
def yang(): return syl('y', ['a'], 'ng', 'm', False)
def rerm(): return syl('r', ['Q'], 'm', 'f')
def toon(): return syl('t', ['O'], 'n', 'm')
def nii(): return syl('n', ['i'], '', 'h')
def nan_f(): return syl('n', ['a'], 'n', 'f')
def rai(): return syl('r', ['a', 'i'], '', 'm')
def rook(): return syl('r', ['O'], 'k', 'l')
def chan(): return syl('ch', ['a'], 'n', 'r', False)
def ro(): return syl('r', ['O'], '', 'm')
def duu(): return syl('d', ['u'], '', 'm')


def build_voice():
    marks = []
    pieces = []
    t0 = [0.0]

    def add(arr, pause, key):
        marks.append({"key": key, "start": round(t0[0], 3), "end": round(t0[0] + len(arr) / SR, 3)})
        pieces.append(arr); pieces.append(gap(pause))
        t0[0] += len(arr) / SR + pause

    w_gap = lambda: gap(0.16)
    # 1 "สวัสดี"
    add(sawatdee(), 0.55, "intro.hollowgrin.l1")
    # 2 "คุณไม่คุ้นหน้าเลย"
    add(np.concatenate([khun(), w_gap(), mai_f(), khun_h(), naa(), loei()]), 0.85, "intro.hollowgrin.l2")
    # 3 "อืมมมมม"
    add(hum(1.5), 0.6, "intro.hollowgrin.l3")
    # 4 "ดูคุณจะเป็นเพื่อนใหม่สินะ"
    add(np.concatenate([duu(), khun(), w_gap(), ca(), pen(), phuean(), mai_l(), w_gap(), si(), na_h()]), 0.7, "intro.hollowgrin.l4")
    # 5 "อืมมมมม"
    add(hum(1.6), 0.6, "intro.hollowgrin.l5")
    # 6 "hehehe"
    add(hehehe(), 0.95, "intro.hollowgrin.l6")
    # 7 "อีกไม่นาน เราจะได้เล่นเกมด้วยกัน"
    add(np.concatenate([iik(), mai_f(), naan(), w_gap(), rao(), ca(), dai(), len_(), kem(), duai(), kan()]), 0.95, "intro.hollowgrin.l7")
    # 8 "แต่เกมยังไม่เริ่มตอนนี้"
    add(np.concatenate([tee(), kem(), yang(), mai_f(), rerm(), toon(), nii()]), 0.75, "intro.hollowgrin.l8")
    # 9 "นั่นไม่เป็นไรหรอก"
    add(np.concatenate([nan_f(), mai_f(), pen(), rai(), rook()]), 0.75, "intro.hollowgrin.l9")
    # 10 "ฉันรอได้"
    add(np.concatenate([chan(), ro(), dai()]), 0.95, "intro.hollowgrin.l10")
    # 11 "อีกไม่นานหรอก เพื่อนใหม่"
    add(np.concatenate([iik(), mai_f(), naan(), rook(), w_gap(), phuean(), mai_l()]), 0.0, "intro.hollowgrin.l11")

    x = np.concatenate(pieces)
    x = x / (np.abs(x).max() + 1e-9)
    x = np.tanh(x * 1.15)
    x = ms.lp(x, 6500)
    x = x + 0.45 * np.roll(x, int(0.013 * SR))
    x = ms.hp(x, 60)
    x = ms.reverb(x, 1.6, 0.30)
    return x, marks, t0[0]


# ------------------------------------------------------------------ music box
def make_music(total_sec, wind_start):
    beat = 0.64
    # original melody in A minor, 3/4 (midi note, beats)
    A = [(76, 2), (72, 1), (74, 2), (71, 1), (72, 2), (69, 1), (71, 3),
         (69, 2), (72, 1), (76, 2), (74, 1), (72, 1), (71, 1), (68, 1), (69, 3)]
    B = [(76, 2), (79, 1), (77, 2), (74, 1), (76, 2), (72, 1), (74, 3),
         (72, 2), (71, 1), (69, 2), (68, 1), (64, 1), (68, 1), (71, 1), (69, 3)]
    seq = (A + B) * 8
    out = np.zeros(int((total_sec + 8) * SR))
    t = 0.7   # small lead-in of silence
    for midi, beats in seq:
        if t > total_sec + 4: break
        f = 440.0 * 2 ** ((midi - 69) / 12.0) * 2 ** (rng.uniform(-18, 18) / 1200.0)   # slightly out of tune
        dur = 2.4; n = int(dur * SR); tt = np.arange(n) / SR
        tone = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 9)
                + 0.25 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt * 14)) * np.exp(-tt * 2.2)
        s = int(t * SR)
        if s + n > len(out): n = len(out) - s; tone = tone[:n]
        out[s:s + n] += tone * (1.0 if beats >= 2 else 0.8)
        t += beats * beat
    # music box comb: also a faint quiet low note every bar
    # wow & flutter
    tt = np.arange(len(out)) / SR
    wow = 1 + 0.010 * np.sin(2 * np.pi * 0.6 * tt) + 0.004 * np.sin(2 * np.pi * 5.0 * tt)
    # the spring unwinds: playback speed falls to 0.3x after wind_start
    speed = wow * np.where(tt < wind_start, 1.0, np.clip(1.0 - 0.7 * (tt - wind_start) / 5.5, 0.3, 1.0) ** 1.6)
    idx = np.cumsum(speed)
    idx = idx[idx < len(out) - 1]
    y = np.interp(idx, np.arange(len(out)), out)
    # cut shortly after the spring has run down, with a fade
    y = y[: int((wind_start + 9.0) * SR)]
    fade_n = int(3.0 * SR)
    y[-fade_n:] *= np.linspace(1, 0, fade_n)
    y = ms.lp(y, 4200) + 0.012 * ms.lp(rng.normal(size=len(y)), 800)
    return ms.reverb(y, 1.8, 0.35)


# ------------------------------------------------------------------ the laugh left in the dark
def make_intro_laugh():
    parts = []
    base = 160
    gaps = [0.10, 0.11, 0.13, 0.16, 0.20, 0.26, 0.34, 0.45]
    for i in range(8):
        pitch = base * (0.9 ** i)
        h = ms._noise(0.07, 800, 5000, 0.5)
        a_n = int((0.15 + 0.012 * i) * SR)
        a = ms.vowel(np.linspace(pitch * 1.1, pitch * 0.78, a_n),
                     [(530, 100, 1.0), (1840, 120, 0.7), (2480, 180, 0.3)],
                     np.hanning(a_n) ** 0.7, breath=0.15, rough=0.4)
        parts += [h, a, ms._sil(gaps[i])]
    x = np.concatenate(parts)
    x = np.tanh(x * 1.8)
    x = ms.mix(x * 0.7, ms.pitchshift(x, -7)[: len(x)] * 0.6) if False else x
    x = x + 0.4 * np.roll(x, int(0.017 * SR))
    return ms.reverb(x, 2.6, 0.38)


if __name__ == "__main__":
    voice, marks, vlen = build_voice()
    ms.save("intro_voice", voice)
    laugh = make_intro_laugh()
    ms.save("intro_laugh", laugh)
    # music: voice starts at 5.5 s in game; eyes vanish ~0.7 s after voice end
    VOICE_START = 5.5
    wind = VOICE_START + vlen + 0.2
    total = wind + 4.5 + 5.5
    music = make_music(total, wind)
    ms.save("intro_music", music)
    info = {"voice_len": round(vlen, 3), "laugh_len": round(len(laugh) / SR, 3), "music_len": round(len(music) / SR, 3),
            "marks": marks}
    print("INTRO_INFO " + json.dumps(info, ensure_ascii=False))

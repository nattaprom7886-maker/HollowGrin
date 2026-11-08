#!/usr/bin/env python3
"""Generates every texture of the Mr. Hollowgrin mod (host.png, host_glow.png, disguise.png).
UV layout MUST match HostModel.java (128x192) and the vanilla villager layout (64x64)."""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "hollowgrin", "textures", "entity")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(1337)

# ---------- helpers ----------
def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }

def clamp(c):
    return tuple(int(max(0, min(255, x))) for x in c)

def shade(c, k):
    return clamp((c[0] * (1 + k), c[1] * (1 + k), c[2] * (1 + k)))

class Tex:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.noise = rng.uniform(-1, 1, (h, w))

    def fill_rect(self, rect, fn):
        x0, y0, x1, y1 = rect
        fw, fh = x1 - x0, y1 - y0
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fn(x - x0, y - y0, fw, fh, self.noise[y, x])
                if c is not None:
                    self.px[x, y] = clamp(c) + (255,) if len(c) == 3 else tuple(int(t) for t in c)

    def put(self, rect, lx, ly, c):
        x0, y0, x1, y1 = rect
        if 0 <= lx < x1 - x0 and 0 <= ly < y1 - y0:
            self.px[x0 + lx, y0 + ly] = clamp(c) + (255,)

    def save(self, name):
        self.img.save(os.path.join(OUT, name))

def paint(tex, box, per_face, default):
    f = faces(*box)
    for name, rect in f.items():
        fn = per_face.get(name, default)
        tex.fill_rect(rect, fn)

# ---------- palette ----------
SKIN = (214, 196, 178); SKIN_D = (168, 146, 136)
COAT = (36, 25, 32); STRIPE = (96, 22, 30)
VEST = (205, 190, 148); BUTTON = (150, 20, 28)
TROUSER = (52, 48, 58); TROUSER_S = (84, 80, 92)
GLOVE = (240, 238, 232); GLOVE_D = (196, 192, 184); STAIN = (122, 18, 22)
HAT = (24, 22, 27); BAND = (122, 16, 26); BOW = (176, 22, 36)
GUM = (82, 8, 16); TONGUE = (150, 42, 54); TOOTH = (238, 230, 202); TOOTH_D = (176, 160, 130)
SHIRT = (232, 226, 214)

def solid(c, grain=0.06):
    return lambda x, y, w, h, n: shade(c, n * grain)

def pinstripe(base, stripe, period=3, grain=0.05):
    return lambda x, y, w, h, n: shade(stripe if x % period == 0 else base, n * grain)

# =====================================================================
#  HOST TEXTURE (128 x 192)
# =====================================================================
T = Tex(128, 192)

# --- torso (0,0, 18,20,12) ---
def torso_front(x, y, w, h, n):
    if y < 5 and 6 <= x <= 11 and (x - 6) >= abs(y - 0) * 0.0 and (y < 4 - abs(x - 8.5) * 0.9 + 1):
        return shade(SHIRT, n * 0.04)
    if x % 3 == 0:
        return shade(STRIPE, n * 0.05)
    return shade(COAT, n * 0.06)
paint(T, (0, 0, 18, 20, 12), {"front": torso_front, "back": pinstripe(COAT, STRIPE)}, solid(COAT))

# --- belly (0,32, 16,14,6): waistcoat with red buttons, watch chain ---
def belly_front(x, y, w, h, n):
    c = shade(VEST, n * 0.05 + (0.04 if (x // 2) % 2 else 0))
    if x in (7, 8) and y in (1, 4, 7, 10, 12):
        return BUTTON
    if y == 8 and 2 <= x <= 5:
        return (178, 150, 60)
    if x in (0, 15):
        return shade(COAT, 0.05)
    return c
paint(T, (0, 32, 16, 14, 6), {"front": belly_front}, solid(COAT))

# --- legs (0,52, 5,12,5) ---
def leg(x, y, w, h, n):
    if y >= 9:
        return shade((12, 10, 14), n * 0.1 + (0.5 if y == 9 else 0))
    return shade(TROUSER_S if x % 3 == 1 else TROUSER, n * 0.05)
paint(T, (0, 52, 5, 12, 5), {}, leg)

# --- arms (0,70, 6,22,6) sleeve + white cuff ---
def arm(x, y, w, h, n):
    if y >= 17:
        return shade(SHIRT, n * 0.04 - (0.1 if y == 17 else 0))
    return shade(STRIPE if x % 3 == 0 else COAT, n * 0.05)
paint(T, (0, 70, 6, 22, 6), {}, arm)

# --- glove (24,70, 7,7,7) ---
def glove(x, y, w, h, n):
    c = shade(GLOVE, n * 0.03)
    if y == 0 or y == h - 1:
        return shade(GLOVE_D, 0)
    if x % 2 == 1 and y > 2:        # finger seams
        c = shade(GLOVE_D, n * 0.03)
    if (x * 7 + y * 13) % 29 == 0:  # little blood specks
        c = STAIN
    return c
paint(T, (24, 70, 7, 7, 7), {}, glove)

# --- head (60,0, 14,9,14) ---
def head_front(x, y, w, h, n):
    c = shade(SKIN, n * 0.05)
    # eye sockets (hollow)
    for ex in (2, 8):
        if ex <= x < ex + 4 and 1 <= y <= 4:
            return (4, 2, 6) if (ex + 0 < x < ex + 3 or y in (2, 3)) else (20, 10, 22)
        if ex - 1 <= x <= ex + 4 and 0 <= y <= 5:
            c = shade(SKIN_D, -0.25 + n * 0.05)
    if y == 0 and 1 <= x <= 12:
        c = shade(SKIN_D, -0.3)                      # heavy brow
    if y in (4, 5) and x in (6, 7):
        c = shade(SKIN, -0.12)                       # nose
    if y == 6 and x in (6, 7) and False:
        c = (120, 80, 80)
    # grin: gum line + sharp upper teeth
    if y == 5 and (x <= 1 or x >= 12):
        return GUM                                   # smile corners curl up
    if y == 6:
        return GUM if x % 3 != 1 else TOOTH
    if y in (7, 8):
        return TOOTH if x % 3 == 1 else (GUM if y == 7 else shade(GUM, -0.5))
    return c
def head_side(front_edge_right):
    def fn(x, y, w, h, n):
        near = (x >= w - 5) if front_edge_right else (x < 5)
        if near and y in (6, 7):
            return GUM
        if near and y == 8:
            return TOOTH
        return shade(SKIN, n * 0.05 - 0.05)
    return fn
def head_mouth_bottom(x, y, w, h, n):
    ring = x < 2 or x >= w - 2 or y < 2 or y >= h - 2
    if ring:
        return TOOTH if (x + y) % 3 == 0 else shade(GUM, -0.3)
    return shade(GUM, n * 0.1)
paint(T, (60, 0, 14, 9, 14), {
    "front": head_front, "right": head_side(True), "left": head_side(False),
    "bottom": head_mouth_bottom, "top": solid(shade(SKIN, -0.2))}, solid(shade(SKIN, -0.1)))

# --- jaw (60,23, 13,5,13) ---
def jaw_front(x, y, w, h, n):
    if y <= 1:
        return TOOTH if x % 3 == 0 else shade(GUM, -0.4)
    if y == 2:
        return TOOTH if x % 3 == 0 else shade(SKIN_D, 0)
    return shade(SKIN, n * 0.05 - (0.08 if y == 4 else 0))
def jaw_top(x, y, w, h, n):
    ring = x < 2 or x >= w - 2 or y >= h - 2
    if ring:
        return TOOTH if (x + y) % 3 == 1 else shade(GUM, -0.3)
    if 3 <= x <= 9 and 2 <= y <= 9:
        return shade(TONGUE, n * 0.08 - 0.1 * (abs(x - 6) / 3.0))
    return shade(GUM, n * 0.1)
paint(T, (60, 23, 13, 5, 13), {"front": jaw_front, "top": jaw_top}, solid(shade(SKIN, -0.08)))

# --- hat brim (0,98, 20,2,20), crown (60,42, 12,14,12), band (60,70, 13,3,13) ---
paint(T, (0, 98, 20, 2, 20), {"bottom": solid(shade(HAT, -0.4))}, solid(HAT, 0.1))
paint(T, (60, 42, 12, 14, 12), {}, lambda x, y, w, h, n: shade(HAT, n * 0.1 + (0.18 if x in (2, 3) else 0)))
paint(T, (60, 70, 13, 3, 13), {}, solid(BAND, 0.08))
# bow wing (60,88, 4,3,1) and knot (72,88, 3,3,2)
paint(T, (60, 88, 4, 3, 1), {}, lambda x, y, w, h, n: shade(BOW, n * 0.08 + (0.15 if y == 0 else 0)))
paint(T, (72, 88, 3, 3, 2), {}, solid(shade(BOW, -0.25), 0.06))
# eyeball (84,88, 2,2,2) -> blood red
paint(T, (84, 88, 2, 2, 2), {}, solid((235, 20, 20), 0.05))

# --- mouth arms (0,122, 3,3,14), palm (50,122, 6,5,5), fingers (36,122) ---
def maw_arm(x, y, w, h, n):
    c = shade((176, 170, 168), n * 0.06)             # pale, bloodless flesh
    if (x * 5 + y * 3) % 17 == 0:
        c = (86, 20, 40)                              # dark veins
    return c
paint(T, (0, 122, 3, 3, 14), {}, maw_arm)
paint(T, (50, 122, 6, 5, 5), {}, glove)
for yy in range(122, 129):
    for xx in range(36, 49):
        T.px[xx, yy] = clamp(shade(GLOVE, T.noise[yy, xx] * 0.03)) + (255,)

T.save("host.png")

# =====================================================================
#  GLOW TEXTURE (eyes + mouth interior)
# =====================================================================
G = Tex(128, 192)
paint(G, (84, 88, 2, 2, 2), {}, solid((255, 40, 40), 0.0))
G_bottom = faces(60, 0, 14, 9, 14)["bottom"]
G.fill_rect(G_bottom, lambda x, y, w, h, n: (96, 0, 8))
G_jt = faces(60, 23, 13, 5, 13)["top"]
G.fill_rect(G_jt, lambda x, y, w, h, n: (96, 0, 8))
G.save("host_glow.png")

# =====================================================================
#  DISGUISE (villager UV layout, 64 x 64) - own artwork, looks almost normal
# =====================================================================
D = Tex(64, 64)
V_SKIN = (196, 150, 122); V_SKIN_D = (160, 112, 90)
ROBE = (116, 78, 46); ROBE_D = (84, 54, 32); APRON = (200, 190, 168)

def v_face(x, y, w, h, n):
    c = shade(V_SKIN, n * 0.05)
    if y == 3 and 0 <= x <= 7:
        return shade((70, 48, 34), n * 0.05)         # unibrow
    for ex in (1, 5):
        if y == 4 and ex <= x <= ex + 1:
            return (236, 236, 232)
        if y == 5 and ex <= x <= ex + 1:
            return (236, 236, 232) if x == ex + 1 else (200, 24, 24)   # tiny red pinprick pupil
    if y == 8 and 1 <= x <= 6:
        return shade(V_SKIN, -0.13)                  # faint, too-wide smile line
    if y == 7 and x in (0, 7):
        return shade(V_SKIN, -0.13)
    return c
paint(D, (0, 0, 8, 10, 8), {"front": v_face}, solid(V_SKIN, 0.05))
paint(D, (24, 0, 2, 4, 2), {}, solid(V_SKIN_D, 0.05))
paint(D, (16, 20, 8, 12, 6), {"front": lambda x, y, w, h, n: shade(APRON if 2 <= x <= 5 else ROBE, n * 0.05)}, solid(ROBE, 0.05))
paint(D, (0, 38, 8, 20, 6), {"front": lambda x, y, w, h, n: shade(ROBE_D if y in (10, 11) else ROBE, n * 0.05)}, solid(ROBE, 0.05))
paint(D, (44, 22, 4, 8, 4), {}, solid(ROBE, 0.05))
paint(D, (40, 38, 8, 4, 4), {"front": lambda x, y, w, h, n: shade(V_SKIN, n * 0.05) if y == 3 else shade(ROBE, n * 0.05)}, solid(ROBE, 0.05))
paint(D, (0, 22, 4, 12, 4), {}, lambda x, y, w, h, n: shade(ROBE_D if y < 8 else (40, 30, 24), n * 0.05))
D.save("disguise.png")
print("textures written to", os.path.abspath(OUT))

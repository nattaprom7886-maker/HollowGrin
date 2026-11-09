#!/usr/bin/env python3
"""Textures for Mr. Hollowgrin v1.1 (host.png + host_glow.png, 128x192).
The villager disguise now uses the vanilla villager texture, so nothing is generated for it.
UV offsets MUST match HostModel.java."""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "hollowgrin", "textures", "entity")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(1337)

def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }

def clamp(c): return tuple(int(max(0, min(255, x))) for x in c)
def shade(c, k): return clamp((c[0] * (1 + k), c[1] * (1 + k), c[2] * (1 + k)))

class Tex:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0)); self.px = self.img.load()
        self.noise = rng.uniform(-1, 1, (h, w))
    def fill_rect(self, rect, fn):
        x0, y0, x1, y1 = [int(round(t)) for t in rect]
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fn(x - x0, y - y0, x1 - x0, y1 - y0, self.noise[y, x])
                if c is not None:
                    self.px[x, y] = clamp(c) + (255,)
    def save(self, name): self.img.save(os.path.join(OUT, name))

def paint(tex, box, per_face, default):
    for name, rect in faces(*box).items():
        tex.fill_rect(rect, per_face.get(name, default))

SKIN = (214, 196, 178); SKIN_D = (168, 146, 136)
COAT = (36, 25, 32); STRIPE = (96, 22, 30)
VEST = (205, 190, 148); BUTTON = (150, 20, 28)
TROUSER = (52, 48, 58); TROUSER_S = (84, 80, 92)
GLOVE = (240, 238, 232); GLOVE_D = (196, 192, 184); STAIN = (122, 18, 22)
HAT = (24, 22, 27); BAND = (122, 16, 26); BOW = (176, 22, 36)
GUM = (82, 8, 16); TONGUE = (150, 42, 54); TOOTH = (238, 230, 202)
SHIRT = (232, 226, 214)

def solid(c, grain=0.06): return lambda x, y, w, h, n: shade(c, n * grain)
def pinstripe(base, stripe, period=3, grain=0.05):
    return lambda x, y, w, h, n: shade(stripe if x % period == 0 else base, n * grain)

T = Tex(128, 192)

# ---- torso 20x16x14 @ (0,0)
def torso_front(x, y, w, h, n):
    if y < 4 and 8 <= x <= 11 and y < 4 - abs(x - 9.5) * 0.8:
        return shade(SHIRT, n * 0.04)
    return shade(STRIPE if x % 3 == 0 else COAT, n * 0.05)
paint(T, (0, 0, 20, 16, 14), {"front": torso_front, "back": pinstripe(COAT, STRIPE)}, solid(COAT))

# ---- belly 18x13x6 @ (0,30): waistcoat
def belly_front(x, y, w, h, n):
    if x in (8, 9) and y in (1, 4, 7, 10):
        return BUTTON
    if y == 8 and 2 <= x <= 6:
        return (178, 150, 60)
    if x in (0, 17):
        return shade(COAT, 0.05)
    return shade(VEST, n * 0.05 + (0.04 if (x // 2) % 2 else 0))
paint(T, (0, 30, 18, 13, 6), {"front": belly_front}, solid(COAT))

# ---- legs 6x14x6 @ (0,49)
def leg(x, y, w, h, n):
    if y >= 11:
        return shade((12, 10, 14), n * 0.1 + (0.5 if y == 11 else 0))
    return shade(TROUSER_S if x % 3 == 1 else TROUSER, n * 0.05)
paint(T, (0, 49, 6, 14, 6), {}, leg)

# ---- arms 7x16x7 @ (0,70)
def arm(x, y, w, h, n):
    if y >= 12:
        return shade(SHIRT, n * 0.04 - (0.1 if y == 12 else 0))
    return shade(STRIPE if x % 3 == 0 else COAT, n * 0.05)
paint(T, (0, 70, 7, 16, 7), {}, arm)

# ---- glove 7x6x7 @ (28,70)
def glove(x, y, w, h, n):
    if y == 0 or y == h - 1:
        return GLOVE_D
    c = shade(GLOVE, n * 0.03)
    if x % 3 == 1 and y > 1:
        c = shade(GLOVE_D, n * 0.03)
    if (x * 7 + y * 13) % 31 == 0:
        c = STAIN
    return c
paint(T, (28, 70, 7, 6, 7), {}, glove)

# ---- head 16x10x14 @ (68,0)
def head_front(x, y, w, h, n):
    c = shade(SKIN, n * 0.05)
    for ex in (3, 9):                                    # hollow eye sockets, 4x4
        if ex <= x < ex + 4 and 2 <= y < 6:
            return (4, 2, 6) if (ex < x < ex + 3 or y in (3, 4)) else (22, 12, 24)
        if ex - 1 <= x <= ex + 4 and 1 <= y <= 6:
            c = shade(SKIN_D, -0.25 + n * 0.05)
    if y == 0 and 2 <= x <= 13:
        c = shade(SKIN_D, -0.3)
    if y in (5, 6) and x in (7, 8):
        c = shade(SKIN, -0.14)
    if y == 6 and (x <= 1 or x >= 14):
        return GUM                                       # smile corners curl up
    if y == 7:
        return GUM if x % 3 != 1 else TOOTH
    if y in (8, 9):
        return TOOTH if x % 3 == 1 else (GUM if y == 8 else shade(GUM, -0.5))
    return c
def head_side(front_edge_right):
    def fn(x, y, w, h, n):
        near = (x >= w - 6) if front_edge_right else (x < 6)
        if near and y in (7, 8):
            return GUM
        if near and y == 9:
            return TOOTH
        return shade(SKIN, n * 0.05 - 0.05)
    return fn
def mouth_bottom(x, y, w, h, n):
    if x < 2 or x >= w - 2 or y < 2 or y >= h - 2:
        return TOOTH if (x + y) % 3 == 0 else shade(GUM, -0.3)
    return shade(GUM, n * 0.1)
paint(T, (68, 0, 16, 10, 14), {"front": head_front, "right": head_side(True), "left": head_side(False),
                              "bottom": mouth_bottom, "top": solid(shade(SKIN, -0.2))}, solid(shade(SKIN, -0.1)))

# ---- jaw 15x5x13 @ (68,24)
def jaw_front(x, y, w, h, n):
    if y <= 1:
        return TOOTH if x % 3 == 0 else shade(GUM, -0.4)
    if y == 2:
        return TOOTH if x % 3 == 0 else SKIN_D
    return shade(SKIN, n * 0.05 - (0.08 if y == 4 else 0))
def jaw_top(x, y, w, h, n):
    if x < 2 or x >= w - 2 or y >= h - 2:
        return TOOTH if (x + y) % 3 == 1 else shade(GUM, -0.3)
    if 4 <= x <= 10 and 2 <= y <= 9:
        return shade(TONGUE, n * 0.08 - 0.1 * (abs(x - 7) / 3.0))
    return shade(GUM, n * 0.1)
paint(T, (68, 24, 15, 5, 13), {"front": jaw_front, "top": jaw_top}, solid(shade(SKIN, -0.08)))

# ---- hat: brim 20x2x20 @ (0,96), crown 12x12x12 @ (68,42), band 13x3x13 @ (68,66)
paint(T, (0, 96, 20, 2, 20), {"bottom": solid(shade(HAT, -0.4))}, solid(HAT, 0.1))
paint(T, (68, 42, 12, 12, 12), {}, lambda x, y, w, h, n: shade(HAT, n * 0.1 + (0.18 if x in (2, 3) else 0)))
paint(T, (68, 66, 13, 3, 13), {}, solid(BAND, 0.08))
paint(T, (68, 84, 4, 3, 1), {}, lambda x, y, w, h, n: shade(BOW, n * 0.08 + (0.15 if y == 0 else 0)))
paint(T, (80, 84, 3, 3, 2), {}, solid(shade(BOW, -0.25), 0.06))
paint(T, (92, 84, 2, 2, 2), {}, solid((235, 20, 20), 0.05))      # eyeball

# ---- mouth arms: seg 3x3x14 @ (0,122), palm 6x5x5 @ (50,122), fingers @ (36,122)
def maw_arm(x, y, w, h, n):
    c = shade((176, 170, 168), n * 0.06)
    return (86, 20, 40) if (x * 5 + y * 3) % 17 == 0 else c
paint(T, (0, 122, 3, 3, 14), {}, maw_arm)
paint(T, (50, 122, 6, 5, 5), {}, glove)
for yy in range(122, 129):
    for xx in range(36, 49):
        T.px[xx, yy] = clamp(shade(GLOVE, T.noise[yy, xx] * 0.03)) + (255,)
T.save("host.png")

# ---- glow: eyes + red inside of the open mouth
G = Tex(128, 192)
paint(G, (92, 84, 2, 2, 2), {}, solid((255, 40, 40), 0.0))
G.fill_rect(faces(68, 0, 16, 10, 14)["bottom"], lambda x, y, w, h, n: (96, 0, 8))
G.fill_rect(faces(68, 24, 15, 5, 13)["top"], lambda x, y, w, h, n: (96, 0, 8))
G.save("host_glow.png")

old = os.path.join(OUT, "disguise.png")
if os.path.exists(old):
    os.remove(old)
print("ok")


# =====================================================================
#  MAW TEXTURE (64x64): the inside of the villager's open mouth + black hands.
#  Used by MawParts.java / MawLayer.java. Transparent pixels = cut-out (teeth gaps).
# =====================================================================
M = Tex(64, 64)
INK = (13, 12, 16); INK_HI = (36, 34, 42)
ROOF = (112, 20, 32); FLOOR = (98, 14, 26); TONG = (178, 64, 80)

def teeth_plate(upper):
    def fn(x, y, w, h, n):
        if h != 3 or w < 5:
            return shade(TOOTH, n * 0.04 - 0.1)
        yy = y if upper else (2 - y)              # row 0 = base of the tooth
        if yy == 0:
            return shade(TOOTH, n * 0.04 - 0.12)
        if x % 2 == 0:
            return shade(TOOTH, n * 0.04 + (0.05 if yy == 2 else -0.05))
        return None                               # gap between teeth -> cut-out
    return fn

paint(M, (0, 0, 8, 1, 8), {}, lambda x, y, w, h, n: shade(ROOF, n * 0.12 + (0.15 if (x * 3 + y * 5) % 11 == 0 else 0)))
paint(M, (32, 0, 8, 1, 8), {}, lambda x, y, w, h, n: shade(FLOOR, n * 0.12))
paint(M, (0, 10, 7, 3, 1), {}, teeth_plate(True))
paint(M, (16, 10, 7, 3, 1), {}, teeth_plate(False))
paint(M, (0, 15, 1, 3, 7), {}, teeth_plate(True))
paint(M, (16, 15, 1, 3, 7), {}, teeth_plate(False))
def tongue(x, y, w, h, n):
    c = shade(TONG, n * 0.08)
    if w >= 5 and abs(x - w / 2.0) < 0.6:
        c = shade(TONG, -0.18)                    # groove down the middle
    return c
paint(M, (32, 10, 3, 2, 7), {}, tongue)
def ink(x, y, w, h, n):
    c = shade(INK, n * 0.25)
    if (x + y * 2) % 7 == 0:
        c = INK_HI
    return c
paint(M, (0, 26, 3, 3, 12), {}, ink)
paint(M, (32, 26, 6, 5, 5), {}, ink)
for yy in range(38, 44):
    for xx in range(32, 44):
        M.px[xx, yy] = clamp(shade(INK, M.noise[yy, xx] * 0.25)) + (255,)
M.save("maw.png")
print("maw.png written")

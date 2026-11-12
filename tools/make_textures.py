#!/usr/bin/env python3
"""Textures for Mr. Hollowgrin v1.7 (host.png + host_glow.png, 128x192).
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

SHIRT_D = (190, 184, 172)
OVER = (46, 50, 62); OVER_D = (30, 33, 42); OVER_L = (70, 76, 92); RIVET = (176, 150, 70)
SOCKET = (4, 2, 6); BAG = (74, 66, 72)

# ---- chest 16x10x8 @ (0,0): shirt + overall straps
def strap_cols(x): return x in (4, 5, 10, 11)
def chest_front(x, y, w, h, n):
    if strap_cols(x):
        if y == 5 and x in (4, 11): return RIVET
        return shade(OVER, n * 0.05)
    return shade(SHIRT if y > 0 else SHIRT_D, n * 0.04 - (0.08 if x in (0, 15) else 0))
def chest_back(x, y, w, h, n):
    if strap_cols(x) or y == 9:
        return shade(OVER, n * 0.05)
    return shade(SHIRT_D, n * 0.04)
def chest_top(x, y, w, h, n):
    return shade(OVER, n * 0.05) if strap_cols(x) else shade(SHIRT, n * 0.04)
paint(T, (0, 0, 16, 10, 8), {"front": chest_front, "back": chest_back, "top": chest_top}, solid(SHIRT_D, 0.04))

# ---- waist 11x4x6 @ (0,20)
paint(T, (0, 20, 11, 4, 6), {}, lambda x, y, w, h, n: shade(OVER_D if y == h - 1 else OVER, n * 0.05))

# ---- bib 8x8x1 @ (0,32)
def bib_front(x, y, w, h, n):
    if (x, y) in ((0, 0), (7, 0)): return RIVET
    if 1 <= x <= 6 and 4 <= y <= 6 and (x in (1, 6) or y in (4, 6)):
        return shade(OVER_L, n * 0.05)                    # chest pocket stitching
    return shade(OVER, n * 0.06)
paint(T, (0, 32, 8, 8, 1), {"front": bib_front}, solid(OVER_D))

# ---- neck 4x5x4 @ (24,32): long, tendons showing
def neck(x, y, w, h, n):
    c = shade(SKIN, n * 0.05 - 0.05)
    if x in (0, 3) or (y in (1, 3) and x in (1, 2)):
        c = shade(SKIN_D, -0.2 + n * 0.04)
    return c
paint(T, (24, 32, 4, 5, 4), {}, neck)

# ---- ear 2x5x3 @ (44,32), nose 2x3x2 @ (56,32)
paint(T, (44, 32, 2, 5, 3), {}, lambda x, y, w, h, n: shade(SKIN_D if x == 0 else SKIN, n * 0.05 - 0.05))
paint(T, (56, 32, 2, 3, 2), {}, lambda x, y, w, h, n: shade(SKIN, n * 0.04 + (0.05 if y == 0 else -0.12 if y == h - 1 else 0)))

# ---- legs 5x14x5 @ (0,49): overalls + black boots
def leg(x, y, w, h, n):
    if y >= 11:
        return shade((12, 10, 14), n * 0.1 + (0.5 if y == 11 else 0))
    if y == 10:
        return shade(OVER_L, n * 0.05)                    # rolled cuff
    return shade(OVER_D if x in (0, w - 1) else OVER, n * 0.05)
paint(T, (0, 49, 5, 14, 5), {}, leg)

# ---- arms: bare, muscular, veins. upper 5x9x5 @ (0,70), forearm 5x9x5 @ (0,86)
def upper_arm(x, y, w, h, n):
    c = shade(SKIN, n * 0.05 - (0.1 if x in (0, w - 1) else 0))
    return c
def fore_arm(x, y, w, h, n):
    c = shade(SKIN, n * 0.05 - (0.1 if x in (0, w - 1) else 0))
    if (x * 3 + y * 5) % 13 == 0: c = shade(SKIN_D, -0.25)       # veins
    if y >= h - 1: c = shade(SHIRT_D, n * 0.04)           # cuff
    return c
paint(T, (0, 70, 5, 9, 5), {}, upper_arm)
paint(T, (0, 86, 5, 9, 5), {}, fore_arm)

# ---- glove 5x6x5 @ (28,70)
def glove(x, y, w, h, n):
    if y == 0 or y == h - 1:
        return GLOVE_D
    c = shade(GLOVE, n * 0.03)
    if x % 3 == 1 and y > 1:
        c = shade(GLOVE_D, n * 0.03)
    if (x * 7 + y * 13) % 31 == 0:
        c = STAIN
    return c
paint(T, (28, 70, 5, 6, 5), {}, glove)

# ---- head 10x12x9 @ (68,0): long face, huge hollow sockets, ear-to-ear grin
def socket_cells(x0):
    cells = set()
    for y in range(2, 7):
        for x in range(x0, x0 + 3):
            if y in (2, 6) and x in (x0, x0 + 2) and y == 6:
                continue
            cells.add((x, y))
    return cells
SOCK = socket_cells(1) | socket_cells(6)
def head_front(x, y, w, h, n):
    if (x, y) in SOCK:
        return shade(SOCKET, n * 0.2)
    c = shade(SKIN, n * 0.05)
    if y <= 1:
        c = shade(SKIN_D, -0.22 + n * 0.04)                # shadow under the hat
    # eye bags / dark smudge around the sockets
    near = any(abs(x - sx) <= 1 and abs(y - sy) <= 1 for sx, sy in SOCK)
    if near: c = shade(BAG, n * 0.05)
    if y == 7 and (x in (1, 2, 3, 6, 7, 8)): c = shade(BAG, -0.25)
    if x in (4, 5) and 4 <= y <= 8: c = shade(SKIN, -0.1 + n * 0.04)   # bridge + nose shadow
    # grin: gum line + needle teeth, curling up at the cheeks
    if y == 8 and (x <= 1 or x >= 8): return GUM
    if y == 9: return GUM
    if y == 10: return TOOTH if x % 2 == 0 else shade(GUM, -0.5)
    if y == 11: return TOOTH if x % 2 == 0 else (14, 6, 10)
    return c
def head_side(front_edge_right):
    def fn(x, y, w, h, n):
        near = (x >= w - 3) if front_edge_right else (x < 3)
        if near and y in (9, 10): return GUM
        if near and y == 11: return TOOTH
        return shade(SKIN, n * 0.05 - 0.05)
    return fn
def mouth_bottom(x, y, w, h, n):
    if x < 1 or x >= w - 1 or y < 1 or y >= h - 1:
        return TOOTH if (x + y) % 2 == 0 else shade(GUM, -0.3)
    return shade(GUM, n * 0.1)
paint(T, (68, 0, 10, 12, 9), {"front": head_front, "right": head_side(True), "left": head_side(False),
                              "bottom": mouth_bottom, "top": solid(shade(SKIN, -0.2))}, solid(shade(SKIN, -0.1)))

# ---- jaw 9x4x9 @ (68,24)
def jaw_front(x, y, w, h, n):
    if y <= 1:
        return TOOTH if x % 2 == 1 else shade(GUM, -0.4)
    if y == 2:
        return shade(GUM, -0.2)
    return shade(SKIN, n * 0.05 - 0.08)
def jaw_top(x, y, w, h, n):
    if x < 1 or x >= w - 1 or y >= h - 1:
        return TOOTH if (x + y) % 2 == 1 else shade(GUM, -0.3)
    if 2 <= x <= 6 and 2 <= y <= 7:
        return shade(TONGUE, n * 0.08 - 0.1 * (abs(x - 4) / 2.0))
    return shade(GUM, n * 0.1)
paint(T, (68, 24, 9, 4, 9), {"front": jaw_front, "top": jaw_top}, solid(shade(SKIN, -0.08)))

# ---- hat: brim 18x2x18 @ (0,100), crown 11x14x11 @ (68,42), band 12x3x12 @ (68,70)
paint(T, (0, 100, 18, 2, 18), {"bottom": solid(shade(HAT, -0.4))}, solid(HAT, 0.1))
paint(T, (68, 42, 11, 14, 11), {}, lambda x, y, w, h, n: shade(HAT, n * 0.1 + (0.18 if x in (2, 3) else 0)))
paint(T, (68, 70, 12, 3, 12), {}, solid(BAND, 0.08))
paint(T, (68, 88, 4, 3, 1), {}, lambda x, y, w, h, n: shade(BOW, n * 0.08 + (0.15 if y == 0 else 0)))
paint(T, (80, 88, 3, 3, 2), {}, solid(shade(BOW, -0.25), 0.06))
paint(T, (92, 88, 1, 1, 1), {}, solid((235, 20, 20), 0.05))      # eyeball

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
paint(G, (92, 88, 1, 1, 1), {}, solid((255, 40, 40), 0.0))
G.fill_rect(faces(68, 0, 10, 12, 9)["bottom"], lambda x, y, w, h, n: (96, 0, 8))
G.fill_rect(faces(68, 24, 9, 4, 9)["top"], lambda x, y, w, h, n: (96, 0, 8))
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

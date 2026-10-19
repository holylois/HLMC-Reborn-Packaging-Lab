"""Relic icons, original pixel art drawn from shapes plus an automatic dark outline (vanilla-like readability)."""
from PIL import Image
import os, sys

class Icon:
    def __init__(self): self.px = {}
    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: self.px[(x, y)] = c
    def diag(self, x0, y0, n, c, step=(1, -1)):
        for i in range(n): self.put(x0 + i * step[0], y0 + i * step[1], c)
    def thick_diag(self, x0, y0, n, c, edge=None):
        """Two-pixel bar running up-right: main colour plus a lighter or darker edge below it."""
        for i in range(n):
            self.put(x0 + i, y0 - i, c)
            if edge: self.put(x0 + i, y0 - i + 1, edge)
    def rect(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1): self.put(x, y, c)
    def disc(self, cx, cy, r, c):
        for x in range(16):
            for y in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.5: self.put(x, y, c)
    def image(self, outline=(30, 22, 18)):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        filled = set(self.px)
        for (x, y) in [(x, y) for x in range(16) for y in range(16) if (x, y) not in filled]:
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))): im.putpixel((x, y), outline + (255,))
        for (x, y), c in self.px.items(): im.putpixel((x, y), c + (255,))
        return im

WOOD_D, WOOD, WOOD_L = (74, 48, 28), (112, 76, 44), (150, 108, 64)
LEATHER, LEATHER_D = (120, 72, 40), (86, 50, 28)
BRASS_D, BRASS, BRASS_L = (150, 104, 30), (214, 160, 54), (255, 226, 130)
VERD_D, VERD, VERD_L = (40, 104, 96), (72, 160, 140), (140, 214, 190)
AQUA_D, AQUA, AQUA_L = (30, 110, 130), (70, 190, 210), (170, 240, 245)
IRON_D, IRON, IRON_L = (96, 100, 108), (160, 166, 176), (222, 226, 232)
STONE_D, STONE, STONE_L = (84, 84, 88), (124, 124, 128), (164, 164, 168)
EMBER_D, EMBER, EMBER_L = (170, 50, 20), (250, 120, 30), (255, 214, 120)
FLAME_O, FLAME_Y, FLAME_W = (255, 150, 40), (255, 214, 90), (255, 250, 220)

def lantern():
    i = Icon()
    i.rect(6, 0, 8, 0, IRON_D); i.put(5, 1, IRON_D); i.put(9, 1, IRON_D); i.rect(6, 2, 8, 2, IRON_D)      # hanging ring
    i.rect(5, 3, 9, 3, BRASS); i.rect(4, 4, 10, 4, BRASS_D); i.put(7, 3, BRASS_L)                       # cap
    i.rect(4, 5, 4, 10, BRASS_D); i.rect(10, 5, 10, 10, BRASS_D)                                        # frame posts
    i.rect(5, 5, 9, 10, AQUA_D); i.rect(5, 5, 5, 10, AQUA); i.rect(6, 5, 6, 6, AQUA_L)                 # aqua glass
    i.rect(7, 7, 7, 9, FLAME_O); i.put(7, 6, FLAME_Y); i.put(7, 8, FLAME_W); i.put(6, 8, FLAME_O); i.put(8, 8, FLAME_O)  # flame
    i.rect(4, 11, 10, 11, BRASS_D); i.rect(5, 12, 9, 12, BRASS); i.put(7, 12, BRASS_L)                  # base
    i.put(7, 13, IRON_D); i.rect(6, 14, 8, 14, BRASS); i.put(7, 15, BRASS_D)                            # little bell charm
    return i

def clapper():
    i = Icon()
    i.thick_diag(1, 14, 3, LEATHER, LEATHER_D)                                  # grip
    i.put(3, 10, BRASS_D); i.put(4, 11, BRASS_D); i.put(5, 12, BRASS_D); i.put(2, 9, BRASS_D)   # cross guard
    i.thick_diag(4, 11, 6, VERD, VERD_D)                                        # bronze rod with patina
    i.disc(11.5, 3.5, 2.6, VERD_D); i.disc(11, 3, 1.8, VERD); i.put(10, 2, VERD_L); i.put(11, 2, VERD_L)  # clapper ball
    for x, y in ((13, 4), (12, 5), (6, 9)): i.put(x, y, (200, 196, 170))        # barnacles
    return i

def hook(cast=False):
    i = Icon()
    i.thick_diag(1, 14, 12, WOOD, WOOD_D)                                       # rod
    for k in (2, 5): i.put(1 + k, 14 - k, VERD); i.put(1 + k, 15 - k, VERD_D)   # aqua wraps
    if not cast:
        for y in range(4, 11): i.put(13, y, (220, 220, 220))                    # line
        i.rect(12, 11, 14, 12, BRASS); i.put(13, 13, BRASS_D); i.put(13, 11, BRASS_L)   # bell on the line
    i.put(13, 2, BRASS); i.put(13, 3, BRASS_D)                                  # brass tip
    return i

def hammer():
    """Mace-like: long haft, a heavy rounded stone head with iron flanges, one glowing rune."""
    i = Icon()
    i.thick_diag(1, 14, 8, WOOD, WOOD_D)
    i.put(3, 12, LEATHER); i.put(4, 11, LEATHER); i.put(3, 13, LEATHER_D)
    i.disc(11, 4, 3.4, STONE_D); i.disc(10.6, 3.6, 2.6, STONE); i.put(9, 2, STONE_L); i.put(10, 2, STONE_L); i.put(9, 3, STONE_L)
    for x, y in ((11, 0), (15, 4), (11, 8), (7, 4)): i.put(x, y, IRON_D)            # flanges
    for x, y in ((14, 1), (14, 7), (8, 1)): i.put(x, y, IRON)
    for x, y in ((10, 3), (11, 3), (10, 4), (12, 4), (10, 5), (11, 5), (12, 6)): i.put(x, y, EMBER)   # rune
    i.put(11, 4, EMBER_L)
    return i

def blade():
    i = Icon()
    i.thick_diag(1, 14, 3, LEATHER, LEATHER_D)                                  # rough wrap
    i.put(3, 10, STONE_D); i.put(4, 11, STONE_D); i.put(2, 9, STONE_D); i.put(5, 12, STONE_D)   # guard
    i.thick_diag(4, 11, 7, IRON, IRON_D)                                        # blade
    i.diag(5, 9, 6, EMBER)                                                      # glowing unfinished edge
    i.put(10, 4, EMBER_L); i.put(11, 4, IRON_D); i.put(11, 3, IRON_D)            # blunt, unfinished end
    return i

ICONS = {"loiss_lantern": lantern, "drowned_bell_clapper": clapper, "bell_keepers_hook": hook,
         "first_rune_hammer": hammer, "unfinished_blade": blade}

if __name__ == "__main__":
    out = sys.argv[1]
    sheet = Image.new("RGBA", (16 * 7, 16), (40, 40, 48, 255))
    n = 0
    for name, draw in ICONS.items():
        im = draw().image(); im.save(os.path.join(out, name + ".png")); sheet.alpha_composite(im, (16 * n, 0)); n += 1
    cast = hook(True).image(); cast.save(os.path.join(out, "bell_keepers_hook_cast.png")); sheet.alpha_composite(cast, (16 * n, 0)); n += 1
    staff = Image.open(os.path.join(out, "staff_of_quiet_roads.png")).convert("RGBA"); sheet.alpha_composite(staff, (16 * n, 0))
    sheet.resize((16 * 7 * 12, 16 * 12), Image.NEAREST).save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "relic-sheet.png"))

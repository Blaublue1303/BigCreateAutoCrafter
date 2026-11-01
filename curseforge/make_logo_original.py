"""Builds a CurseForge logo (400 x 400 PNG) drawn entirely by this script, without any Create or Minecraft textures:
the 21-crafter layout (5 x 5 without corners) loaded with a big recipe, on the same blueprint background.

Usage: python3 -I curseforge/make_logo_original.py curseforge/logo.png
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_logo  # noqa: E402  (same folder; also works with python3 -I)

SIZE = make_logo.SIZE
S = 4  # supersampling

LAYOUT = [".###.", "#####", "#####", "#####", ".###."]
# what each crafter holds: a = alloy (grey-green), p = plank (wood), s = stone (dark grey); like the crushing wheel
CONTENT = [".aaa.", "aapaa", "apspa", "aapaa", ".aaa."]
COLOURS = {"a": ((176, 186, 178), (120, 130, 124)), "p": ((214, 170, 104), (150, 108, 58)), "s": ((128, 128, 132), (82, 82, 88))}


def tile(draw, x, y, size, content):
    r = size // 7
    # brass frame
    draw.rounded_rectangle((x, y, x + size, y + size), radius=r, fill=(70, 42, 24))
    draw.rounded_rectangle((x + S * 3, y + S * 3, x + size - S * 3, y + size - S * 3), radius=r - S * 2, fill=(214, 160, 70))
    draw.rounded_rectangle((x + S * 7, y + S * 7, x + size - S * 7, y + size - S * 7), radius=r - S * 4, fill=(92, 58, 34))
    # item cube in the middle (simple isometric block)
    light, dark = COLOURS[content]
    c = size / 2
    w = size * 0.30
    cx, cy = x + c, y + c + size * 0.02
    top = [(cx, cy - w * 0.9), (cx + w, cy - w * 0.4), (cx, cy + w * 0.1), (cx - w, cy - w * 0.4)]
    left = [(cx - w, cy - w * 0.4), (cx, cy + w * 0.1), (cx, cy + w * 1.0), (cx - w, cy + w * 0.5)]
    right = [(cx + w, cy - w * 0.4), (cx, cy + w * 0.1), (cx, cy + w * 1.0), (cx + w, cy + w * 0.5)]
    draw.polygon(top, fill=tuple(min(255, v + 30) for v in light))
    draw.polygon(left, fill=light)
    draw.polygon(right, fill=dark)


def cog(draw, cx, cy, radius, teeth=10):
    points = []
    for i in range(teeth * 2):
        angle = math.pi * i / teeth
        r = radius if i % 2 == 0 else radius * 0.8
        for da in (-0.22, 0.22):
            a = angle + da * math.pi / teeth
            points.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    draw.polygon(points, fill=(232, 182, 84), outline=(70, 42, 24), width=S * 4)
    draw.ellipse((cx - radius * 0.55, cy - radius * 0.55, cx + radius * 0.55, cy + radius * 0.55), fill=(150, 98, 40),
                 outline=(70, 42, 24), width=S * 3)
    draw.ellipse((cx - radius * 0.2, cy - radius * 0.2, cx + radius * 0.2, cy + radius * 0.2), fill=(70, 42, 24))


def main(target):
    big = SIZE * S
    art = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(art)
    cell = 58 * S
    gap = 4 * S
    grid = 5 * cell + 4 * gap
    left = (big - grid) // 2 - 14 * S
    top = (big - grid) // 2 - 14 * S
    for row, line in enumerate(LAYOUT):
        for col, ch in enumerate(line):
            if ch == "#":
                tile(draw, left + col * (cell + gap), top + row * (cell + gap), cell, CONTENT[row][col])
    cog(draw, big - 80 * S, big - 80 * S, 44 * S)
    art = art.resize((SIZE, SIZE), Image.LANCZOS)

    logo = make_logo.background()
    shadow = Image.new("RGBA", art.size, (0, 0, 0, 0))
    shadow.putalpha(art.getchannel("A").point(lambda a: 140 if a > 0 else 0))
    shifted = Image.new("RGBA", art.size, (0, 0, 0, 0))
    shifted.paste(shadow, (5, 8), shadow)
    logo = Image.alpha_composite(logo, shifted.filter(ImageFilter.GaussianBlur(7)))
    logo = Image.alpha_composite(logo, art)
    logo.putalpha(make_logo.background().getchannel("A"))
    logo.save(target)


if __name__ == "__main__":
    main(sys.argv[1])

"""Builds the CurseForge project logo (400 x 400 PNG) from the in-game item renders of the client smoke test.

Input: run-smoketest/screenshots/bcac-item-renders.png (Mechanical Crafter left, Crushing Wheel right, on magenta).
Usage: python3 -I curseforge/make_logo.py <item-renders.png> <logo.png> [--round]
  default: rounded square with the crafter and a crushing wheel
  --round: round logo with one large crafter, nothing else
"""
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

SIZE = 400
SCALE = 4  # draw the background at 4x and scale down for smooth edges
KEY = (255, 0, 255)


def cut_out(render, box):
    """Crops one item render and makes the magenta key colour transparent."""
    part = render.crop(box).convert("RGBA")
    pixels = part.load()
    for y in range(part.height):
        for x in range(part.width):
            r, g, b, _ = pixels[x, y]
            if (r, g, b) == KEY:
                pixels[x, y] = (0, 0, 0, 0)
    return part.crop(part.getbbox())


def shape_outline(draw, box, round_logo, radius, **kwargs):
    """Draws the logo's outline shape: a circle or a rounded square."""
    if round_logo:
        draw.ellipse(box, **kwargs)
    else:
        draw.rounded_rectangle(box, radius=radius, **kwargs)


def background(round_logo=False):
    big = SIZE * SCALE
    radius = 72 * SCALE
    # blueprint blue, lighter in the middle
    bg = Image.new("RGBA", (big, big))
    centre, edge = (96, 156, 230), (28, 74, 150)
    draw = ImageDraw.Draw(bg)
    for r in range(big, 0, -4):
        t = r / big
        colour = tuple(int(centre[i] + (edge[i] - centre[i]) * min(1, t * 1.25)) for i in range(3))
        c = big // 2
        draw.ellipse((c - r, c - r, c + r, c + r), fill=colour + (255,))
    # schematic grid
    grid = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(grid)
    step = 25 * SCALE
    for i in range(0, big + 1, step):
        major = (i // step) % 4 == 0
        colour = (210, 230, 255, 70 if major else 34)
        width = 2 * SCALE if major else SCALE
        gdraw.line((i, 0, i, big), fill=colour, width=width)
        gdraw.line((0, i, big, i), fill=colour, width=width)
    bg = Image.alpha_composite(bg, grid)
    # vignette
    vignette = Image.new("L", (big, big), 0)
    shape_outline(ImageDraw.Draw(vignette), (0, 0, big, big), round_logo, radius, outline=255, width=46 * SCALE)
    vignette = vignette.filter(ImageFilter.GaussianBlur(40 * SCALE))
    dark = Image.new("RGBA", (big, big), (8, 22, 60, 255))
    dark.putalpha(ImageChops.multiply(vignette, Image.new("L", (big, big), 170)))
    bg = Image.alpha_composite(bg, dark)
    # frame: dark outline with a light inner rim
    frame = ImageDraw.Draw(bg)
    shape_outline(frame, (0, 0, big - 1, big - 1), round_logo, radius, outline=(14, 34, 78, 255), width=7 * SCALE)
    shape_outline(frame, (7 * SCALE, 7 * SCALE, big - 7 * SCALE, big - 7 * SCALE), round_logo, radius - 7 * SCALE,
                  outline=(150, 200, 255, 150), width=2 * SCALE)
    # gloss on the upper half
    gloss = Image.new("L", (big, big), 0)
    ImageDraw.Draw(gloss).ellipse((-big // 3, -big * 3 // 4, big * 4 // 3, big // 2), fill=44)
    gloss = gloss.filter(ImageFilter.GaussianBlur(20 * SCALE))
    white = Image.new("RGBA", (big, big), (255, 255, 255, 255))
    white.putalpha(gloss)
    bg = Image.alpha_composite(bg, white)
    # rounded corners
    mask = Image.new("L", (big, big), 0)
    shape_outline(ImageDraw.Draw(mask), (0, 0, big - 1, big - 1), round_logo, radius, fill=255)
    bg.putalpha(mask)
    return bg.resize((SIZE, SIZE), Image.LANCZOS)


def with_shadow(canvas, item, pos, blur=10, offset=(6, 10), alpha=150):
    shadow = Image.new("RGBA", item.size, (0, 0, 0, 0))
    shadow.putalpha(item.getchannel("A").point(lambda a: alpha if a > 0 else 0))
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    layer.paste(shadow, (pos[0] + offset[0], pos[1] + offset[1]), shadow)
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    canvas = Image.alpha_composite(canvas, layer)
    canvas.alpha_composite(item, pos)
    return canvas


def main(source, target, round_logo=False):
    render = Image.open(source).convert("RGB")
    half = render.width // 2
    crafter = cut_out(render, (0, 0, half, render.height))

    if round_logo:
        # one large crafter, centred in the circle (its corners must stay inside)
        crafter = crafter.resize((262, round(262 * crafter.height / crafter.width)), Image.LANCZOS)
        logo = background(round_logo=True)
        logo = with_shadow(logo, crafter, ((SIZE - crafter.width) // 2, (SIZE - crafter.height) // 2 - 6), blur=12, offset=(6, 12))
        logo.putalpha(background(round_logo=True).getchannel("A"))
        logo.save(target)
        return

    wheel = cut_out(render, (half, 0, render.width, render.height))
    crafter = crafter.resize((250, round(250 * crafter.height / crafter.width)), Image.LANCZOS)
    wheel = wheel.resize((150, round(150 * wheel.height / wheel.width)), Image.LANCZOS)

    logo = background()
    logo = with_shadow(logo, crafter, ((SIZE - crafter.width) // 2 - 22, (SIZE - crafter.height) // 2 - 18))
    logo = with_shadow(logo, wheel, (SIZE - wheel.width - 34, SIZE - wheel.height - 40), blur=8, offset=(4, 8))
    # keep the rounded corners transparent
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, SIZE - 1, SIZE - 1), radius=72, fill=255)
    logo.putalpha(ImageChops.multiply(logo.getchannel("A"), mask))
    logo.save(target)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], "--round" in sys.argv[3:])

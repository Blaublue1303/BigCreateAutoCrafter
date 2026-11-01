"""Makes the screenshots for the CurseForge description, whose image upload only takes images narrower than 850 px.

Menu shots are cropped to the menu so the text stays sharp; world shots are scaled down as a whole.
Usage: python3 -I curseforge/make_description_images.py   (from the repository root)
"""
from pathlib import Path

from PIL import Image

SOURCE = Path("curseforge/screenshots")
TARGET = Path("curseforge/description-images")
MAX_WIDTH = 840

# file -> crop box (left, top, right, bottom) in the 1600 x 900 screenshot, or None for the whole picture
CROPS = {
    "01-crafter-setup.png": None,
    "02-register-region.png": (0, 180, 1260, 900),
    "03-stock-keeper-tag.png": (210, 20, 1140, 870),
    "04-stock-keeper-panel.png": (210, 20, 1140, 870),
    "05-profile-menu.png": (430, 225, 1230, 675),
    "06-factory-gauge.png": (480, 190, 1280, 710),
    "07-factory-gauge-tooltip.png": (490, 190, 1330, 710),
}


def main():
    TARGET.mkdir(exist_ok=True)
    for name, box in CROPS.items():
        image = Image.open(SOURCE / name).convert("RGB")
        if box is not None:
            image = image.crop(box)
        if image.width > MAX_WIDTH:
            image = image.resize((MAX_WIDTH, round(MAX_WIDTH * image.height / image.width)), Image.LANCZOS)
        image.save(TARGET / name, optimize=True)
        print(name, image.size)


if __name__ == "__main__":
    main()

"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo grove, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONT = ROOT / "app/src/main/res/font"

# The app's own palette (ui/theme/Theme.kt).
KRAFT = (0xD8, 0xC3, 0x9D)
GRAIN = (0xC9, 0xB1, 0x86)
PAPER = (0xF1, 0xE7, 0xD1)
INK = (0x2F, 0x2A, 0x24)
MOSS = (0x55, 0x70, 0x3A)

# The order they appear in the store, with the caption over each: an ink line and a moss one.
PAGES = [
    ("2-tree", ("Every meeting", "is a ring"), ("Каждая встреча —", "годовое кольцо")),
    ("1-grove", ("A grove of real trees", "on an old map"), ("Роща настоящих", "деревьев на карте")),
    ("3-meet", ("Only at the tree,", "within 50 metres"), ("Только у дерева,", "в пределах 50 метров")),
    ("4-hug", ("A hug: the heartbeat", "slows to the tree's"), ("Объятие: сердце", "бьётся в ритме дерева")),
    ("11-key", ("Work out the species", "like an old field guide"), ("Порода — по ключу,", "как в старом определителе")),
    ("6-chronicle", ("A timelapse of the tree", "through the years"), ("Таймлапс дерева", "сквозь годы")),
    ("5-herbarium", ("A fallen leaf a season", "for the herbarium"), ("Упавший лист в сезон —", "в гербарий")),
    ("7-hollow", ("A note in the hollow", "opens a year later"), ("Записка в дупле", "откроется через год")),
]


def font(name, size):
    return ImageFont.truetype(str(FONT / name), size)


def wood(w, h, cx, cy):
    """Kraft paper with the rings of a sawn trunk spreading from (cx, cy), as on the tree's page."""
    g = Image.new("RGB", (w, h), KRAFT)
    d = ImageDraw.Draw(g)
    r, step = 60, 34
    while r < w + h:
        d.ellipse((cx - r, cy - r * 0.92, cx + r, cy + r * 0.92), outline=GRAIN, width=2)
        r += step
        step += 1
    return g.convert("RGBA")


def lay(g, card, x, y):
    """A screen on the page: a soft shadow and a thin ink rule round it."""
    sh = Image.new("RGBA", (card.width + 80, card.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle((40, 50, 40 + card.width, 50 + card.height), fill=(50, 35, 20, 110))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)), (x - 40, y - 40))
    g.alpha_composite(card.convert("RGBA"), (x, y))
    ImageDraw.Draw(g).rectangle((x - 3, y - 3, x + card.width + 2, y + card.height + 2), outline=INK, width=3)


def fit(name, text, size, width):
    while font(name, size).getlength(text) > width:
        size -= 2
    return font(name, size)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = wood(W, H, W - 60, H - 40)
    d = ImageDraw.Draw(g)
    d.rectangle((0, 0, W, 330), fill=PAPER)
    d.rectangle((60, 40, W - 60, 46), fill=INK)
    size = min(fit("pt_serif_bold.ttf", line, 80, W - 140).size for line in caption)
    title = font("pt_serif_bold.ttf", size)
    d.text((W / 2, 135), caption[0], font=title, fill=INK, anchor="mm")
    d.text((W / 2, 235), caption[1], font=title, fill=MOSS, anchor="mm")
    d.rectangle((60, 318, W - 60, 321), fill=INK)
    im = Image.open(src).convert("RGB")
    w = 820
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 380
    # The screen runs off the bottom edge, like a notebook page that goes on.
    lay(g, im.crop((0, 0, w, H - top + 20)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = wood(W, H, W - 230, H // 2)
    ImageDraw.Draw(g).rectangle((0, 0, 540, H), fill=PAPER)
    # The cross-section card from the first page of the welcome.
    ring = Image.open(SHOTS / lang / "0-welcome-2.jpg").convert("RGB").crop((40, 100, 1040, 1010))
    ring = ring.resize((round(ring.width * 400 / ring.height), 400), Image.LANCZOS)
    lay(g, ring, W - ring.width - 40, 46)
    d = ImageDraw.Draw(g)
    name, lines = ("Heartwood", ["Befriend a real tree.", "Every meeting", "is a ring."]) if lang == "en" else \
        ("Сердцевина", ["Подружитесь с деревом.", "Каждая встреча —", "годовое кольцо."])
    d.rectangle((48, 100, 500, 106), fill=INK)
    d.text((48, 118), name, font=fit("pt_serif_bold.ttf", name, 96, 452), fill=INK)
    d.rectangle((48, 240, 500, 242), fill=INK)
    sub = fit("pt_serif_bold.ttf", lines[0], 34, 452)
    for i, l in enumerate(lines):
        d.text((48, 266 + i * 46), l, font=sub, fill=MOSS if i else INK)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def icon():
    """The README logo has transparent rounded corners; Play wants a full square and rounds it itself."""
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA")
    bg = Image.new("RGBA", logo.size, logo.getpixel((logo.width // 2, 40))[:3] + (255,))
    bg.alpha_composite(logo)
    bg.convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()

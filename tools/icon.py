"""Draws the README logo (the launcher icon: a sawn cross-section on kraft) as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling

# (centre x, centre y, radius, colour, ring width or 0 for a filled disc), in the 108-unit icon grid.
PARTS = [
    (54, 54, 31, (0x4A, 0x36, 0x26), 0),
    (54, 54, 27.5, (0xDC, 0xC3, 0x95), 0),
    (53.4, 54.6, 24, (0xE9, 0xD3, 0xA6), 0),
    (53.4, 54.6, 22.5, (0xA8, 0x60, 0x2A), 1.3),
    (53.2, 54.8, 19.5, (0x5F, 0x7D, 0x3C), 1.2),
    (53, 55, 16.8, (0x5F, 0x74, 0x88), 1.2),
    (52.9, 55.1, 14.2, (0x7F, 0x9A, 0x55), 1.1),
    (52.8, 55.2, 11.6, (0xA8, 0x60, 0x2A), 1.1),
    (52.7, 55.3, 9.2, (0x2F, 0x2A, 0x24), 0.9),
    (52.6, 55.4, 6.8, (0x5F, 0x7D, 0x3C), 1.0),
    (52.5, 55.5, 4.4, (0xA8, 0x60, 0x2A), 1.0),
    (52.4, 55.6, 2.2, (0x7A, 0x4F, 0x2D), 0),
]


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), (0xD8, 0xC3, 0x9D, 255))
    d = ImageDraw.Draw(im)
    for cx, cy, r, col, w in PARTS:
        box = [(cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u]
        if w:
            d.ellipse(box, outline=col, width=max(1, int(w * u)))
        else:
            d.ellipse(box, fill=col)
    d.line([(78 * u, 40 * u), (73 * u, 43 * u), (70 * u, 44 * u)], fill=(0x2C, 0x20, 0x17), width=int(1.2 * u))
    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()

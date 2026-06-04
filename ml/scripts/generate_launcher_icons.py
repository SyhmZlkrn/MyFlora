r"""
Build the MyFlora launcher icon set + in-app brand from the source logo PNG.

Source: app/src/main/res/drawable/logo_myflora_source.png (full image — leaf
mark + wordmark on white). We strip the wordmark, crop the leaf mark, and
alpha-mask the white background so the mark floats on whatever surface it lands
on. The cropped mark is then written as:

  app/src/main/res/drawable/logo_myflora.png
        Transparent leaf mark — used by SplashScreen / LoginScreen /
        AboutFloraScreen Image() composables.
  app/src/main/res/mipmap-*/ic_launcher_foreground.webp
        Adaptive icon foreground (mark only, centred in 66dp safe zone).
  app/src/main/res/mipmap-*/ic_launcher.webp
  app/src/main/res/mipmap-*/ic_launcher_round.webp
        Legacy bitmap launcher tiles for API 24-25 — white background under
        the mark, square (rounded corner) and circle variants.
  app/src/main/playstore_icon.png
        512x512 round, for the store listing.

Usage (Windows PowerShell):
  $env:PYTHONPATH=(Resolve-Path .\.pydeps).Path
  $env:PYTHONNOUSERSITE='1'
  python .\ml\scripts\generate_launcher_icons.py
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Below this RGB-average a pixel is treated as fully opaque mark.
OPAQUE_THRESHOLD = 220
# Above this it's pure background — fully transparent.
WHITE_THRESHOLD = 248
# Linear ramp between thresholds gives clean anti-aliased edges.


def find_mark_bbox(src: Image.Image, text_top_hint: float = 0.66) -> tuple[int, int, int, int]:
    """Bounding box of the coloured mark, ignoring the wordmark in the bottom
    third of the source image."""
    w, h = src.size
    top_region = src.crop((0, 0, w, int(h * text_top_hint))).convert("RGBA")
    px = top_region.load()
    min_x, min_y, max_x, max_y = w, h, 0, 0
    for y in range(top_region.height):
        for x in range(top_region.width):
            r, g, b, a = px[x, y]
            if a < 16:
                continue
            if r > 240 and g > 240 and b > 240:
                continue
            if x < min_x: min_x = x
            if y < min_y: min_y = y
            if x > max_x: max_x = x
            if y > max_y: max_y = y
    pad = int(max(top_region.width, top_region.height) * 0.04)
    return (
        max(0, min_x - pad),
        max(0, min_y - pad),
        min(w, max_x + pad),
        min(int(h * text_top_hint), max_y + pad),
    )


def whiten_to_alpha(img: Image.Image) -> Image.Image:
    """Replace white background with transparency. Anti-aliases edges by ramping
    alpha between OPAQUE_THRESHOLD and WHITE_THRESHOLD."""
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            brightness = (r + g + b) / 3
            if brightness >= WHITE_THRESHOLD:
                px[x, y] = (r, g, b, 0)
            elif brightness >= OPAQUE_THRESHOLD:
                t = (brightness - OPAQUE_THRESHOLD) / (WHITE_THRESHOLD - OPAQUE_THRESHOLD)
                px[x, y] = (r, g, b, int(a * (1 - t)))
    return img


def composite_centered(canvas_size: int, mark: Image.Image, fit_fraction: float,
                       bg_color: tuple[int, int, int, int] = (0, 0, 0, 0)) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), bg_color)
    target = int(canvas_size * fit_fraction)
    mw, mh = mark.size
    scale = target / max(mw, mh)
    new_size = (max(1, int(mw * scale)), max(1, int(mh * scale)))
    scaled = mark.resize(new_size, Image.LANCZOS)
    ox = (canvas_size - new_size[0]) // 2
    oy = (canvas_size - new_size[1]) // 2
    canvas.paste(scaled, (ox, oy), scaled)
    return canvas


def make_legacy_square(size: int, mark: Image.Image) -> Image.Image:
    """White tile with leaf mark, rounded corners."""
    base = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    base.alpha_composite(composite_centered(size, mark, 0.78))
    corner = int(size * 0.18)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([(0, 0), (size, size)], radius=corner, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out


def make_legacy_round(size: int, mark: Image.Image) -> Image.Image:
    base = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    base.alpha_composite(composite_centered(size, mark, 0.78))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([(0, 0), (size, size)], fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out


def make_adaptive_foreground(size: int, mark: Image.Image) -> Image.Image:
    # 66/108 ≈ safe zone. Foreground is transparent everywhere except the mark.
    return composite_centered(size, mark, 66 / 108)


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    res = root / "app" / "src" / "main" / "res"
    source = res / "drawable" / "logo_myflora_source.png"
    if not source.exists():
        # Bootstrap: first run uses the in-app drawable as source then archives it.
        legacy = res / "drawable" / "logo_myflora.png"
        if legacy.exists():
            print(f"  bootstrap: copying {legacy.name} -> {source.name}")
            legacy.replace(source)
        else:
            print(f"missing source: {source}")
            return 1

    src = Image.open(source).convert("RGBA")
    bbox = find_mark_bbox(src)
    print(f"  detected mark bbox: {bbox}  src={src.size}")
    mark = whiten_to_alpha(src.crop(bbox))

    # In-app brand: transparent leaf mark.
    in_app = res / "drawable" / "logo_myflora.png"
    mark.save(in_app, "PNG")
    print(f"  wrote in-app drawable -> {in_app}  ({mark.size[0]}x{mark.size[1]})")

    for density, size in DENSITIES.items():
        out_dir = res / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        make_legacy_square(size, mark).save(
            out_dir / "ic_launcher.webp", "WEBP", quality=95, method=6
        )
        make_legacy_round(size, mark).save(
            out_dir / "ic_launcher_round.webp", "WEBP", quality=95, method=6
        )
        fg_px = round(108 * size / 48)
        make_adaptive_foreground(fg_px, mark).save(
            out_dir / "ic_launcher_foreground.webp", "WEBP", quality=95, method=6
        )
        print(f"  wrote {density:>8} legacy {size}x{size} + foreground {fg_px}x{fg_px}")

    play = root / "app" / "src" / "main" / "playstore_icon.png"
    make_legacy_round(512, mark).save(play, "PNG")
    print(f"  wrote playstore_icon.png 512x512 -> {play}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

from PIL import Image, ImageOps


IMAGE_EXTENSIONS = (".jpg", ".jpeg", ".png", ".webp")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert YOLO detection labels into cropped flower classification images."
    )
    parser.add_argument(
        "--source",
        type=Path,
        default=Path("ml/downloads/malaysian-flower-detection-v2"),
        help="Path to the downloaded YOLOv8 dataset.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("ml/generated/classification-dataset"),
        help="Destination for cropped classification images.",
    )
    parser.add_argument(
        "--margin",
        type=float,
        default=0.12,
        help="Extra padding around each bounding box.",
    )
    parser.add_argument(
        "--min-side",
        type=int,
        default=32,
        help="Skip crops smaller than this size.",
    )
    return parser.parse_args()


def find_image(images_dir: Path, stem: str) -> Path | None:
    for extension in IMAGE_EXTENSIONS:
        candidate = images_dir / f"{stem}{extension}"
        if candidate.exists():
            return candidate
    return None


def clamp(value: int, lower: int, upper: int) -> int:
    return max(lower, min(value, upper))


def square_pad(image: Image.Image) -> Image.Image:
    size = max(image.width, image.height)
    canvas = Image.new("RGB", (size, size), color=(255, 255, 255))
    offset_x = (size - image.width) // 2
    offset_y = (size - image.height) // 2
    canvas.paste(image, (offset_x, offset_y))
    return canvas


def load_class_names(dataset_root: Path) -> list[str]:
    data_yaml = dataset_root / "data.yaml"
    names: list[str] = []
    reading_names = False
    for line in data_yaml.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped == "names:":
            reading_names = True
            continue
        if reading_names:
            if not stripped.startswith("- "):
                break
            names.append(stripped[2:].strip())
    if not names:
        raise ValueError(f"Could not read class names from {data_yaml}")
    return names


def main() -> None:
    args = parse_args()
    source_root: Path = args.source
    output_root: Path = args.output
    class_names = load_class_names(source_root)
    summary: dict[str, dict[str, int]] = {}

    if output_root.exists():
        for existing in output_root.glob("*"):
            if existing.is_dir():
                for child in existing.rglob("*"):
                    if child.is_file():
                        child.unlink()

    for split in ("train", "valid", "test"):
        images_dir = source_root / split / "images"
        labels_dir = source_root / split / "labels"
        if not images_dir.exists() or not labels_dir.exists():
            continue

        split_counter: Counter[str] = Counter()
        split_output = output_root / split

        for label_path in sorted(labels_dir.glob("*.txt")):
            image_path = find_image(images_dir, label_path.stem)
            if image_path is None:
                continue

            with Image.open(image_path) as source_image:
                image = ImageOps.exif_transpose(source_image).convert("RGB")
                width, height = image.size
                lines = [
                    line.strip()
                    for line in label_path.read_text(encoding="utf-8").splitlines()
                    if line.strip()
                ]

                for index, row in enumerate(lines):
                    parts = row.split()
                    if len(parts) != 5:
                        continue

                    class_index = int(parts[0])
                    x_center, y_center, box_width, box_height = map(float, parts[1:])
                    label = class_names[class_index]

                    crop_width = box_width * width
                    crop_height = box_height * height
                    margin_x = crop_width * args.margin
                    margin_y = crop_height * args.margin

                    left = clamp(int((x_center * width) - (crop_width / 2) - margin_x), 0, width - 1)
                    top = clamp(int((y_center * height) - (crop_height / 2) - margin_y), 0, height - 1)
                    right = clamp(int((x_center * width) + (crop_width / 2) + margin_x), left + 1, width)
                    bottom = clamp(int((y_center * height) + (crop_height / 2) + margin_y), top + 1, height)

                    crop = image.crop((left, top, right, bottom))
                    if min(crop.size) < args.min_side:
                        continue

                    crop = square_pad(crop)
                    destination_dir = split_output / label
                    destination_dir.mkdir(parents=True, exist_ok=True)
                    destination_file = destination_dir / f"{label_path.stem}_{index:02d}.jpg"
                    crop.save(destination_file, format="JPEG", quality=95)
                    split_counter[label] += 1

        summary[split] = dict(sorted(split_counter.items()))

    output_root.mkdir(parents=True, exist_ok=True)
    (output_root / "summary.json").write_text(
        json.dumps(summary, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()

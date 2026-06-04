"""
Step 4: Build the labels JSON consumed by `LocalFlowerClassifier.kt`.

Output format:
    [
      { "scientific": "Hibiscus rosa-sinensis",
        "common":     "Hibiscus",
        "malay":      "Bunga Raya" },
      ...
    ]

The list is indexed by softmax output position (same order as `class_index.json`
produced in step 2). Common names + Malay names come from:
  1. Pl@ntNet species-id-to-name metadata (when available)
  2. A hand-curated override map for the five flowers Flora cares about most
  3. Fallback: scientific name only
"""

import argparse
import json
from pathlib import Path


# Hand-curated Malaysian species — overrides whatever the upstream metadata says.
# Add more entries here as the team identifies common local flora.
MALAY_OVERRIDES: dict[str, dict[str, str]] = {
    "Hibiscus rosa-sinensis": {"common": "Hibiscus", "malay": "Bunga Raya"},
    "Bougainvillea glabra":   {"common": "Bougainvillea", "malay": "Bunga Kertas"},
    "Bougainvillea spectabilis": {"common": "Bougainvillea", "malay": "Bunga Kertas"},
    "Jasminum sambac":        {"common": "Jasmine", "malay": "Melur"},
    "Plumeria rubra":         {"common": "Frangipani", "malay": "Kemboja"},
    "Plumeria obtusa":        {"common": "Frangipani", "malay": "Kemboja"},
    "Ixora coccinea":         {"common": "Ixora", "malay": "Bunga Siantan"},
    "Tabernaemontana divaricata": {"common": "Crape Jasmine", "malay": "Melur Hutan"},
    "Nelumbo nucifera":       {"common": "Lotus", "malay": "Teratai"},
    "Hylocereus undatus":     {"common": "Dragon Fruit Flower", "malay": "Buah Naga"},
    "Etlingera elatior":      {"common": "Torch Ginger", "malay": "Bunga Kantan"},
}


def find_species_metadata(data_root: Path) -> Path:
    for cand in data_root.rglob("plantnet300K_species_names.json"):
        return cand
    raise FileNotFoundError("plantnet300K_species_id_2_name.json not found under " + str(data_root))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, required=True, help="Dataset root")
    parser.add_argument("--class-index", type=Path, required=False,
                        help="class_index.json from step 2. Defaults to <data>/../output/class_index.json")
    parser.add_argument("--output", type=Path, required=True, help="Destination JSON path")
    args = parser.parse_args()

    meta_path = find_species_metadata(args.data)
    with open(meta_path) as f:
        species_by_id = json.load(f)   # { "class_id": "Scientific name", ... }

    class_index_path = args.class_index or (args.data.parent / "output" / "class_index.json")
    with open(class_index_path) as f:
        class_order = json.load(f)     # [ "class_id", "class_id", ... ] (string ids)

    labels: list[dict[str, str]] = []
    missing = 0
    for cls in class_order:
        scientific = species_by_id.get(cls, "").strip()
        if not scientific:
            missing += 1
            scientific = f"Unknown species ({cls})"

        override = MALAY_OVERRIDES.get(scientific)
        if override:
            entry = {"scientific": scientific, **override}
        else:
            entry = {"scientific": scientific, "common": scientific, "malay": ""}
        labels.append(entry)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(labels, indent=2, ensure_ascii=False))

    print(f"Wrote {len(labels)} labels → {args.output}")
    print(f"Missing scientific names: {missing}")
    print(f"Malay overrides applied:  {sum(1 for x in labels if x['malay'])}")


if __name__ == "__main__":
    main()

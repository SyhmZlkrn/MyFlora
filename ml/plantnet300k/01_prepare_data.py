"""
Step 1: Download and extract the Pl@ntNet-300k dataset from Zenodo.

Record: https://zenodo.org/record/4726653
Size:   ~31 GB compressed, ~40 GB extracted
Layout after extraction:
    data/
        plantnet_300K/
            images/train/<class_id>/*.jpg
            images/val/<class_id>/*.jpg
            images/test/<class_id>/*.jpg
            plantnet300K_species_id_2_name.json
            plantnet300K_metadata.json
"""

import argparse
import hashlib
import json
import os
import shutil
import sys
import tarfile
import zipfile
from pathlib import Path
from urllib.parse import urlparse

import requests
from tqdm import tqdm


ZENODO_RECORD = "4726653"
ZENODO_API = f"https://zenodo.org/api/records/{ZENODO_RECORD}"


def fetch_record_metadata() -> dict:
    print(f"Fetching Zenodo record {ZENODO_RECORD} metadata…")
    r = requests.get(ZENODO_API, timeout=60)
    r.raise_for_status()
    return r.json()


def download_file(url: str, dest: Path, expected_md5: str | None = None) -> None:
    if dest.exists() and expected_md5:
        print(f"{dest.name} already present, verifying checksum…")
        if file_md5(dest) == expected_md5:
            print("  checksum OK, skipping download")
            return
        print("  checksum mismatch, re-downloading")
        dest.unlink()

    print(f"Downloading {url} → {dest}")
    with requests.get(url, stream=True, timeout=300) as r:
        r.raise_for_status()
        total = int(r.headers.get("content-length", 0))
        with open(dest, "wb") as f, tqdm(total=total, unit="B", unit_scale=True) as pbar:
            for chunk in r.iter_content(chunk_size=1 << 20):
                if not chunk:
                    continue
                f.write(chunk)
                pbar.update(len(chunk))


def file_md5(path: Path) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def extract(archive: Path, target: Path) -> None:
    print(f"Extracting {archive.name}…")
    if archive.suffix in (".tar", ".gz", ".tgz") or archive.name.endswith(".tar.gz"):
        with tarfile.open(archive) as tar:
            tar.extractall(target)
    elif archive.suffix == ".zip":
        with zipfile.ZipFile(archive) as zf:
            zf.extractall(target)
    else:
        raise RuntimeError(f"Unknown archive type: {archive.name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True, help="Dataset root directory")
    parser.add_argument("--keep-archive", action="store_true", help="Keep downloaded archive after extraction")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    archive_dir = args.output / "archives"
    archive_dir.mkdir(exist_ok=True)

    record = fetch_record_metadata()
    files = record.get("files", [])
    if not files:
        print("No files listed on Zenodo record. Aborting.")
        sys.exit(1)

    for f in files:
        url = f["links"]["self"]
        name = f["key"]
        md5 = f.get("checksum", "").replace("md5:", "") or None
        dest = archive_dir / name
        download_file(url, dest, expected_md5=md5)
        extract(dest, args.output)
        if not args.keep_archive:
            dest.unlink()

    # Sanity check
    images_root = next(args.output.rglob("images_train"), None) or \
                  next(args.output.rglob("images/train"), None)
    if images_root is None:
        print("WARNING: Could not locate images/train directory. Inspect extraction output.")
    else:
        n_classes = sum(1 for p in images_root.iterdir() if p.is_dir())
        print(f"Found {n_classes} class directories under {images_root}")

    meta = next(args.output.rglob("plantnet300K_species_id_2_name.json"), None)
    if meta:
        with open(meta) as f:
            species = json.load(f)
        print(f"Species metadata loaded: {len(species)} entries")


if __name__ == "__main__":
    main()

from __future__ import annotations

from pathlib import Path

from roboflow import Roboflow


WORKSPACE = "my-workspace-rnskj"
PROJECT = "malaysian-flower-detection"
VERSION = 2
FORMAT = "yolov8"
OUTPUT = "ml/downloads/malaysian-flower-detection-v2"


def load_api_key() -> str:
    for line in Path("local.properties").read_text(encoding="utf-8").splitlines():
        if line.startswith("ROBOFLOW_API_KEY="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError("ROBOFLOW_API_KEY was not found in local.properties")


def main() -> None:
    api_key = load_api_key()
    rf = Roboflow(api_key=api_key)
    project = rf.workspace(WORKSPACE).project(PROJECT)
    project.version(VERSION).download(FORMAT, location=OUTPUT, overwrite=True)
    print(f"Downloaded {WORKSPACE}/{PROJECT}/{VERSION} to {OUTPUT}")


if __name__ == "__main__":
    main()

r"""
Convert the AlexNet PlantNet checkpoint (.pth) shipped by the user into a
float32 .tflite asset usable by Plantnet300kClassifier.

Pipeline:
    1. Build torchvision AlexNet(num_classes=1081)
    2. Load state_dict (strip `module.` DataParallel prefix)
    3. Wrap model so its forward returns softmax probs
    4. Export to ONNX with input shape (1, 3, 224, 224)
    5. Run onnx2tf to produce TFLite (auto NCHW->NHWC transpose)
    6. Copy result to app/src/main/assets/models/plantnet300k.tflite

Usage (Windows PowerShell):
    $env:PYTHONPATH=(Resolve-Path .\.pydeps).Path
    $env:PYTHONNOUSERSITE='1'
    python .\ml\scripts\convert_alexnet_pth_to_tflite.py `
        --pth "C:\Users\muhds\Downloads\best_model.pth"
"""
from __future__ import annotations

import argparse
import os
import shutil
import sys
from collections import OrderedDict
from pathlib import Path


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Convert AlexNet .pth to .tflite")
    p.add_argument("--pth", required=True, type=Path, help="Path to best_model.pth")
    p.add_argument("--num-classes", type=int, default=1081)
    p.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/models/plantnet300k.tflite"),
        help="Final asset destination",
    )
    p.add_argument(
        "--workdir",
        type=Path,
        default=Path("ml/generated/alexnet_convert"),
        help="Where ONNX + intermediate TFLite live",
    )
    return p.parse_args()


def load_alexnet(pth_path: Path, num_classes: int):
    """Load torchvision AlexNet and apply the user's state_dict."""
    import torch
    from torchvision.models import alexnet

    model = alexnet(weights=None, num_classes=num_classes)
    raw = torch.load(pth_path, map_location="cpu", weights_only=False)

    # Accept either a raw state_dict or a checkpoint dict that wraps one.
    state_dict = raw
    if isinstance(raw, dict) and not any(k.endswith(".weight") for k in raw.keys()):
        for key in ("state_dict", "model_state_dict", "model", "net"):
            if key in raw and isinstance(raw[key], dict):
                state_dict = raw[key]
                print(f"  unwrapped checkpoint['{key}']")
                break

    # Strip `module.` prefix from DataParallel-trained checkpoints.
    cleaned = OrderedDict()
    for k, v in state_dict.items():
        new_k = k[len("module."):] if k.startswith("module.") else k
        cleaned[new_k] = v

    missing, unexpected = model.load_state_dict(cleaned, strict=False)
    if missing:
        print(f"  WARN missing keys ({len(missing)}): {missing[:5]}…")
    if unexpected:
        print(f"  WARN unexpected keys ({len(unexpected)}): {unexpected[:5]}…")
    if not missing and not unexpected:
        print("  state_dict loaded cleanly")
    model.eval()
    return model


class WithSoftmax(object):
    """Wrap a model so forward() returns softmax probabilities, matching the
    expectation in Plantnet300kClassifier (which does not apply softmax itself)."""

    def __new__(cls, base):
        import torch.nn as nn
        import torch.nn.functional as F

        class _Wrapped(nn.Module):
            def __init__(self, m):
                super().__init__()
                self.m = m

            def forward(self, x):
                logits = self.m(x)
                return F.softmax(logits, dim=1)

        return _Wrapped(base)


def export_onnx(model, onnx_path: Path):
    import torch

    onnx_path.parent.mkdir(parents=True, exist_ok=True)
    dummy = torch.randn(1, 3, 224, 224)
    print(f"  exporting ONNX -> {onnx_path}")
    torch.onnx.export(
        model,
        dummy,
        str(onnx_path),
        input_names=["input"],
        output_names=["probs"],
        dynamic_axes=None,
        opset_version=17,
        do_constant_folding=True,
    )


def onnx_to_tflite(onnx_path: Path, out_dir: Path) -> Path:
    """Run onnx2tf and return the path of the resulting fp32 TFLite file."""
    import onnx2tf

    out_dir.mkdir(parents=True, exist_ok=True)
    print(f"  running onnx2tf -> {out_dir}")
    onnx2tf.convert(
        input_onnx_file_path=str(onnx_path),
        output_folder_path=str(out_dir),
        copy_onnx_input_output_names_to_tflite=True,
        non_verbose=True,
    )
    # onnx2tf emits several variants (float16, dynamic_range, integer_quant).
    # We want the plain float32 file. Naming convention: <stem>_float32.tflite
    candidates = list(out_dir.glob("*_float32.tflite"))
    if not candidates:
        # Fallback: pick first .tflite produced.
        candidates = list(out_dir.glob("*.tflite"))
    if not candidates:
        raise RuntimeError(f"onnx2tf produced no .tflite in {out_dir}")
    return candidates[0]


def main() -> int:
    args = parse_args()
    if not args.pth.exists():
        print(f"pth not found: {args.pth}", file=sys.stderr)
        return 1

    print("[1/4] Loading AlexNet + state_dict")
    base = load_alexnet(args.pth, args.num_classes)
    model = WithSoftmax(base)

    print("[2/4] Exporting ONNX")
    onnx_path = args.workdir / "alexnet_softmax.onnx"
    export_onnx(model, onnx_path)

    print("[3/4] ONNX -> TFLite via onnx2tf")
    tflite_dir = args.workdir / "tflite"
    if tflite_dir.exists():
        shutil.rmtree(tflite_dir)
    produced = onnx_to_tflite(onnx_path, tflite_dir)
    print(f"  produced: {produced}  ({produced.stat().st_size / 1e6:.1f} MB)")

    print("[4/4] Copying to app assets")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(produced, args.output)
    print(f"  installed -> {args.output}  ({args.output.stat().st_size / 1e6:.1f} MB)")
    print("done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

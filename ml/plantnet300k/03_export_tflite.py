"""
Step 3: Export a Keras checkpoint to TFLite with INT8 post-training quantization.

Two files are produced:
  flora_flower_classifier_fp32.tflite  — fallback, ~30 MB
  flora_flower_classifier.tflite       — INT8, ~10 MB (use this in the app)

INT8 quantization needs a representative dataset — we stream 200 random
training images through the network and collect activation stats.
"""

import argparse
import random
from pathlib import Path

import numpy as np
import tensorflow as tf
from tensorflow import keras


IMAGE_SIZE = 224
REPRESENTATIVE_SAMPLES = 200


def find_train_dir(data_root: Path) -> Path:
    for cand in [
        data_root / "plantnet_300K" / "images" / "train",
        data_root / "plantnet_300K" / "images_train",
        data_root / "images" / "train",
        data_root / "images_train",
    ]:
        if cand.is_dir():
            return cand
    raise FileNotFoundError("train split not found under " + str(data_root))


def representative_dataset_gen(train_dir: Path):
    all_jpegs = list(train_dir.rglob("*.jpg"))
    random.shuffle(all_jpegs)
    for path in all_jpegs[:REPRESENTATIVE_SAMPLES]:
        img = tf.io.read_file(str(path))
        img = tf.image.decode_jpeg(img, channels=3)
        img = tf.image.resize(img, (IMAGE_SIZE, IMAGE_SIZE))
        img = tf.cast(img, tf.float32)
        yield [tf.expand_dims(img, 0)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", type=Path, required=True, help="Path to best.keras")
    parser.add_argument("--data", type=Path, required=True, help="Dataset root (for representative samples)")
    parser.add_argument("--output", type=Path, required=True, help="Output directory")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    model = keras.models.load_model(args.checkpoint)

    # --- FP32 TFLite (fallback) ---
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    fp32_model = converter.convert()
    fp32_path = args.output / "flora_flower_classifier_fp32.tflite"
    fp32_path.write_bytes(fp32_model)
    print(f"FP32 model: {fp32_path}  ({len(fp32_model) / 1024 / 1024:.1f} MB)")

    # --- INT8 TFLite (primary deployment target) ---
    train_dir = find_train_dir(args.data)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = lambda: representative_dataset_gen(train_dir)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.float32   # keep float input, internal ops quantized
    converter.inference_output_type = tf.float32
    int8_model = converter.convert()
    int8_path = args.output / "flora_flower_classifier.tflite"
    int8_path.write_bytes(int8_model)
    print(f"INT8 model: {int8_path}  ({len(int8_model) / 1024 / 1024:.1f} MB)")


if __name__ == "__main__":
    main()

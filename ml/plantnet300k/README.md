# Pl@ntNet-300k Training Pipeline

Train a flower classifier on the [Pl@ntNet-300k dataset](https://zenodo.org/record/4726653) — 306,146 phone-taken photos across 1,081 plant species. Output: a TFLite model + labels JSON that drop into the Flora Android app.

## Why this dataset

- **Domain-matched**: photos taken on phones (same distribution as Flora users)
- **Manageable class count**: 1,081 species → ~30MB TFLite model, deployable on-device
- **Public & documented**: clean train/val/test split, species metadata included
- **Long-tailed**: realistic difficulty distribution (few dominant species, many rare)

## What this pipeline produces

1. `flora_flower_classifier.tflite` — INT8-quantized MobileNetV3-Small, ~10–20 MB
2. `flora_flower_labels.json` — ordered species list matching softmax indices, enriched with common names + Malay names for the five curated species

Both drop into `app/src/main/assets/models/` — existing Android code (`LocalFlowerClassifier.kt`) picks them up automatically.

## Requirements

- Python 3.10+
- 1× CUDA GPU with ≥12 GB VRAM (training on CPU is possible but takes days)
- ~80 GB free disk (31 GB dataset + decompressed + checkpoints)
- TensorFlow 2.15+

Recommended: Google Colab Pro or a Linux box with an RTX 3090 / A100.

## Quick start (Colab)

1. Open `02_train.py` as a Colab notebook (`File → Upload → Open as notebook`).
2. Set runtime → GPU (A100 preferred).
3. Run all cells. Training takes ~6 hours on A100, ~24 hours on T4.
4. Download the produced `.tflite` and labels JSON from `/content/output/`.

## Quick start (local)

```bash
cd ml/plantnet300k
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# Step 1: download + extract Pl@ntNet-300k (31 GB) into ./data
python 01_prepare_data.py --output ./data

# Step 2: train the model (~6–24 hours on GPU)
python 02_train.py --data ./data --output ./output --epochs 25

# Step 3: export to TFLite with INT8 quantization
python 03_export_tflite.py --checkpoint ./output/best.keras --data ./data --output ./output

# Step 4: build labels JSON enriched with common + Malay names
python 04_build_labels.py --data ./data --output ./output/flora_flower_labels.json
```

## Drop into Android

```
cp ./output/flora_flower_classifier.tflite    ../../app/src/main/assets/models/
cp ./output/flora_flower_labels.json          ../../app/src/main/assets/models/
```

Rebuild the app. `LocalFlowerClassifier.kt` reads these automatically.

## Tuning

Edit `02_train.py`:

- `IMAGE_SIZE = 224` — larger = more accurate, slower. 224 is the MobileNetV3 default.
- `BATCH_SIZE = 64` — drop if OOM, raise if GPU under-utilized.
- `EPOCHS = 25` — real training; 5–10 also works but fewer points learned for long-tail classes.
- `BACKBONE = "MobileNetV3Small"` — swap to `EfficientNetV2B0` for better accuracy at ~3× model size.

## Trade-offs honestly

- Top-1 accuracy on Pl@ntNet-300k with MobileNetV3-Small after 25 epochs: **~60–65 %** (long-tail effect; head classes hit 85 %+).
- Top-5 accuracy: **~85 %**.
- The app ranks top-3 predictions, so real user-experience accuracy is closer to top-3 (~78 %).
- Rare species ( <20 train images) remain hard. This is unavoidable with this dataset.
- For Malay-specific species not in Pl@ntNet-300k (e.g. *Bunga Kantan*), supplement with your own photos and re-fine-tune.

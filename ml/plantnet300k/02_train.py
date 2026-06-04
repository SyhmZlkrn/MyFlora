"""
Step 2: Fine-tune MobileNetV3-Small on Pl@ntNet-300k.

Runs on:
  - Kaggle TPU v5e-8  (8 cores, bfloat16)           <- primary target
  - Colab / Kaggle A100 / P100 / T4 GPU             (mixed_float16)
  - Local single GPU                                (mixed_float16)
  - CPU                                             (debug only, very slow)

Auto-detects accelerator at startup. No flag needed.

Two phases:
  Phase 1 - backbone frozen, train new head (PHASE1_EPOCHS).
  Phase 2 - unfreeze top FINE_TUNE_LAYERS, fine-tune with cosine-decayed LR + linear warmup.

Production extras:
  * Mixed precision (bfloat16 on TPU, float16 on GPU) - ~2x throughput, ~0.5x memory.
  * XLA compile on TPU is automatic; on GPU enabled via jit_compile=True.
  * steps_per_execution tuned per device (TPU=128, GPU=32) to amortize host overhead.
  * Cosine LR w/ linear warmup via keras.optimizers.schedules.CosineDecay.
  * AdamW + global grad clipping.
  * Per-class inverse-frequency weights (--balance) to counter long-tail distribution.
  * Resumable training (--resume) via last.keras + training_log.json reconstruction.
  * --quick 5-epoch smoke mode for local sanity-check before burning cloud hours.

Outputs (under --output):
  best.keras           -> highest val_top1 checkpoint
  last.keras           -> latest epoch checkpoint (for resume)
  class_index.json     -> softmax index -> class folder name
  class_weights.json   -> computed per-class weights (if --balance)
  training_log.json    -> epoch-by-epoch metrics across all phases/runs
  training_log.csv     -> same, CSV format for plotting

Example invocations:

  # Local smoke test (5 epochs, single GPU or CPU)
  python 02_train.py --data /data/plantnet --output ./runs/smoke --quick

  # Kaggle TPU v5e-8 full run
  python 02_train.py --data /kaggle/input/plantnet300k --output /kaggle/working --epochs 100 --balance

  # Colab A100 full run
  python 02_train.py --data /content/plantnet --output /content/drive/MyDrive/flora_runs/a100 --epochs 100 --balance

  # Resume after Kaggle session timeout
  python 02_train.py --data /kaggle/input/plantnet300k --output /kaggle/working --epochs 100 --balance --resume
"""

import argparse
import json
import math
import os
from collections import Counter
from pathlib import Path

import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.applications import MobileNetV3Small


# ---- Hyperparameters --------------------------------------------------------
IMAGE_SIZE = 224
DEFAULT_EPOCHS = 100
PHASE1_EPOCHS = 3
FINE_TUNE_LAYERS = 80
LR_PHASE1 = 1e-3
LR_PHASE2_PEAK = 5e-4
WARMUP_EPOCHS = 2
WEIGHT_DECAY = 1e-4
LABEL_SMOOTHING = 0.05
GRAD_CLIP_NORM = 1.0
DROPOUT = 0.3

# Per-device batch-size defaults. Global batch = per-core * num_replicas.
#   TPU v5e has 16GB HBM per core -> 128 fits MobileNetV3-Small @ 224 easily.
#   A100 40GB -> 256.  P100/T4 16GB -> 96.  Local 8-12GB -> 64.
PER_REPLICA_BATCH = {
    "TPU": 128,
    "GPU": 128,        # tweak down to 96 for T4/P100 if OOM, up to 256 for A100
    "CPU": 32,
}

# steps_per_execution amortizes the Python/TF boundary. Bigger = faster on fast
# accelerators, but too big hurts first-epoch startup and checkpoint granularity.
STEPS_PER_EXECUTION = {
    "TPU": 128,
    "GPU": 32,
    "CPU": 1,
}
# -----------------------------------------------------------------------------


# ---- Accelerator setup ------------------------------------------------------
def configure_accelerators():
    """Returns (strategy, device_type: 'TPU'|'GPU'|'CPU', num_replicas)."""
    # 1. Try TPU first (Kaggle / Colab TPU runtimes).
    tpu_resolver = None
    try:
        tpu_resolver = tf.distribute.cluster_resolver.TPUClusterResolver()
    except (ValueError, tf.errors.NotFoundError):
        tpu_resolver = None

    if tpu_resolver is not None:
        tf.config.experimental_connect_to_cluster(tpu_resolver)
        tf.tpu.experimental.initialize_tpu_system(tpu_resolver)
        strategy = tf.distribute.TPUStrategy(tpu_resolver)
        keras.mixed_precision.set_global_policy("mixed_bfloat16")
        print(f"Accelerator: TPU ({strategy.num_replicas_in_sync} cores, bfloat16)")
        return strategy, "TPU", strategy.num_replicas_in_sync

    # 2. Fall back to GPU.
    gpus = tf.config.list_physical_devices("GPU")
    for g in gpus:
        try:
            tf.config.experimental.set_memory_growth(g, True)
        except Exception as err:
            print(f"  memory_growth failed on {g.name}: {err}")

    if gpus:
        keras.mixed_precision.set_global_policy("mixed_float16")
        if len(gpus) > 1:
            strategy = tf.distribute.MirroredStrategy()
            print(f"Accelerator: {len(gpus)} GPUs (MirroredStrategy, mixed_float16)")
        else:
            strategy = tf.distribute.get_strategy()
            print("Accelerator: 1 GPU (mixed_float16)")
        return strategy, "GPU", strategy.num_replicas_in_sync

    # 3. CPU fallback.
    strategy = tf.distribute.get_strategy()
    print("Accelerator: CPU (fp32, very slow — use for debug only)")
    return strategy, "CPU", 1


# ---- Data ------------------------------------------------------------------
def find_split_dir(data_root: Path, split: str) -> Path:
    """Pl@ntNet-300k has used two different layouts across releases. Handle both."""
    candidates = [
        data_root / "plantnet_300K" / "images" / split,
        data_root / "plantnet_300K" / f"images_{split}",
        data_root / "images" / split,
        data_root / f"images_{split}",
    ]
    for c in candidates:
        if c.is_dir():
            return c
    raise FileNotFoundError(f"Could not find {split} split under {data_root}")


def build_augmentation() -> keras.Sequential:
    return keras.Sequential(
        [
            layers.RandomFlip("horizontal"),
            layers.RandomRotation(0.10),
            layers.RandomZoom(0.10),
            layers.RandomTranslation(0.05, 0.05),
            layers.RandomContrast(0.10),
            layers.RandomBrightness(0.10),
        ],
        name="augmentation",
    )


def make_dataset(directory: Path, shuffle: bool, batch_size: int,
                 augment: keras.Sequential | None, device_type: str):
    """tf.data pipeline tuned for throughput.

    - drop_remainder=True: TPU + XLA need static shapes.
    - cache() val only: train set is 300k images, too big for RAM. Val ~50k fits.
    - prefetch(AUTOTUNE): keep accelerator fed while CPU decodes next batch.
    - non-deterministic option: small additional throughput win.
    """
    ds = keras.utils.image_dataset_from_directory(
        directory,
        labels="inferred",
        label_mode="int",
        image_size=(IMAGE_SIZE, IMAGE_SIZE),
        batch_size=None,                    # batch manually below with drop_remainder
        shuffle=shuffle,
        interpolation="bilinear",
    )
    class_names = ds.class_names

    opts = tf.data.Options()
    opts.experimental_deterministic = False
    ds = ds.with_options(opts)

    AUTOTUNE = tf.data.AUTOTUNE

    if augment is not None:
        ds = ds.map(lambda x, y: (augment(x, training=True), y), num_parallel_calls=AUTOTUNE)

    if shuffle:
        ds = ds.shuffle(buffer_size=min(10_000, batch_size * 32), reshuffle_each_iteration=True)

    ds = ds.batch(batch_size, drop_remainder=True, num_parallel_calls=AUTOTUNE)

    if not shuffle:
        # Val set fits in RAM; cache to skip re-decoding every epoch.
        # On TPU, cache lives on the VM host, which is fine.
        ds = ds.cache()

    ds = ds.prefetch(AUTOTUNE)
    return ds, class_names


def compute_class_weights(train_dir: Path, class_names: list[str]) -> dict[int, float]:
    """Inverse-frequency weighting to rebalance long-tail classes."""
    counts = Counter()
    for idx, name in enumerate(class_names):
        folder = train_dir / name
        counts[idx] = sum(1 for _ in folder.iterdir()) if folder.is_dir() else 0
    n_samples = sum(counts.values())
    n_classes = len(class_names)
    return {
        idx: (n_samples / (n_classes * c)) if c > 0 else 0.0
        for idx, c in counts.items()
    }


# ---- Model -----------------------------------------------------------------
def build_model(num_classes: int) -> keras.Model:
    backbone = MobileNetV3Small(
        input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3),
        include_top=False,
        weights="imagenet",
        include_preprocessing=True,   # [-1, 1] scaling internally — matches Android inference path
    )
    backbone.trainable = False

    inputs = keras.Input(shape=(IMAGE_SIZE, IMAGE_SIZE, 3), dtype=tf.float32)
    x = backbone(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(DROPOUT)(x)
    # Force softmax fp32 under mixed precision for numerical stability.
    outputs = layers.Dense(num_classes, activation="softmax", dtype="float32", name="predictions")(x)
    return keras.Model(inputs, outputs, name="plantnet_classifier")


def compile_model(model: keras.Model, lr_schedule, device_type: str,
                  *, clip_norm: float | None = GRAD_CLIP_NORM):
    optimizer = keras.optimizers.AdamW(
        learning_rate=lr_schedule,
        weight_decay=WEIGHT_DECAY,
        global_clipnorm=clip_norm,
    )

    # Sparse + label smoothing requires one-hot detour.
    def loss_fn(y_true, y_pred):
        y_true_oh = tf.one_hot(tf.cast(y_true, tf.int32), depth=tf.shape(y_pred)[-1])
        return keras.losses.categorical_crossentropy(
            y_true_oh, y_pred, label_smoothing=LABEL_SMOOTHING
        )

    # jit_compile: TPU already XLA-compiles, so don't double up. GPU benefits.
    jit = (device_type == "GPU")

    model.compile(
        optimizer=optimizer,
        loss=loss_fn,
        metrics=[
            keras.metrics.SparseCategoricalAccuracy(name="top1"),
            keras.metrics.SparseTopKCategoricalAccuracy(k=5, name="top5"),
        ],
        jit_compile=jit,
        steps_per_execution=STEPS_PER_EXECUTION[device_type],
    )


def cosine_warmup_schedule(peak_lr: float, total_steps: int, warmup_steps: int):
    """Linear warmup from 0 -> peak_lr over warmup_steps, then cosine decay to 0.

    Uses the built-in CosineDecay (TF 2.13+) so it serializes cleanly via SavedModel.
    """
    return keras.optimizers.schedules.CosineDecay(
        initial_learning_rate=0.0,
        decay_steps=max(1, total_steps - warmup_steps),
        alpha=0.0,
        warmup_target=peak_lr,
        warmup_steps=max(1, warmup_steps),
        name="cosine_warmup",
    )


# ---- Main ------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, required=True, help="Dataset root (output of step 1)")
    parser.add_argument("--output", type=Path, required=True, help="Where to save checkpoints and logs")
    parser.add_argument("--epochs", type=int, default=DEFAULT_EPOCHS, help="Total epochs (phase1 + phase2)")
    parser.add_argument("--batch-size", type=int, default=None,
                        help="Per-replica batch size. Default auto-picks per device: "
                             f"{PER_REPLICA_BATCH}")
    parser.add_argument("--phase", type=str, default="both", choices=["1", "2", "both"],
                        help="Which phase to run: 1 (head-only), 2 (fine-tune), or both (default)")
    parser.add_argument("--quick", action="store_true",
                        help="Smoke test: 5 total epochs, for local sanity-check before cloud runs.")
    parser.add_argument("--balance", action="store_true",
                        help="Apply inverse-frequency class weights (helps with long-tail distribution).")
    parser.add_argument("--resume", action="store_true",
                        help="Resume from last.keras if it exists. Epochs already completed are skipped.")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)

    if args.quick:
        global PHASE1_EPOCHS
        PHASE1_EPOCHS = 1
        args.epochs = 5
        print("--quick mode: phase1=1 epoch, total=5 epochs")

    # ---- Accelerator --------------------------------------------------------
    strategy, device_type, num_replicas = configure_accelerators()
    per_replica_bs = args.batch_size or PER_REPLICA_BATCH[device_type]
    global_batch = per_replica_bs * num_replicas
    print(f"Per-replica batch: {per_replica_bs} | Replicas: {num_replicas} | Global batch: {global_batch}")

    # ---- Data ---------------------------------------------------------------
    train_dir = find_split_dir(args.data, "train")
    val_dir = find_split_dir(args.data, "val")
    print(f"Train dir: {train_dir}")
    print(f"Val dir:   {val_dir}")

    aug = build_augmentation()
    train_ds, class_names = make_dataset(train_dir, shuffle=True, batch_size=global_batch,
                                         augment=aug, device_type=device_type)
    val_ds, _ = make_dataset(val_dir, shuffle=False, batch_size=global_batch,
                             augment=None, device_type=device_type)

    num_classes = len(class_names)
    print(f"Classes: {num_classes}")
    (args.output / "class_index.json").write_text(json.dumps(class_names, indent=2))

    class_weight = None
    if args.balance:
        if device_type == "TPU":
            print("NOTE: class_weight on TPU works in TF 2.15+ but adds a small per-step cost.")
        print("Computing class weights ...")
        weights_path = args.output / "class_weights.json"
        if weights_path.exists():
            class_weight = {int(k): float(v) for k, v in json.loads(weights_path.read_text()).items()}
        else:
            class_weight = compute_class_weights(train_dir, class_names)
            weights_path.write_text(json.dumps(class_weight, indent=2))
        print(f"Class-weight range: [{min(class_weight.values()):.3f}, {max(class_weight.values()):.3f}]")

    # Cardinality for LR schedule
    steps_per_epoch = tf.data.experimental.cardinality(train_ds).numpy()
    if steps_per_epoch <= 0:
        steps_per_epoch = sum(1 for _ in train_dir.rglob("*.jpg")) // global_batch
    print(f"Steps per epoch: {steps_per_epoch}")

    # ---- Callbacks ----------------------------------------------------------
    best_path = args.output / "best.keras"
    last_path = args.output / "last.keras"

    callbacks = [
        keras.callbacks.ModelCheckpoint(str(best_path), monitor="val_top1", mode="max",
                                        save_best_only=True, verbose=1),
        keras.callbacks.ModelCheckpoint(str(last_path), save_best_only=False, verbose=0),
        keras.callbacks.CSVLogger(str(args.output / "training_log.csv"), append=True),
        keras.callbacks.EarlyStopping(monitor="val_top1", mode="max", patience=10,
                                      restore_best_weights=True),
        keras.callbacks.TerminateOnNaN(),
    ]

    hist1 = None
    hist2 = None

    # ---- Resume -------------------------------------------------------------
    resumed_epoch = 0
    if args.resume and last_path.exists():
        print(f"Resuming from {last_path}")
        with strategy.scope():
            model = keras.models.load_model(last_path, compile=False)
        log_path = args.output / "training_log.json"
        if log_path.exists():
            existing = json.loads(log_path.read_text())
            done = 0
            for phase in ("phase1", "phase2"):
                if phase in existing and "loss" in existing[phase]:
                    done += len(existing[phase]["loss"])
            resumed_epoch = done
            print(f"  resumed at epoch {resumed_epoch}")

    # ---- Phase 1: head-only warmup ------------------------------------------
    if args.phase in ("1", "both") and resumed_epoch < PHASE1_EPOCHS:
        with strategy.scope():
            if resumed_epoch == 0:
                model = build_model(num_classes)
            compile_model(model, lr_schedule=LR_PHASE1, device_type=device_type)

        model.summary(line_length=110)
        print(f"\n=== Phase 1: head-only warmup (epochs {resumed_epoch} -> {PHASE1_EPOCHS}) ===")
        hist1 = model.fit(
            train_ds,
            validation_data=val_ds,
            initial_epoch=resumed_epoch,
            epochs=PHASE1_EPOCHS,
            callbacks=callbacks,
            class_weight=class_weight,
        )
        resumed_epoch = PHASE1_EPOCHS

    # ---- Phase 2: fine-tune backbone ----------------------------------------
    if args.phase in ("2", "both") and resumed_epoch < args.epochs:
        if args.phase == "2" and resumed_epoch == 0:
            assert best_path.exists() or last_path.exists(), \
                "No checkpoint found. Run --phase 1 first."
            src = last_path if last_path.exists() else best_path
            print(f"\nLoading checkpoint from {src} ...")
            with strategy.scope():
                model = keras.models.load_model(src, compile=False)

        print(f"\n=== Phase 2: fine-tune top {FINE_TUNE_LAYERS} backbone layers "
              f"(epochs {resumed_epoch} -> {args.epochs}) ===")

        with strategy.scope():
            backbone = next(l for l in model.layers if isinstance(l, keras.Model))
            backbone.trainable = True
            for layer in backbone.layers[:-FINE_TUNE_LAYERS]:
                layer.trainable = False

            p2_epochs = args.epochs - PHASE1_EPOCHS
            lr_sched = cosine_warmup_schedule(
                peak_lr=LR_PHASE2_PEAK,
                total_steps=p2_epochs * steps_per_epoch,
                warmup_steps=WARMUP_EPOCHS * steps_per_epoch,
            )
            compile_model(model, lr_schedule=lr_sched, device_type=device_type)

        hist2 = model.fit(
            train_ds,
            validation_data=val_ds,
            initial_epoch=resumed_epoch,
            epochs=args.epochs,
            callbacks=callbacks,
            class_weight=class_weight,
        )

    # ---- Merge and persist training log -------------------------------------
    log_path = args.output / "training_log.json"
    log = {}
    if log_path.exists():
        log = json.loads(log_path.read_text())
    if hist1 is not None:
        log.setdefault("phase1", {})
        for k, v in hist1.history.items():
            log["phase1"].setdefault(k, []).extend(float(x) for x in v)
    if hist2 is not None:
        log.setdefault("phase2", {})
        for k, v in hist2.history.items():
            log["phase2"].setdefault(k, []).extend(float(x) for x in v)
    log_path.write_text(json.dumps(log, indent=2))

    print("\nDone. Best checkpoint:", best_path)
    print("     Latest checkpoint:", last_path)


if __name__ == "__main__":
    main()

from __future__ import annotations

import argparse
import json
import random
from pathlib import Path

import numpy as np
import tensorflow as tf


SEED = 42


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a MobileNetV2 flower classifier and export it as TensorFlow Lite."
    )
    parser.add_argument(
        "--data",
        type=Path,
        default=Path("ml/generated/classification-dataset"),
        help="Path to the cropped classification dataset.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("ml/models"),
        help="Directory for trained models and metadata.",
    )
    parser.add_argument("--image-size", type=int, default=224)
    parser.add_argument("--batch-size", type=int, default=24)
    parser.add_argument("--epochs", type=int, default=10)
    parser.add_argument("--fine-tune-epochs", type=int, default=4)
    return parser.parse_args()


def seed_everything() -> None:
    random.seed(SEED)
    np.random.seed(SEED)
    tf.random.set_seed(SEED)


def make_dataset(directory: Path, image_size: int, batch_size: int, shuffle: bool) -> tf.data.Dataset:
    return tf.keras.utils.image_dataset_from_directory(
        directory,
        labels="inferred",
        label_mode="int",
        image_size=(image_size, image_size),
        batch_size=batch_size,
        shuffle=shuffle,
        seed=SEED,
    )


def main() -> None:
    args = parse_args()
    seed_everything()

    train_dir = args.data / "train"
    valid_dir = args.data / "valid"
    test_dir = args.data / "test"
    output_dir = args.output
    output_dir.mkdir(parents=True, exist_ok=True)

    autotune = tf.data.AUTOTUNE
    train_ds = make_dataset(train_dir, args.image_size, args.batch_size, shuffle=True)
    valid_ds = make_dataset(valid_dir, args.image_size, args.batch_size, shuffle=False)
    test_ds = make_dataset(test_dir, args.image_size, args.batch_size, shuffle=False)

    class_names = train_ds.class_names
    num_classes = len(class_names)

    train_ds = train_ds.prefetch(autotune)
    valid_ds = valid_ds.prefetch(autotune)
    test_ds = test_ds.prefetch(autotune)

    augmentation = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.08),
            tf.keras.layers.RandomZoom(0.12),
            tf.keras.layers.RandomContrast(0.1),
        ],
        name="augmentation",
    )

    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(args.image_size, args.image_size, 3),
        include_top=False,
        weights="imagenet",
    )
    base_model.trainable = False

    inputs = tf.keras.Input(shape=(args.image_size, args.image_size, 3))
    x = augmentation(inputs)
    x = tf.keras.applications.mobilenet_v2.preprocess_input(x)
    x = base_model(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    model = tf.keras.Model(inputs, outputs)

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_accuracy",
            patience=4,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=2,
            min_lr=1e-6,
        ),
        tf.keras.callbacks.ModelCheckpoint(
            filepath=str(output_dir / "flower_classifier_best.keras"),
            monitor="val_accuracy",
            save_best_only=True,
        ),
    ]

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    model.fit(
        train_ds,
        validation_data=valid_ds,
        epochs=args.epochs,
        callbacks=callbacks,
        verbose=2,
    )

    base_model.trainable = True
    for layer in base_model.layers[:-30]:
        layer.trainable = False
    for layer in base_model.layers:
        if isinstance(layer, tf.keras.layers.BatchNormalization):
            layer.trainable = False

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    model.fit(
        train_ds,
        validation_data=valid_ds,
        epochs=args.epochs + args.fine_tune_epochs,
        initial_epoch=args.epochs,
        callbacks=callbacks,
        verbose=2,
    )

    best_model = tf.keras.models.load_model(output_dir / "flower_classifier_best.keras")
    test_loss, test_accuracy = best_model.evaluate(test_ds, verbose=0)

    labels_path = output_dir / "flora_flower_labels.json"
    labels_path.write_text(json.dumps(class_names, indent=2), encoding="utf-8")

    metadata = {
        "image_size": args.image_size,
        "labels": class_names,
        "test_loss": float(test_loss),
        "test_accuracy": float(test_accuracy),
    }
    (output_dir / "flower_classifier_metrics.json").write_text(
        json.dumps(metadata, indent=2),
        encoding="utf-8",
    )

    converter = tf.lite.TFLiteConverter.from_keras_model(best_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    (output_dir / "flora_flower_classifier.tflite").write_bytes(tflite_model)

    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()

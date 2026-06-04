Roboflow TFLite model — DROP HERE
==================================

Required files:
  roboflow_flowers.tflite  (the exported model)
  roboflow_labels.txt      (one class name per line, matching training order)

How to get them:
  1. Open your Roboflow project:
     https://universe.roboflow.com/my-workspace-rnskj/malaysian-flower-detection
  2. Go to the "Deploy" or "Versions" tab → pick your trained version.
  3. Click "Download Dataset" or "Download Model" → format: TFLite
     (TFLite export produces a model with embedded metadata — works out of
      the box with the TFLite Task Vision ObjectDetector used by FloraApp.)
  4. Unzip the download. You'll find something like:
       detect.tflite        → rename to roboflow_flowers.tflite
       labelmap.txt         → rename to roboflow_labels.txt
  5. Copy both into this folder.

Notes:
  - Model MUST be an object-detection TFLite with metadata (SSD-MobileNet,
    EfficientDet-Lite, or Roboflow's standard detect export).
  - Pure YOLO TFLite exports without metadata will NOT work with the Task
    Vision API — use Roboflow's built-in TFLite export, not "YOLOv5/v8 tflite".
  - If you only have a classification model, rename the file to
    roboflow_flowers.tflite anyway — the code will log a clear error
    prompting you to swap in a detection model.

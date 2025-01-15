# Flower Classification Android App

## Overview

**FlowerClassification** is an Android app that uses the CameraX API to capture real-time video, analyzes flower images, and classifies them with a pretrained TensorFlow Lite model. The app provides the top three predicted flower labels along with their confidence scores, offering users a reliable classification result.
The model was fine-tuned on a flower dataset using TFLite Model Maker. For more info about TFLite Model Maker, [official GitHub repository](https://github.com/tensorflow/examples/tree/master/tensorflow_examples/lite/model_maker).
**Please note:** The model was not trained on all possible flower species, so it may only recognize a limited set of flower types. Predictions for flowers outside this trained dataset may not be accurate.
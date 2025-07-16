import tensorflow as tf
import numpy as np
import cv2
import os

# ✅ Load the original model
converter = tf.lite.TFLiteConverter.from_saved_model("your_model_dir")  # OR from_keras_model(...)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# ✅ Representative dataset from registration faces
image_dir = "faces/"  # your directory
def preprocess_image(path):
    img = cv2.imread(path)
    img = cv2.resize(img, (160, 160))  # matches model input
    img = img.astype(np.float32) / 255.0  # normalize
    return np.expand_dims(img, axis=0)

def representative_data_gen():
    image_files = os.listdir(image_dir)
    for i, file in enumerate(image_files[:100]):
        image = preprocess_image(os.path.join(image_dir, file))
        yield [image]

# 🔧 INT8 quantization with float output
converter.representative_dataset = representative_data_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.int8
converter.inference_output_type = tf.float32  # keep embeddings precise

# 🚀 Convert and save
tflite_model = converter.convert()
with open("model_quant_input_int8.tflite", "wb") as f:
    f.write(tflite_model)

print("✅ Quantized model saved as model_quant_input_int8.tflite")

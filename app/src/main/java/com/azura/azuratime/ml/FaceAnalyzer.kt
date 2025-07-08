package com.azura.azuratime.ml

import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import com.azura.azuratime.ml.FaceRecognizer
import com.azura.azuratime.ml.BitmapUtils

@Suppress("UnsafeOptInUsageError")
class FaceAnalyzer (
    private val onFaceEmbedding: (Rect, FloatArray, IntSize) -> Unit // <-- Add IntSize for image size
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            // .setMinFaceSize(0.15f) // Uncomment to ignore tiny faces
            .enableTracking()
            .build()
    )

    // Compose‑observable face boxes
    var faceBounds by mutableStateOf<List<Rect>>(emptyList())
        private set

    // Raw image dimensions before rotation
    var imageSize by mutableStateOf(IntSize(0, 0))
        private set

    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L

    override fun analyze(imageProxy: ImageProxy) {
        // Skip frames if processing is taking too long (throttle to ~10 FPS)
        val currentTime = System.currentTimeMillis()
        if (isProcessing.get() || (currentTime - lastProcessTime < 100)) {
            imageProxy.close()
            return
        }
        isProcessing.set(true)
        lastProcessTime = currentTime

        val mediaImage: Image? = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            // record raw dims (swap for 90/270°)
            imageSize = if (rotation % 180 == 0)
                IntSize(imageProxy.width, imageProxy.height)
            else
                IntSize(imageProxy.height, imageProxy.width)

            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    faceBounds = faces.map { it.boundingBox }
                    for (face in faces) {
                        // Crop + preprocess + infer embedding
                        val floatArray = BitmapUtils.preprocessFace(
                            image       = mediaImage,
                            boundingBox = face.boundingBox,
                            rotation    = rotation
                        )
                        val embedding = FaceRecognizer.recognizeFace(floatArray)
                        onFaceEmbedding(face.boundingBox, embedding, imageSize) // <-- Pass imageSize
                    }
                    Log.d("FaceAnalyzer", "Detected ${faces.size} faces")
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Detection failed", e)
                    faceBounds = emptyList()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing.set(false)
                }
        } else {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    fun close() {
        detector.close()
    }
}
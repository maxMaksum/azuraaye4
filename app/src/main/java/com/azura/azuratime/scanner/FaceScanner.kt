package com.azura.azuratime.scanner

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.azura.azuratime.ml.FaceAnalyzer

import com.azura.azuratime.ui.FaceOverlay
import com.azura.azuratime.ui.PermissionsHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import com.google.accompanist.permissions.isGranted


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceScanner(
    useBackCamera: Boolean = false,
    onFaceEmbedding: (Rect, FloatArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // State for face bounds and image size
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf<androidx.compose.ui.unit.IntSize?>(null) }

    PermissionsHandler(permissionState = cameraPermissionState) {
        val executor by remember { mutableStateOf(Executors.newSingleThreadExecutor()) }
        DisposableEffect(Unit) { onDispose { executor.shutdown() } }

        val analyzer = remember(onFaceEmbedding) {
            FaceAnalyzer { rect, embedding, imgSize ->
                faceBounds = if (rect != null) listOf(rect) else emptyList()
                imageSize = imgSize
                onFaceEmbedding(rect, embedding)
            }
        }

        val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

        LaunchedEffect(cameraPermissionState.status, useBackCamera) {
            if (cameraPermissionState.status.isGranted) {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(executor, analyzer)
                    }
                val selector = if (useBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }
        }

        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (faceBounds.isNotEmpty() && imageSize != null) {
                FaceOverlay(
                    faceBounds = faceBounds,
                    imageSize = imageSize!!,
                    isFrontCamera = !useBackCamera,
                    paddingFactor = 0.1f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


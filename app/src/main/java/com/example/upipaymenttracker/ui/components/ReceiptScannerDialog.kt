package com.example.upipaymenttracker.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.upipaymenttracker.TransactionModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ReceiptScannerDialog(
    onDismiss: () -> Unit,
    onResult: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isProcessing) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                imageCapture = ImageCapture.Builder().build()
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    Log.e("SCANNER", "Use case binding failed", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // UI Controls
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

                Button(
                    onClick = {
                        isProcessing = true
                        takePhoto(context, imageCapture!!, cameraExecutor) { bitmap ->
                            processImage(bitmap) { amount, merchant ->
                                onResult(amount, merchant)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.padding(32.dp).align(Alignment.BottomCenter)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Receipt")
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            image.close()
            onPhotoCaptured(bitmap)
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("SCANNER", "Capture failed: ${exception.message}", exception)
        }
    })
}

private fun processImage(bitmap: Bitmap, onResult: (String, String) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val fullText = visionText.text
            Log.d("SCANNER", "Recognized Text: $fullText")

            // Simple Logic to find Amount and Merchant
            val lines = fullText.split("\n")
            val amountRegex = "(?i)(?:Total|Amount|INR|Rs)\\.?\\s?([0-9,]+(?:\\.[0-9]{2})?)".toRegex()
            
            var amount = ""
            var merchant = lines.firstOrNull() ?: "General Expense"

            // Try to find total amount
            for (line in lines) {
                val match = amountRegex.find(line)
                if (match != null) {
                    amount = match.groupValues[1]
                }
            }
            
            // If no amount found with labels, look for the largest number (common for total)
            if (amount.isEmpty()) {
                val numberRegex = "([0-9]+(?:\\.[0-9]{2})?)".toRegex()
                val numbers = numberRegex.findAll(fullText).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
                if (numbers.isNotEmpty()) {
                    amount = numbers.maxOrNull().toString()
                }
            }

            onResult(amount, merchant)
        }
        .addOnFailureListener { e ->
            Log.e("SCANNER", "Text recognition failed", e)
            onResult("", "Unknown")
        }
}
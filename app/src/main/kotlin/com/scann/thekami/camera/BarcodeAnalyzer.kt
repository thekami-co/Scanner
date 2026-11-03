package com.scann.thekami.camera

import android.graphics.Rect
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.scann.thekami.model.BarcodeParser
import com.scann.thekami.model.ScanResult

/**
 * Where a barcode was found, in the coordinates of the upright camera frame
 * ([imageWidth] x [imageHeight]) so the UI can map it onto the preview.
 */
data class DetectedBox(
    val rect: Rect,
    val imageWidth: Int,
    val imageHeight: Int
)

/** Analyses at most ~8 frames per second to keep CPU use low on weak devices. */
private const val MinFrameIntervalMs = 120L

class BarcodeAnalyzer(
    private val onResult: (ScanResult, DetectedBox?) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @Volatile
    private var isProcessing = false

    private var lastAnalyzedAt = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val now = SystemClock.elapsedRealtime()

        if (mediaImage == null || isProcessing || now - lastAnalyzedAt < MinFrameIntervalMs) {
            imageProxy.close()
            return
        }

        lastAnalyzedAt = now
        isProcessing = true
        val rotation = imageProxy.imageInfo.rotationDegrees
        val swapped = rotation == 90 || rotation == 270
        val imageWidth = if (swapped) imageProxy.height else imageProxy.width
        val imageHeight = if (swapped) imageProxy.width else imageProxy.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val firstValid = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (firstValid != null) {
                    val box = firstValid.boundingBox?.let { DetectedBox(it, imageWidth, imageHeight) }
                    onResult(BarcodeParser.parse(firstValid), box)
                }
            }
            .addOnFailureListener {
                // Silently ignore — next frame will retry
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    fun close() {
        scanner.close()
    }
}

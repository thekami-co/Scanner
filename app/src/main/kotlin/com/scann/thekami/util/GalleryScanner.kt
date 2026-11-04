package com.scann.thekami.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.scann.thekami.model.BarcodeParser
import com.scann.thekami.model.ScanResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object GalleryScanner {

    suspend fun scanImage(context: Context, uri: Uri): ScanResult? = try {
        val inputImage = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, uri)
        }
        suspendCancellableCoroutine { continuation ->
            val scanner = BarcodeScanning.getClient()
            val closed = AtomicBoolean(false)
            fun closeScanner() {
                if (closed.compareAndSet(false, true)) scanner.close()
            }
            continuation.invokeOnCancellation { closeScanner() }
            try {
                scanner.process(inputImage).addOnCompleteListener { task ->
                    try {
                        val result = if (task.isSuccessful) {
                            task.result.firstOrNull { !it.rawValue.isNullOrBlank() }
                                ?.let { BarcodeParser.parse(it) }
                        } else {
                            null
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    } finally {
                        closeScanner()
                    }
                }
            } catch (e: Exception) {
                closeScanner()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

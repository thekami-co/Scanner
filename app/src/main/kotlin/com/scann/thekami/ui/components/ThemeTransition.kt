package com.scann.thekami.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot
import kotlin.math.max

private const val RevealDurationMs = 450

/**
 * Telegram-style theme switch: the old screen is frozen as a snapshot, the new theme is applied
 * underneath it, and a circle grows from the tap point to reveal the new theme.
 */
@Stable
class ThemeTransitionState {
    internal var snapshot by mutableStateOf<ImageBitmap?>(null)
    internal var origin by mutableStateOf(Offset.Zero)
    internal val radius = Animatable(0f)

    suspend fun play(view: View, origin: Offset, change: () -> Unit) {
        if (snapshot != null) {
            change()
            return
        }
        val bitmap = captureWindow(view)
        if (bitmap == null) {
            change()
            return
        }
        this.origin = origin
        radius.snapTo(0f)
        snapshot = bitmap.asImageBitmap()
        try {
            change()
            // Two frames so the new theme is drawn beneath the snapshot before it is revealed.
            withFrameNanos { }
            withFrameNanos { }
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()
            val maxRadius = hypot(max(origin.x, width - origin.x), max(origin.y, height - origin.y))
            radius.animateTo(maxRadius, tween(RevealDurationMs, easing = FastOutSlowInEasing))
        } finally {
            snapshot = null
        }
    }
}

@Composable
fun rememberThemeTransitionState(): ThemeTransitionState = remember { ThemeTransitionState() }

@Composable
fun ThemeTransitionOverlay(state: ThemeTransitionState, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot ?: return
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            // Swallow touches while the animation runs
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
    ) {
        drawImage(snapshot)
        drawCircle(
            color = Color.Black,
            radius = state.radius.value,
            center = state.origin,
            blendMode = BlendMode.Clear
        )
    }
}

private suspend fun captureWindow(view: View): Bitmap? {
    val window = view.context.findActivity()?.window ?: return null
    val decor = window.decorView
    if (decor.width == 0 || decor.height == 0) return null
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val copied = suspendCancellableCoroutine { continuation ->
                PixelCopy.request(
                    window,
                    bitmap,
                    { result -> if (continuation.isActive) continuation.resume(result == PixelCopy.SUCCESS) },
                    Handler(Looper.getMainLooper())
                )
            }
            if (copied) bitmap else null
        } else {
            decor.drawToBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

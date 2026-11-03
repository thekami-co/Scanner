package com.scann.thekami.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scann.thekami.R
import com.scann.thekami.camera.BarcodeAnalyzer
import com.scann.thekami.camera.DetectedBox
import com.scann.thekami.model.ScanResult
import com.scann.thekami.ui.components.LocalAnimationsEnabled
import com.scann.thekami.ui.components.bottomNavHeight
import com.scann.thekami.util.GalleryScanner
import com.scann.thekami.util.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Camera UI is always dark, whatever the app theme, so overlays use a fixed scrim
private val ScanScrim = Color.Black.copy(alpha = 0.5f)
private val ScanFrameSize = 260.dp
private val DetectedPadding = 14.dp
private val DetectedMinSide = 120.dp
private val SuccessBadgeSize = 64.dp

private const val TabAnimationMs = 220L

@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onBarcodeScanned: (ScanResult) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val animations = LocalAnimationsEnabled.current
    val coroutineScope = rememberCoroutineScope()
    var pendingScanJob by remember { mutableStateOf<Job?>(null) }
    var galleryScanJob by remember { mutableStateOf<Job?>(null) }
    val isActiveState by rememberUpdatedState(isActive)
    val animationsState by rememberUpdatedState(animations)
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    fun checkCameraPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(checkCameraPermission()) }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var boundLens by remember { mutableIntStateOf(-1) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasMultipleCameras by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }
    var showSuccessFlash by remember { mutableStateOf(false) }
    var detectedRect by remember { mutableStateOf<Rect?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var isProcessingGalleryImage by remember { mutableStateOf(false) }
    var galleryScanFailed by remember { mutableStateOf(false) }

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    // COMPATIBLE (TextureView) so the tab fade animation also fades the camera
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Leaving Scan invalidates both the confirmation delay and any in-flight gallery scan.
    LaunchedEffect(isActive) {
        if (isActive) {
            hasScanned = false
            showSuccessFlash = false
            detectedRect = null
        } else {
            pendingScanJob?.cancel()
            galleryScanJob?.cancel()
            isProcessingGalleryImage = false
            galleryScanFailed = false
        }
    }

    LaunchedEffect(camera) {
        zoomRatio = 1f
    }

    LaunchedEffect(zoomRatio) {
        if (abs(zoomRatio - 1f) > 0.01f) {
            showZoomIndicator = true
            delay(1200)
        }
        showZoomIndicator = false
    }

    val transformState = rememberTransformableState { zoomChange, _, _ ->
        val boundCamera = camera
        val zoomInfo = boundCamera?.cameraInfo?.zoomState?.value
        if (boundCamera != null && zoomInfo != null) {
            val ratio = (zoomRatio * zoomChange).coerceIn(zoomInfo.minZoomRatio, zoomInfo.maxZoomRatio)
            boundCamera.cameraControl.setZoomRatio(ratio)
            zoomRatio = ratio
        }
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraPermanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    // Snaps the frame onto the code (when we know where it is), confirms with haptics,
    // waits briefly so the user sees it, then navigates to the result.
    fun confirmScan(result: ScanResult, box: DetectedBox? = null) {
        if (!isActiveState || hasScanned ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) return
        hasScanned = true
        showSuccessFlash = true
        detectedRect = box?.toViewRect(viewSize, mirrored = lensFacing == CameraSelector.LENS_FACING_FRONT)
        Haptics.confirm(view)
        pendingScanJob = coroutineScope.launch {
            delay(if (animationsState) 500 else 120)
            if (isActiveState && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                currentOnBarcodeScanned(result)
            }
        }
    }

    // One analyzer (and one ML Kit client) for the lifetime of the screen, reused on every bind.
    val analyzer = remember {
        BarcodeAnalyzer { result, box ->
            if (isActiveState && !hasScanned && !isProcessingGalleryImage) {
                confirmScan(result, box)
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null || !isActiveState || hasScanned || !isProcessingGalleryImage) {
            isProcessingGalleryImage = false
        } else {
            galleryScanFailed = false
            galleryScanJob = coroutineScope.launch {
                val result = GalleryScanner.scanImage(context, uri)
                isProcessingGalleryImage = false
                if (!isActiveState || hasScanned) return@launch
                if (result != null) {
                    confirmScan(result)
                } else {
                    galleryScanFailed = true
                    delay(2200)
                    galleryScanFailed = false
                }
            }
        }
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Pick the permission up again if the user granted it from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> hasCameraPermission = checkCameraPermission()
                Lifecycle.Event.ON_STOP -> {
                    if (pendingScanJob?.isActive == true) {
                        pendingScanJob?.cancel()
                        hasScanned = false
                        showSuccessFlash = false
                        detectedRect = null
                    }
                    if (galleryScanJob?.isActive == true) {
                        galleryScanJob?.cancel()
                        isProcessingGalleryImage = false
                        galleryScanFailed = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingScanJob?.cancel()
            galleryScanJob?.cancel()
            cameraExecutor.shutdown()
            analyzer.close()
        }
    }

    // The camera is bound only while the scan screen is in use and released on other tabs
    // and overlays. Binding and releasing wait for the tab animation so they don't cause jank.
    LaunchedEffect(hasCameraPermission, isActive, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect

        if (isActive != (camera != null) && animationsState) delay(TabAnimationMs)

        val cameraProvider = try {
            context.awaitCameraProvider()
        } catch (e: Exception) {
            return@LaunchedEffect
        }

        if (!isActive) {
            cameraProvider.unbindAll()
            camera = null
            boundLens = -1
            isFlashOn = false
            return@LaunchedEffect
        }
        if (camera != null && boundLens == lensFacing) return@LaunchedEffect

        val hasBack = cameraProvider.hasCameraSafe(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFront = cameraProvider.hasCameraSafe(CameraSelector.DEFAULT_FRONT_CAMERA)
        hasMultipleCameras = hasBack && hasFront

        val wantsFront = lensFacing == CameraSelector.LENS_FACING_FRONT
        if ((wantsFront && !hasFront) || (!wantsFront && !hasBack)) {
            if (wantsFront && hasBack) lensFacing = CameraSelector.LENS_FACING_BACK
            if (!wantsFront && hasFront) lensFacing = CameraSelector.LENS_FACING_FRONT
            return@LaunchedEffect
        }

        isFlashOn = false

        // Same 16:9 aspect for preview and analysis, so a point on the analysed
        // frame maps onto the preview without distortion. Analysis runs at a lower
        // resolution, which is plenty for codes and much lighter on weak devices.
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector(1920, 1080))
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            boundLens = lensFacing
        } catch (e: Exception) {
            // Camera bind failed
        }
    }

    val fadeInSpec = if (animations) fadeIn() else EnterTransition.None
    val fadeOutSpec = if (animations) fadeOut() else ExitTransition.None

    Box(modifier = modifier.background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewSize = it }
            )

            // The preview runs behind the bottom nav; controls and the frame are laid out
            // in the area above it. Both share the preview's top-left origin, so detected
            // positions can be used here directly.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = bottomNavHeight())
                    .transformable(state = transformState)
            ) {
                ScanOverlay(
                    detectedRect = detectedRect,
                    success = showSuccessFlash,
                    animations = animations,
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = showZoomIndicator,
                    enter = fadeInSpec,
                    exit = fadeOutSpec,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 34.dp)
                ) {
                    ZoomIndicator(ratio = zoomRatio)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasMultipleCameras) {
                        CircleIconButton(
                            iconRes = R.drawable.hgi_switch_camera,
                            contentDescription = "Switch camera",
                            onClick = {
                                Haptics.tick(view)
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        )
                    }
                    if (camera?.cameraInfo?.hasFlashUnit() == true) {
                        CircleIconButton(
                            iconRes = if (isFlashOn) R.drawable.hgi_flash_on else R.drawable.hgi_flash_off,
                            contentDescription = if (isFlashOn) "Turn flash off" else "Turn flash on",
                            active = isFlashOn,
                            onClick = {
                                isFlashOn = !isFlashOn
                                camera?.cameraControl?.enableTorch(isFlashOn)
                            }
                        )
                    }
                }
            }
        } else {
            CameraPermissionPrompt(
                permanentlyDenied = cameraPermanentlyDenied,
                onAction = {
                    if (cameraPermanentlyDenied) {
                        openAppSettings()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasCameraPermission) ScanHint()
            AnimatedVisibility(
                visible = galleryScanFailed,
                enter = fadeInSpec,
                exit = fadeOutSpec,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "No QR code or barcode found in this image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
        CircleIconButton(
            iconRes = R.drawable.hgi_gallery,
            contentDescription = "Scan from gallery",
            loading = isProcessingGalleryImage,
            onClick = {
                if (isActiveState && !hasScanned && !isProcessingGalleryImage) {
                    isProcessingGalleryImage = true
                    galleryScanFailed = false
                    galleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = bottomNavHeight() + 24.dp)
        )
    }
}

private fun resolutionSelector(width: Int, height: Int): ResolutionSelector =
    ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(width, height),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

private fun ProcessCameraProvider.hasCameraSafe(selector: CameraSelector): Boolean =
    try {
        hasCamera(selector)
    } catch (e: CameraInfoUnavailableException) {
        false
    }

/**
 * Maps a box from the analysed frame onto a FILL_CENTER preview of [viewSize].
 * The front camera preview is mirrored but the analysed frame is not, so [mirrored] flips x.
 */
private fun DetectedBox.toViewRect(viewSize: IntSize, mirrored: Boolean): Rect? {
    if (viewSize == IntSize.Zero || imageWidth == 0 || imageHeight == 0) return null

    val scale = max(viewSize.width / imageWidth.toFloat(), viewSize.height / imageHeight.toFloat())
    val offsetX = (viewSize.width - imageWidth * scale) / 2f
    val offsetY = (viewSize.height - imageHeight * scale) / 2f

    val srcLeft = if (mirrored) imageWidth - rect.right else rect.left
    val srcRight = if (mirrored) imageWidth - rect.left else rect.right

    return Rect(
        left = srcLeft * scale + offsetX,
        top = rect.top * scale + offsetY,
        right = srcRight * scale + offsetX,
        bottom = rect.bottom * scale + offsetY
    )
}
private fun Rect.coerceMinSide(minSide: Float): Rect {
    val newWidth = max(width, minSide)
    val newHeight = max(height, minSide)
    return Rect(
        left = center.x - newWidth / 2f,
        top = center.y - newHeight / 2f,
        right = center.x + newWidth / 2f,
        bottom = center.y + newHeight / 2f
    )
}

@Composable
private fun ScanHint() {
    Surface(
        shape = CircleShape,
        color = ScanScrim,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.hgi_scan),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Point at a QR code or barcode",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ZoomIndicator(ratio: Float) {
    Surface(
        shape = CircleShape,
        color = ScanScrim,
        contentColor = Color.White
    ) {
        Text(
            text = String.format(Locale.US, "%.1f×", ratio),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Corner brackets that rest in the centre of the screen and, once a code is detected,
 * spring onto it. A check badge confirms the scan.
 */
@Composable
private fun ScanOverlay(
    detectedRect: Rect?,
    success: Boolean,
    animations: Boolean,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { size = it }) {
        if (size != IntSize.Zero) {
            val defaultRect = Rect(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = with(density) { ScanFrameSize.toPx() } / 2f
            )
            val target = detectedRect
                ?.inflate(with(density) { DetectedPadding.toPx() })
                ?.coerceMinSide(with(density) { DetectedMinSide.toPx() })
                ?: defaultRect

            val frame by animateRectAsState(
                targetValue = target,
                animationSpec = if (animations) {
                    spring<Rect>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                } else {
                    snap<Rect>()
                }
            )
            val frameColor by animateColorAsState(
                targetValue = if (success) MaterialTheme.colorScheme.tertiary else Color.White,
                label = "scanFrameColor"
            )
            val badgePx = with(density) { SuccessBadgeSize.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCornerBrackets(frame, frameColor)
            }

            AnimatedVisibility(
                visible = success,
                enter = if (animations) scaleIn() + fadeIn() else EnterTransition.None,
                exit = if (animations) scaleOut() + fadeOut() else ExitTransition.None,
                modifier = Modifier.offset {
                    IntOffset(
                        (frame.center.x - badgePx / 2f).roundToInt(),
                        (frame.center.y - badgePx / 2f).roundToInt()
                    )
                }
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(SuccessBadgeSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.hgi_check),
                            contentDescription = "Scan successful",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}


private fun DrawScope.drawCornerBrackets(frame: Rect, color: Color) {
    val strokeWidth = 4.dp.toPx()
    val shortSide = min(frame.width, frame.height)
    val arm = min(36.dp.toPx(), shortSide * 0.3f)
    val radius = min(20.dp.toPx(), arm * 0.6f)
    val left = frame.left + strokeWidth / 2
    val top = frame.top + strokeWidth / 2
    val right = frame.right - strokeWidth / 2
    val bottom = frame.bottom - strokeWidth / 2

    val corners = Path().apply {
        // Top-left
        moveTo(left, top + arm)
        lineTo(left, top + radius)
        arcTo(Rect(left, top, left + 2 * radius, top + 2 * radius), 180f, 90f, false)
        lineTo(left + arm, top)
        // Top-right
        moveTo(right - arm, top)
        lineTo(right - radius, top)
        arcTo(Rect(right - 2 * radius, top, right, top + 2 * radius), 270f, 90f, false)
        lineTo(right, top + arm)
        // Bottom-right
        moveTo(right, bottom - arm)
        lineTo(right, bottom - radius)
        arcTo(Rect(right - 2 * radius, bottom - 2 * radius, right, bottom), 0f, 90f, false)
        lineTo(right - arm, bottom)
        // Bottom-left
        moveTo(left + arm, bottom)
        lineTo(left + radius, bottom)
        arcTo(Rect(left, bottom - 2 * radius, left + 2 * radius, bottom), 90f, 90f, false)
        lineTo(left, bottom - arm)
    }

    drawPath(
        path = corners,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Composable
private fun CircleIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    active: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (active) colorScheme.primaryContainer else ScanScrim
    val contentColor = if (active) colorScheme.onPrimaryContainer else Color.White

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .size(56.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    permanentlyDenied: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = bottomNavHeight())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.hgi_scan),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera access needed",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scann needs the camera to scan QR codes and barcodes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (permanentlyDenied) "Open settings" else "Grant permission")
        }
    }
}

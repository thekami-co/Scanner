package com.scann.thekami

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.scann.thekami.model.ScanAction
import com.scann.thekami.model.ScanResult
import com.scann.thekami.ui.components.BottomNavBar
import com.scann.thekami.ui.components.LocalAnimationsEnabled
import com.scann.thekami.ui.components.LocalNavBarStyle
import com.scann.thekami.ui.components.NavBarStyle
import com.scann.thekami.ui.components.NavTab
import com.scann.thekami.ui.components.rememberSystemAnimationsDisabled
import com.scann.thekami.ui.screens.AboutScreen
import com.scann.thekami.ui.screens.GeneratorScreen
import com.scann.thekami.ui.screens.ResultScreen
import com.scann.thekami.ui.screens.ScannerScreen
import com.scann.thekami.ui.screens.SettingsScreen
import com.scann.thekami.ui.theme.ScannTheme
import com.scann.thekami.util.AppPreferences
import com.scann.thekami.util.Haptics
import com.scann.thekami.util.NotificationController
import com.scann.thekami.util.ScanActionHandler
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {

    private var pendingDeepLinkTab by mutableStateOf<NavTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLinkTab = resolveDeepLinkTab(intent)
        setContent {
            ScannApp(
                initialTab = pendingDeepLinkTab,
                onDeepLinkConsumed = { pendingDeepLinkTab = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkTab = resolveDeepLinkTab(intent)
    }

    private fun resolveDeepLinkTab(intent: Intent?): NavTab? {
        val uri = intent?.data ?: return null
        return when (uri.host) {
            "scan" -> NavTab.SCAN
            "create" -> NavTab.CREATE
            else -> null
        }
    }
}

private sealed class Overlay {
    object None : Overlay()
    object About : Overlay()
    data class Result(val scanResult: ScanResult) : Overlay()
}

private const val BackScaleDelta = 0.1f
private const val BackCornerRadius = 28f

@Composable
fun ScannApp(
    initialTab: NavTab? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedTab by remember { mutableStateOf(initialTab ?: NavTab.SCAN) }
    var visitedTabs by remember { mutableStateOf(setOf(initialTab ?: NavTab.SCAN)) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var settings by remember { mutableStateOf(AppPreferences.load(context)) }

    val systemAnimationsOff = rememberSystemAnimationsDisabled()
    val animationsEnabled = settings.animations && !systemAnimationsOff
    val navStyle = if (settings.floatingNav) NavBarStyle.FLOATING else NavBarStyle.DOCKED
    val isDark = isSystemInDarkTheme()
    val onCamera = selectedTab == NavTab.SCAN && overlay is Overlay.None
    LaunchedEffect(selectedTab) { visitedTabs = visitedTabs + selectedTab }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            selectedTab = initialTab
            overlay = Overlay.None
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(overlay) {
        val current = overlay
        if (current is Overlay.Result) lastResult = current.scanResult
        if (current !is Overlay.None) backProgress = 0f
    }

    PredictiveBackHandler(enabled = overlay !is Overlay.None) { progress ->
        try {
            progress.collect { event -> backProgress = event.progress }
            overlay = Overlay.None
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    val overlayEnter = if (animationsEnabled) {
        fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 16 }
    } else {
        EnterTransition.None
    }
    val overlayExit = if (animationsEnabled) {
        fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 16 }
    } else {
        ExitTransition.None
    }

    // The camera feed is dark, so system bar icons stay light there. A docked nav bar
    // sits on a themed surface, so only a floating bar leaves the camera under the nav area.
    ScannTheme(
        dynamicColor = settings.dynamicColor,
        darkStatusBarIcons = !isDark && !onCamera,
        darkNavBarIcons = !isDark && !(onCamera && settings.floatingNav)
    ) {
        CompositionLocalProvider(
            LocalNavBarStyle provides navStyle,
            LocalAnimationsEnabled provides animationsEnabled
        ) {
            LaunchedEffect(Unit) {
                if (NotificationController.isCreateNotificationEnabled(context)) {
                    NotificationController.showCreateNotification(context)
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Keep CameraX bound while navigating so returning to Scan is instant.
                    TabLayer(visible = selectedTab == NavTab.SCAN, composed = selectedTab == NavTab.SCAN || NavTab.SCAN in visitedTabs) {
                        ScannerScreen(
                            isActive = selectedTab == NavTab.SCAN && overlay is Overlay.None,
                            modifier = Modifier.fillMaxSize(),
                            onBarcodeScanned = { result ->
                                overlay = Overlay.Result(result)
                            }
                        )
                    }
                    TabLayer(visible = selectedTab == NavTab.CREATE, composed = selectedTab == NavTab.CREATE || NavTab.CREATE in visitedTabs) {
                        GeneratorScreen()
                    }
                    TabLayer(visible = selectedTab == NavTab.SETTINGS, composed = selectedTab == NavTab.SETTINGS || NavTab.SETTINGS in visitedTabs) {
                        SettingsScreen(
                            onAboutClick = { overlay = Overlay.About },
                            settings = settings,
                            onSettingsChange = { updated ->
                                settings = updated
                                AppPreferences.save(context, updated)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = overlay is Overlay.None,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f),
                        enter = overlayEnter,
                        exit = overlayExit
                    ) {
                        BottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab ->
                                if (tab != selectedTab) {
                                    Haptics.tick(view)
                                    selectedTab = tab
                                }
                            },
                            style = navStyle
                        )
                    }

                    AnimatedVisibility(
                        visible = overlay is Overlay.Result,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f),
                        enter = overlayEnter,
                        exit = overlayExit
                    ) {
                        val result = (overlay as? Overlay.Result)?.scanResult ?: lastResult
                        if (result != null) {
                            OverlayLayer(backProgress = { backProgress }) {
                                ResultScreen(
                                    scanResult = result,
                                    onBack = { overlay = Overlay.None },
                                    onAction = { action: ScanAction ->
                                        ScanActionHandler.handle(context, action)
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = overlay is Overlay.About,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f),
                        enter = overlayEnter,
                        exit = overlayExit
                    ) {
                        OverlayLayer(backProgress = { backProgress }) {
                            AboutScreen(onBack = { overlay = Overlay.None })
                        }
                    }
                }
            }
        }
    }
}

/** Full-screen overlay that shrinks and rounds its corners while the user swipes back. */
@Composable
private fun OverlayLayer(
    backProgress: () -> Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val progress = backProgress()
                val scale = 1f - BackScaleDelta * progress
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape((BackCornerRadius * progress).dp)
                clip = progress > 0f
            }
    ) {
        content()
    }
}

/**
 * One of the main tabs. Hidden tabs stay composed (so their state survives) but are invisible.
 * Only the tab being shown fades in, so at most one layer is blended per frame.
 */
@Composable
private fun TabLayer(
    visible: Boolean,
    composed: Boolean,
    content: @Composable () -> Unit
) {
    val animations = LocalAnimationsEnabled.current
    val alpha = remember { Animatable(if (visible) 1f else 0f) }

    LaunchedEffect(visible, animations) {
        when {
            !visible -> alpha.snapTo(0f)
            animations -> alpha.animateTo(1f, tween(durationMillis = 160))
            else -> alpha.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 0f else -1f)
            .graphicsLayer { this.alpha = alpha.value }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
    ) {
        if (composed) content()
    }
}

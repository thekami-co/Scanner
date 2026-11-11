package com.scann.thekami

import android.content.Intent
import android.app.Activity
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import com.scann.thekami.util.AppSettings
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.scann.thekami.model.ScanAction
import com.scann.thekami.model.ScanResult
import com.scann.thekami.model.ScanResultType
import com.scann.thekami.ui.components.BottomNavBar
import com.scann.thekami.ui.components.LocalAnimationsEnabled
import com.scann.thekami.ui.components.LocalNavBarStyle
import com.scann.thekami.ui.components.NavBarStyle
import com.scann.thekami.ui.components.NavTab
import com.scann.thekami.ui.components.rememberSystemAnimationsDisabled
import com.scann.thekami.ui.components.RatingPromptDialog
import com.scann.thekami.ui.screens.AboutScreen
import com.scann.thekami.ui.screens.ThemeScreen
import com.scann.thekami.ui.screens.GeneratorScreen
import com.scann.thekami.ui.screens.ResultScreen
import com.scann.thekami.ui.screens.ScannerScreen
import com.scann.thekami.ui.screens.SettingsScreen
import com.scann.thekami.ui.theme.ScannTheme
import com.scann.thekami.util.AdManager
import com.scann.thekami.util.AppPreferences
import com.scann.thekami.util.Haptics
import com.scann.thekami.util.NotificationController
import com.scann.thekami.util.ReviewHelper
import com.scann.thekami.util.ScanActionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var pendingDeepLinkTab by mutableStateOf<NavTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) AdManager.start(this)
        pendingDeepLinkTab = if (savedInstanceState == null) resolveDeepLinkTab(intent) else null
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
    object Theme : Overlay()
    data class Result(val scanResult: ScanResult) : Overlay()
}

private val visitedTabsSaver = Saver<Set<NavTab>, String>(
    save = { tabs -> tabs.joinToString(",") { it.name } },
    restore = { names -> names.split(",").map(NavTab::valueOf).toSet() }
)

private val overlaySaver = Saver<Overlay, Bundle>(
    save = { overlay ->
        Bundle().apply {
            when (overlay) {
                Overlay.None -> putString("overlay", "none")
                Overlay.About -> putString("overlay", "about")
                Overlay.Theme -> putString("overlay", "theme")
                is Overlay.Result -> {
                    putString("overlay", "result")
                    putString("raw", overlay.scanResult.rawValue)
                    putString("format", overlay.scanResult.format)
                    when (val type = overlay.scanResult.type) {
                        is ScanResultType.Url -> {
                            putString("type", "url")
                            putString("value", type.value)
                        }
                        is ScanResultType.PlainText -> {
                            putString("type", "text")
                            putString("value", type.value)
                        }
                        is ScanResultType.Phone -> {
                            putString("type", "phone")
                            putString("value", type.value)
                        }
                        is ScanResultType.Email -> {
                            putString("type", "email")
                            putString("address", type.address)
                            putString("subject", type.subject)
                            putString("body", type.body)
                        }
                        is ScanResultType.Sms -> {
                            putString("type", "sms")
                            putString("phone", type.phoneNumber)
                            putString("message", type.message)
                        }
                        is ScanResultType.Location -> {
                            putString("type", "location")
                            putDouble("latitude", type.latitude)
                            putDouble("longitude", type.longitude)
                        }
                        is ScanResultType.WifiNetwork -> {
                            putString("type", "wifi")
                            putString("ssid", type.ssid)
                            putString("password", type.password)
                            putString("encryption", type.encryptionType)
                        }
                        is ScanResultType.Contact -> {
                            putString("type", "contact")
                            putString("name", type.name)
                            putString("phone", type.phone)
                            putString("email", type.email)
                            putString("organization", type.organization)
                        }
                        is ScanResultType.CalendarEvent -> {
                            putString("type", "calendar")
                            putString("title", type.title)
                            putString("description", type.description)
                            putString("location", type.location)
                            type.startTimeMillis?.let { putLong("start", it) }
                            type.endTimeMillis?.let { putLong("end", it) }
                            putBoolean("allDay", type.isAllDay)
                        }
                        is ScanResultType.Unknown -> {
                            putString("type", "unknown")
                            putString("value", type.raw)
                        }
                    }
                }
            }
        }
    },
    restore = { saved ->
        when (saved.getString("overlay")) {
            "about" -> Overlay.About
            "theme" -> Overlay.Theme
            "result" -> {
                val type = when (saved.getString("type")) {
                    "url" -> ScanResultType.Url(saved.getString("value").orEmpty())
                    "text" -> ScanResultType.PlainText(saved.getString("value").orEmpty())
                    "phone" -> ScanResultType.Phone(saved.getString("value").orEmpty())
                    "email" -> ScanResultType.Email(
                        saved.getString("address").orEmpty(),
                        saved.getString("subject"),
                        saved.getString("body")
                    )
                    "sms" -> ScanResultType.Sms(saved.getString("phone").orEmpty(), saved.getString("message"))
                    "location" -> ScanResultType.Location(
                        saved.getDouble("latitude"),
                        saved.getDouble("longitude")
                    )
                    "wifi" -> ScanResultType.WifiNetwork(
                        saved.getString("ssid").orEmpty(),
                        saved.getString("password"),
                        saved.getString("encryption")
                    )
                    "contact" -> ScanResultType.Contact(
                        saved.getString("name"),
                        saved.getString("phone"),
                        saved.getString("email"),
                        saved.getString("organization")
                    )
                    "calendar" -> ScanResultType.CalendarEvent(
                        saved.getString("title"),
                        saved.getString("description"),
                        saved.getString("location"),
                        saved.getLong("start").takeIf { saved.containsKey("start") },
                        saved.getLong("end").takeIf { saved.containsKey("end") },
                        saved.getBoolean("allDay")
                    )
                    else -> ScanResultType.Unknown(saved.getString("value").orEmpty())
                }
                Overlay.Result(ScanResult(type, saved.getString("raw").orEmpty(), saved.getString("format").orEmpty()))
            }
            else -> Overlay.None
        }
    }
)

private const val BackScaleDelta = 0.1f
private const val BackCornerRadius = 28f

@Composable
fun ScannApp(
    initialTab: NavTab? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedTab by rememberSaveable { mutableStateOf(initialTab ?: NavTab.SCAN) }
    var visitedTabs by rememberSaveable(stateSaver = visitedTabsSaver) {
        mutableStateOf(setOf(initialTab ?: NavTab.SCAN))
    }
    var overlay by rememberSaveable(stateSaver = overlaySaver) { mutableStateOf<Overlay>(Overlay.None) }
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var settings by remember { mutableStateOf(AppPreferences.load(context)) }
    val scope = rememberCoroutineScope()
    var showRatingPrompt by rememberSaveable { mutableStateOf(false) }
    var previousOverlay by remember { mutableStateOf<Overlay>(Overlay.None) }

    val systemAnimationsOff = rememberSystemAnimationsDisabled()
    val animationsEnabled = settings.animations && !systemAnimationsOff
    val navStyle = if (settings.floatingNav) NavBarStyle.FLOATING else NavBarStyle.DOCKED

    fun applySettings(updated: AppSettings) {
        settings = updated
        AppPreferences.save(context, updated)
    }

    val isDark = isSystemInDarkTheme()
    val onCamera = selectedTab == NavTab.SCAN && overlay is Overlay.None
    LaunchedEffect(selectedTab) { visitedTabs = visitedTabs + selectedTab }
    LaunchedEffect(Unit) { AppPreferences.recordAppOpen(context) }

    // Offer a rating only right after the user closes a scan result, never mid-scan.
    LaunchedEffect(overlay) {
        val closedResult = previousOverlay is Overlay.Result && overlay is Overlay.None
        previousOverlay = overlay
        if (closedResult) {
            (context as? Activity)?.let { AdManager.onScanResultClosed(it) }
            // Never stack the rating prompt on top of an ad.
            if (!AdManager.adShownThisSession && AppPreferences.shouldShowRatingPrompt(context)) {
                delay(600)
                showRatingPrompt = true
            }
        }
    }

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
        appTheme = settings.theme,
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

            if (showRatingPrompt) {
                RatingPromptDialog(
                    onRate = { _ ->
                        showRatingPrompt = false
                        AppPreferences.onRatingPromptFinished(context)
                        ReviewHelper.openPlayStoreListing(context)
                    },
                    onNotNow = {
                        showRatingPrompt = false
                        AppPreferences.onRatingPromptPostponed(context)
                    },
                    onNeverAsk = {
                        showRatingPrompt = false
                        AppPreferences.onRatingPromptFinished(context)
                    }
                )
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TabLayer(visible = selectedTab == NavTab.SCAN, composed = selectedTab == NavTab.SCAN || NavTab.SCAN in visitedTabs) {
                        ScannerScreen(
                            isActive = selectedTab == NavTab.SCAN && overlay is Overlay.None,
                            modifier = Modifier.fillMaxSize(),
                            onBarcodeScanned = { result ->
                                AppPreferences.recordSuccessfulScan(context)
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
                            onSettingsChange = { applySettings(it) },
                            onThemeClick = { overlay = Overlay.Theme }
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

                    AnimatedVisibility(
                        visible = overlay is Overlay.Theme,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f),
                        enter = overlayEnter,
                        exit = overlayExit
                    ) {
                        OverlayLayer(backProgress = { backProgress }) {
                            ThemeScreen(
                                settings = settings,
                                onSettingsChange = { applySettings(it) },
                                onBack = { overlay = Overlay.None }
                            )
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

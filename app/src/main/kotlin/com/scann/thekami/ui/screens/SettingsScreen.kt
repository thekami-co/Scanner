package com.scann.thekami.ui.screens

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.scann.thekami.ui.theme.AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.scann.thekami.util.AdManager
import com.scann.thekami.util.ReviewHelper
import com.scann.thekami.BuildConfig
import com.scann.thekami.R
import com.scann.thekami.ui.components.bottomNavClearance
import com.scann.thekami.util.AppPreferences
import com.scann.thekami.util.AppSettings
import com.scann.thekami.util.Haptics
import com.scann.thekami.util.NotificationController

private val ScreenPadding = 20.dp
private val RowVerticalPadding = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAboutClick: () -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onThemeClick: () -> Unit
) {
    val context = LocalContext.current
    var createNotificationEnabled by remember {
        mutableStateOf(NotificationController.isCreateNotificationEnabled(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            createNotificationEnabled = true
            NotificationController.setCreateNotificationEnabled(context, true)
        }
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No browser available
        }
    }

    fun launchInAppReview() {
        ReviewHelper.launchInAppReview(context)
    }

    fun onNotificationToggle(checked: Boolean) {
        val needsPermission = checked &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            createNotificationEnabled = checked
            NotificationController.setCreateNotificationEnabled(context, checked)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomNavClearance())
        ) {
            SectionLabel("Quick access")
            SwitchRow(
                iconRes = R.drawable.hgi_notification,
                title = "Create QR shortcut",
                subtitle = "Adds a quick shortcut to create a QR code from your notifications",
                checked = createNotificationEnabled,
                onCheckedChange = ::onNotificationToggle
            )

            SectionLabel("Appearance")
            SettingsRow(
                iconRes = R.drawable.hgi_palette,
                title = "Theme",
                subtitle = if (settings.dynamicColor && AppPreferences.dynamicColorSupported) "Dynamic colors" else settings.theme.label,
                onClick = onThemeClick
            )
            SwitchRow(
                iconRes = R.drawable.hgi_nav_bar,
                title = "Floating navigation bar",
                subtitle = "Show a compact floating bar instead of docking it to the bottom",
                checked = settings.floatingNav,
                onCheckedChange = { onSettingsChange(settings.copy(floatingNav = it)) }
            )
            SwitchRow(
                iconRes = R.drawable.hgi_animation,
                title = "Animations",
                subtitle = "Tab transitions and scan animations",
                checked = settings.animations,
                onCheckedChange = { onSettingsChange(settings.copy(animations = it)) }
            )

            SectionLabel("About")
            SettingsRow(
                iconRes = R.drawable.hgi_info,
                title = "About Scann",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                onClick = onAboutClick
            )
            SettingsRow(
                iconRes = R.drawable.hgi_star,
                title = "Rate Scann",
                subtitle = "Enjoying the app? Leave us a rating",
                onClick = ::launchInAppReview
            )
            SettingsRow(
                iconRes = R.drawable.hgi_privacy,
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                onClick = { openUrl("https://blog.thekami.tech/posts/scann/") }
            )

            if (AdManager.privacyOptionsRequired(context)) {
                SettingsRow(
                    iconRes = R.drawable.hgi_privacy,
                    title = "Ad privacy settings",
                    subtitle = "Change your ad consent choices",
                    onClick = { (context as? Activity)?.let { AdManager.showPrivacyOptions(it) } }
                )
            }

            SectionLabel("Follow us")
            Row(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialIconButton(R.drawable.ic_linkedin, "LinkedIn") {
                    openUrl("https://www.linkedin.com/company/thekamiofficial")
                }
                SocialIconButton(R.drawable.ic_discord, "Discord") {
                    openUrl("https://www.thekami.tech/discord/")
                }
                SocialIconButton(R.drawable.ic_github, "GitHub") {
                    openUrl("https://github.com/thekami-dev")
                }
            }

            SectionLabel("More from TheKami")
            SettingsRow(
                iconRes = R.drawable.hgi_apps,
                title = "More apps by TheKami",
                subtitle = "Explore our other apps on Play Store",
                onClick = {
                    openUrl("https://play.google.com/store/apps/dev?id=8580703469843686933")
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp)
                    .clickable(role = Role.Button) { openUrl("https://www.thekami.tech") }
            ) {
                Text(
                    text = "Made by",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                AsyncImage(
                    model = "https://www.thekami.tech/logo.png",
                    contentDescription = "TheKami",
                    modifier = Modifier.height(15.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 6.dp)
    )
}

@Composable
private fun RowIcon(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun RowText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(iconRes)
        Spacer(modifier = Modifier.width(20.dp))
        RowText(title, subtitle, Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.hgi_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SwitchRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    Haptics.tick(view)
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(iconRes)
        Spacer(modifier = Modifier.width(20.dp))
        RowText(title, subtitle, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SocialIconButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ThemeRow(
    selected: AppTheme?,
    onSelect: (AppTheme) -> Unit
) {
    val dark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding)
    ) {
        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AppTheme.entries.forEach { theme ->
                ThemeSwatch(theme, dark, theme == selected, onSelect)
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    dark: Boolean,
    selected: Boolean,
    onSelect: (AppTheme) -> Unit
) {
    val scheme = remember(theme, dark) { theme.colorScheme(dark) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onSelect(theme) }
            )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .padding(5.dp)
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = scheme.primary,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = scheme.primaryContainer,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThemeScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(id = R.drawable.hgi_back), contentDescription = "Back")
                }
                Text(text = "Theme", style = MaterialTheme.typography.titleLarge)
            }
            ThemeRow(
                selected = if (settings.dynamicColor && AppPreferences.dynamicColorSupported) null else settings.theme,
                onSelect = { onSettingsChange(settings.copy(theme = it, dynamicColor = false)) }
            )
            if (AppPreferences.dynamicColorSupported) {
                SwitchRow(
                    iconRes = R.drawable.hgi_palette,
                    title = "Dynamic colors",
                    subtitle = "Match the app to your wallpaper colors",
                    checked = settings.dynamicColor,
                    onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) }
                )
            }
        }
    }
}

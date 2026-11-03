package com.scann.thekami.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
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
import com.google.android.play.core.review.ReviewManagerFactory
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
    onSettingsChange: (AppSettings) -> Unit
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

    fun openPlayStoreListing() {
        openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
    }

    fun launchInAppReview() {
        try {
            val activity = context as? Activity
            if (activity == null) {
                openPlayStoreListing()
                return
            }
            val reviewManager = ReviewManagerFactory.create(context)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reviewManager.launchReviewFlow(activity, task.result)
                } else {
                    openPlayStoreListing()
                }
            }
        } catch (e: Exception) {
            openPlayStoreListing()
        }
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
            if (AppPreferences.dynamicColorSupported) {
                SwitchRow(
                    iconRes = R.drawable.hgi_palette,
                    title = "Dynamic colors",
                    subtitle = "Match the app to your wallpaper colors",
                    checked = settings.dynamicColor,
                    onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) }
                )
            }
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
    onCheckedChange: (Boolean) -> Unit
) {
    val view = LocalView.current

    Row(
        modifier = Modifier
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

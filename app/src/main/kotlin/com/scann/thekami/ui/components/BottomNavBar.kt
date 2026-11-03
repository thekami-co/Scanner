package com.scann.thekami.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scann.thekami.R

enum class NavTab(val label: String, @DrawableRes val iconRes: Int) {
    SCAN("Scan", R.drawable.hgi_scan),
    CREATE("Create", R.drawable.hgi_qr_create),
    SETTINGS("Settings", R.drawable.hgi_settings)
}

/**
 * [overlap] is the height of the area (excluding the system navigation inset)
 * that the bar covers at the bottom of the screen.
 */
enum class NavBarStyle(val overlap: Dp) {
    DOCKED(80.dp),
    FLOATING(80.dp) // 64dp pill + 16dp bottom margin
}

val LocalNavBarStyle = staticCompositionLocalOf { NavBarStyle.DOCKED }

/** Height of the nav bar area that overlays screen content. */
@Composable
fun bottomNavHeight(): Dp = LocalNavBarStyle.current.overlap

/** Space a screen must leave at its bottom so content clears the nav bar. */
@Composable
fun bottomNavClearance(): Dp = bottomNavHeight() + 16.dp

private val PillHeight = 64.dp
private val PillPadding = 6.dp
private val PillItemHeight = PillHeight - PillPadding * 2
private val DockedHeight = 80.dp

private fun <T> navSpec(animations: Boolean): FiniteAnimationSpec<T> =
    if (animations) {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    } else {
        snap()
    }

/** Tab click without a ripple: the item gives a small press-scale instead. */
@Composable
private fun Modifier.tabClickable(selected: Boolean, onClick: () -> Unit): Modifier {
    val animations = LocalAnimationsEnabled.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animations) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tabPressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .selectable(
            selected = selected,
            interactionSource = source,
            indication = null,
            role = Role.Tab,
            onClick = onClick
        )
}

@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    style: NavBarStyle,
    modifier: Modifier = Modifier
) {
    when (style) {
        NavBarStyle.DOCKED -> DockedNavBar(selectedTab, onTabSelected, modifier)
        NavBarStyle.FLOATING -> FloatingNavBar(selectedTab, onTabSelected, modifier)
    }
}

@Composable
private fun DockedNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(DockedHeight)
            ) {
                NavTab.entries.forEach { tab ->
                    DockedNavItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockedNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animations = LocalAnimationsEnabled.current
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = navSpec(animations),
        label = "dockedItemProgress"
    )
    val iconColor = lerp(colors.onSurfaceVariant, colors.onPrimaryContainer, progress)
    val labelColor = lerp(colors.onSurfaceVariant, colors.onSurface, progress)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .tabClickable(selected, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(width = 64.dp, height = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = progress
                        alpha = progress
                    }
                    .background(colors.primaryContainer, CircleShape)
            )
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = labelColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/** Compact pill that hugs its content: only the selected tab shows its label. */
@Composable
private fun FloatingNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
            .height(PillHeight)
    ) {
        Row(
            modifier = Modifier.padding(PillPadding),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                FloatingNavItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animations = LocalAnimationsEnabled.current
    val colorSpec = navSpec<Color>(animations)

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = colorSpec,
        label = "navItemBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = colorSpec,
        label = "navItemColor"
    )

    Row(
        modifier = Modifier
            .height(PillItemHeight)
            .tabClickable(selected, onClick)
            .clip(CircleShape)
            .background(backgroundColor)
            .animateContentSize(animationSpec = navSpec(animations))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = if (selected) null else tab.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = if (animations) fadeIn(tween(200, delayMillis = 60)) else EnterTransition.None,
            exit = if (animations) fadeOut(tween(80)) else ExitTransition.None
        ) {
            Text(
                text = tab.label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

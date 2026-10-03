package com.tankobun.app.ui.shell

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunTokens
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** The four top-level places in the dock. Settings live inside [YOU]. */
internal enum class TankobunDestination(val tab: Int, val icon: ImageVector) {
    HOME(0, TankobunIcons.Home),
    LIBRARY(1, TankobunIcons.LibraryBooks),
    BROWSE(2, TankobunIcons.Explore),
    YOU(3, TankobunIcons.AccountCircle),
    ;

    companion object {
        fun forTab(tab: Int): TankobunDestination = when (tab) {
            0 -> HOME
            1 -> LIBRARY
            2 -> BROWSE
            else -> YOU
        }
    }
}

@Composable
internal fun TankobunDestination.label(): String = when (this) {
    TankobunDestination.HOME -> tankobunString(R.string.nav_home)
    TankobunDestination.LIBRARY -> tankobunString(R.string.nav_library)
    TankobunDestination.BROWSE -> tankobunString(R.string.nav_browse)
    TankobunDestination.YOU -> tankobunString(R.string.nav_you)
}

/** Badges shown on dock and rail destinations. */
internal data class TankobunDestinationBadges(
    val libraryUpdates: Int = 0,
    val downloadsActive: Boolean = false,
)

internal val GlassDockHeight = 64.dp
internal val GlassRailWidth = 80.dp
private val GlassBlurRadius = 24.dp
private const val GlassNoiseFactor = 0.04f
private const val GlassInputScale = 0.33f

/**
 * Frosted glass tinted with the theme's own surface hue. The top bar variant fades out at its
 * bottom edge instead of ending on a hard line; the floating variant has a light rim and a
 * soft shadow tinted by the theme.
 */
@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.tankobunGlass(
    hazeState: HazeState?,
    surface: Color,
    bleed: Color,
    backdrop: Color,
    dark: Boolean,
    fadeOut: Boolean,
): Modifier {
    val tint = if (fadeOut) {
        Brush.verticalGradient(
            0f to lerp(surface, bleed, 0.12f).copy(alpha = if (dark) 0.80f else 0.82f),
            0.62f to surface.copy(alpha = if (dark) 0.50f else 0.55f),
            1f to surface.copy(alpha = 0f),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                lerp(surface, bleed, 0.16f).copy(alpha = if (dark) 0.74f else 0.72f),
                surface.copy(alpha = if (dark) 0.66f else 0.62f),
            ),
        )
    }
    if (hazeState == null) {
        return background(
            if (fadeOut) {
                Brush.verticalGradient(0f to surface, 0.7f to surface.copy(alpha = 0.92f), 1f to surface.copy(alpha = 0f))
            } else {
                Brush.verticalGradient(listOf(surface, surface))
            },
        )
    }
    val style = HazeStyle(
        backgroundColor = backdrop,
        tints = listOf(HazeTint(tint)),
        blurRadius = GlassBlurRadius,
        noiseFactor = GlassNoiseFactor,
        fallbackTint = HazeTint(surface.copy(alpha = if (fadeOut) 0.94f else 0.96f)),
    )
    return hazeEffect(state = hazeState, style = style) {
        inputScale = HazeInputScale.Fixed(GlassInputScale)
        forceInvalidateOnPreDraw = true
        if (fadeOut) {
            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
        }
    }
}

/** A floating glass container, used by the dock, the rail and the detail action bar. */
@Composable
internal fun TankobunGlassFloat(
    hazeState: HazeState?,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalTankobunTokens.current
    Box(
        modifier = modifier
            .shadow(elevation = 14.dp, shape = shape, ambientColor = tokens.glassShadow, spotColor = tokens.glassShadow)
            .clip(shape)
            .tankobunGlass(
                hazeState = hazeState,
                surface = tokens.dockSurface,
                bleed = tokens.dockBleed,
                backdrop = tokens.appBackdrop,
                dark = tokens.dark,
                fadeOut = false,
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(0f to tokens.glassHighlight, 0.45f to tokens.glassOutline, 1f to tokens.glassOutline),
                ),
                shape,
            ),
        content = content,
    )
}

@Composable
internal fun TankobunNavigationDock(
    selected: TankobunDestination,
    badges: TankobunDestinationBadges,
    hazeState: HazeState?,
    onSelect: (TankobunDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Long labels and large font scales must never squeeze the other destinations:
        // the selected label gets whatever width is left and ellipsizes or hides itself.
        val narrow = maxWidth < 340.dp
        val itemPadding = if (narrow) 11.dp else 14.dp
        val itemWidth = DockIconSize + itemPadding * 2
        val others = TankobunDestination.entries.size - 1
        val fixedWidth = DockPadding * 2 + DockItemSpacing * others + itemWidth * others +
            itemPadding * 2 + DockIconSize + DockLabelGap + selected.badgeOverhang(badges)
        val labelMaxWidth = (maxWidth - fixedWidth).coerceIn(0.dp, DockLabelMaxWidth)
        TankobunGlassFloat(
            hazeState = hazeState,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.height(GlassDockHeight),
        ) {
            Row(
                modifier = Modifier.padding(DockPadding),
                horizontalArrangement = Arrangement.spacedBy(DockItemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TankobunDestination.entries.forEach { destination ->
                    DockDestinationItem(
                        destination = destination,
                        selected = destination == selected,
                        badges = badges,
                        itemPadding = itemPadding,
                        labelMaxWidth = labelMaxWidth.takeIf { it >= DockLabelMinWidth } ?: 0.dp,
                        onClick = { onSelect(destination) },
                    )
                }
            }
        }
    }
}

private val DockPadding = 6.dp
private val DockItemSpacing = 2.dp
private val DockIconSize = 24.dp
private val DockLabelGap = 8.dp
private val DockLabelMaxWidth = 112.dp
private val DockLabelMinWidth = 36.dp

/** Room for a badge drawn past the icon's end edge, so it never sits on top of the label. */
private fun TankobunDestination.badgeOverhang(badges: TankobunDestinationBadges): Dp = when {
    this == TankobunDestination.LIBRARY && badges.libraryUpdates > 99 -> 20.dp
    this == TankobunDestination.LIBRARY && badges.libraryUpdates > 9 -> 13.dp
    this == TankobunDestination.LIBRARY && badges.libraryUpdates > 0 -> 7.dp
    this == TankobunDestination.YOU && badges.downloadsActive -> 3.dp
    else -> 0.dp
}

@Composable
private fun DockDestinationItem(
    destination: TankobunDestination,
    selected: Boolean,
    badges: TankobunDestinationBadges,
    itemPadding: Dp,
    labelMaxWidth: Dp,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val label = destination.label()
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "Dock item container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "Dock item content",
    )
    Row(
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = DockIconSize + itemPadding * 2)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Tab,
                onClick = {
                    if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onClick()
                },
            )
            .padding(horizontal = itemPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DestinationIcon(destination = destination, badges = badges, label = label, tint = content, size = if (selected) 22.dp else DockIconSize)
        AnimatedVisibility(
            visible = selected && labelMaxWidth > 0.dp,
            enter = fadeIn(tween(160)) + expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(100)) + shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow)),
        ) {
            Row {
                Spacer(Modifier.width(DockLabelGap + destination.badgeOverhang(badges)))
                Text(
                    text = label,
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = labelMaxWidth)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

@Composable
internal fun TankobunNavigationRail(
    selected: TankobunDestination,
    badges: TankobunDestinationBadges,
    hazeState: HazeState?,
    onSelect: (TankobunDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    TankobunGlassFloat(
        hazeState = hazeState,
        shape = RoundedCornerShape(40.dp),
        modifier = modifier.width(GlassRailWidth),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TankobunDestination.entries.forEach { destination ->
                RailDestinationItem(
                    destination = destination,
                    selected = destination == selected,
                    badges = badges,
                    onClick = { onSelect(destination) },
                )
            }
        }
    }
}

@Composable
private fun RailDestinationItem(
    destination: TankobunDestination,
    selected: Boolean,
    badges: TankobunDestinationBadges,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val label = destination.label()
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "Rail item container",
    )
    Column(
        modifier = Modifier
            .width(72.dp)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Tab,
                onClick = {
                    if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onClick()
                },
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            DestinationIcon(
                destination = destination,
                badges = badges,
                label = label,
                tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                size = 22.dp,
            )
        }
        Text(
            text = label,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun DestinationIcon(
    destination: TankobunDestination,
    badges: TankobunDestinationBadges,
    label: String,
    tint: Color,
    size: Dp,
) {
    val updates = badges.libraryUpdates.takeIf { destination == TankobunDestination.LIBRARY && it > 0 }
    val downloads = destination == TankobunDestination.YOU && badges.downloadsActive
    val description = when {
        updates != null -> tankobunString(R.string.nav_library_updates_badge, label, updates)
        downloads -> tankobunString(R.string.nav_you_downloads_badge, label)
        else -> label
    }
    BadgedBox(
        badge = {
            when {
                updates != null -> Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Text(if (updates > 99) "99+" else updates.toString()) }
                downloads -> Badge(containerColor = MaterialTheme.colorScheme.tertiary)
            }
        },
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/** A circular glass-free icon button used in the top bar, with an optional count or dot badge. */
@Composable
internal fun TopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    badgeDot: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(
                selected = false,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                when {
                    badgeCount > 0 -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
                    badgeDot -> Badge(containerColor = MaterialTheme.colorScheme.tertiary)
                }
            },
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(23.dp))
        }
    }
}

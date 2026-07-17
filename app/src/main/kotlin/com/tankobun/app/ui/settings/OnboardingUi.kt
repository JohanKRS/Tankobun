package com.tankobun.app.ui.settings

import com.tankobun.app.ui.icons.TankobunIcons

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.LibraryMode
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.LocalTankobunTokens
import com.tankobun.app.R
import com.tankobun.app.TankobunThemePreference
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.components.TankobunActionButton

internal enum class AppTourStep {
    HOME,
    LIBRARY,
    BROWSE,
    PROFILE,
    SETTINGS,
}

private val AppTourSteps = AppTourStep.entries.toList()

@Composable
internal fun OnboardingDialog(
    initialLibraryMode: LibraryMode,
    initialThemePreference: TankobunThemePreference,
    onPrepareContent: () -> Unit,
    onThemeSelected: (TankobunThemePreference) -> Unit,
    onComplete: (LibraryMode, TankobunThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var selectedLibraryMode by remember(initialLibraryMode) { androidx.compose.runtime.mutableStateOf(initialLibraryMode) }
    var selectedThemePreference by remember(initialThemePreference) { androidx.compose.runtime.mutableStateOf(initialThemePreference) }
    val pageCount = 2
    val isLastPage = pageIndex == pageCount - 1
    val finish = { onComplete(selectedLibraryMode, selectedThemePreference) }

    LaunchedEffect(Unit) {
        onPrepareContent()
    }
    BackHandler { finish() }

    Dialog(
        onDismissRequest = finish,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = LocalTankobunTokens.current.appBackdrop,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OnboardingTopBar(
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onClose = finish,
                )
                AnimatedContent(
                    targetState = pageIndex,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 })
                            .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 12 })
                    },
                    label = "onboarding-page",
                ) { visiblePage ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (visiblePage) {
                            0 -> OnboardingThemeStep(
                                selected = selectedThemePreference,
                                onSelected = { preference ->
                                    selectedThemePreference = preference
                                    onThemeSelected(preference)
                                },
                            )
                            else -> OnboardingLibraryModeStep(
                                selected = selectedLibraryMode,
                                onSelected = { selectedLibraryMode = it },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = finish) {
                        Text(tankobunString(R.string.onboarding_skip))
                    }
                    Spacer(Modifier.weight(1f))
                    if (pageIndex > 0) {
                        TankobunActionButton(
                            label = tankobunString(R.string.common_back),
                            onClick = { pageIndex -= 1 },
                            filled = false,
                        )
                    }
                    TankobunActionButton(
                        label = if (isLastPage) tankobunString(R.string.common_start) else tankobunString(R.string.common_next),
                        onClick = {
                            if (isLastPage) {
                                finish()
                            } else {
                                pageIndex += 1
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppTourOverlay(
    onStepChanged: (AppTourStep) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = AppTourSteps[stepIndex]

    LaunchedEffect(step) {
        onStepChanged(step)
    }
    BackHandler {
        if (stepIndex > 0) {
            stepIndex -= 1
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
                    ),
                ),
            )
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(16.dp),
    ) {
        AnimatedContent(
            targetState = stepIndex,
            modifier = Modifier.align(Alignment.BottomCenter),
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 9 })
                    .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 10 })
            },
            label = "app-tour-card",
        ) { visibleIndex ->
            val visibleStep = AppTourSteps[visibleIndex]
            val isFirst = visibleIndex == 0
            val isLast = visibleIndex == AppTourSteps.lastIndex
            TourCoachCard(
                step = visibleStep,
                stepIndex = visibleIndex,
                stepCount = AppTourSteps.size,
                isFirst = isFirst,
                isLast = isLast,
                onBack = { stepIndex -= 1 },
                onNext = {
                    if (isLast) {
                        onDismiss()
                    } else {
                        stepIndex += 1
                    }
                },
                onSkip = onDismiss,
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    pageIndex: Int,
    pageCount: Int,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    tankobunString(R.string.onboarding_welcome),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tankobunString(R.string.onboarding_step_count, pageIndex + 1, pageCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onClose) {
                Icon(TankobunIcons.Close, contentDescription = tankobunString(R.string.onboarding_close_tutorial))
            }
        }
        OnboardingProgress(currentIndex = pageIndex, count = pageCount)
    }
}

@Composable
private fun OnboardingStepIntro(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = LocalTankobunTokens.current.softAccent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = LocalTankobunStyle.current.themeShapes.control,
                color = LocalTankobunStyle.current.colors.selectedChip,
                contentColor = LocalTankobunStyle.current.colors.selectedChipContent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OnboardingLibraryModeStep(
    selected: LibraryMode,
    onSelected: (LibraryMode) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OnboardingStepIntro(
            icon = TankobunIcons.LibraryBooks,
            title = tankobunString(R.string.onboarding_library_mode_title),
            body = tankobunString(R.string.onboarding_library_mode_body),
        )
        OnboardingChoiceButton(
            selected = selected == LibraryMode.LOCAL,
            title = tankobunString(R.string.onboarding_library_mode_local),
            body = tankobunString(R.string.onboarding_library_mode_local_desc),
            icon = TankobunIcons.LibraryBooks,
            onClick = { onSelected(LibraryMode.LOCAL) },
        )
        OnboardingChoiceButton(
            selected = selected == LibraryMode.ANILIST,
            title = tankobunString(R.string.onboarding_library_mode_anilist),
            body = tankobunString(R.string.onboarding_library_mode_anilist_desc),
            icon = TankobunIcons.Link,
            onClick = { onSelected(LibraryMode.ANILIST) },
        )
    }
}

@Composable
private fun OnboardingThemeStep(
    selected: TankobunThemePreference,
    onSelected: (TankobunThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OnboardingStepIntro(
            icon = TankobunIcons.Palette,
            title = tankobunString(R.string.onboarding_theme_title),
            body = tankobunString(R.string.onboarding_theme_body),
        )
        ThemePicker(selected = selected, onSelect = onSelected)
    }
}

@Composable
private fun OnboardingChoiceButton(
    selected: Boolean,
    title: String,
    body: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val choiceShape = LocalTankobunStyle.current.themeShapes.control
    val bodyColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = bodyColor)
            }
        }
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = choiceShape) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = choiceShape) { content() }
    }
}

@Composable
private fun OnboardingProgress(
    currentIndex: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (index <= currentIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun TourCoachCard(
    step: AppTourStep,
    stepIndex: Int,
    stepCount: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
        tonalElevation = 6.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OnboardingProgress(currentIndex = stepIndex, count = stepCount)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = LocalTankobunStyle.current.themeShapes.control,
                        color = LocalTankobunStyle.current.colors.selectedChip,
                        contentColor = LocalTankobunStyle.current.colors.selectedChipContent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(step.icon(), contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tankobunString(R.string.app_tour_step_count, stepIndex + 1, stepCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            tankobunString(step.titleRes()),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    tankobunString(step.bodyRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (firstPoint, secondPoint) = step.pointRes()
                    TourFeatureRow(firstPoint)
                    TourFeatureRow(secondPoint)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) {
                        Text(tankobunString(R.string.onboarding_skip))
                    }
                    Spacer(Modifier.weight(1f))
                    if (!isFirst) {
                        TankobunActionButton(
                            label = tankobunString(R.string.common_back),
                            onClick = onBack,
                            filled = false,
                        )
                    }
                    TankobunActionButton(
                        label = if (isLast) tankobunString(R.string.common_finish) else tankobunString(R.string.common_next),
                        onClick = onNext,
                    )
                }
            }
        }
    }
}

@Composable
private fun TourFeatureRow(@StringRes textRes: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalTankobunStyle.current.themeShapes.control,
        color = LocalTankobunTokens.current.elevatedSurface,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = LocalTankobunStyle.current.themeShapes.indicator,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(TankobunIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                }
            }
            Text(
                tankobunString(textRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun AppTourStep.icon(): ImageVector =
    when (this) {
        AppTourStep.HOME -> TankobunIcons.Home
        AppTourStep.LIBRARY -> TankobunIcons.LibraryBooks
        AppTourStep.BROWSE -> TankobunIcons.Explore
        AppTourStep.PROFILE -> TankobunIcons.AccountCircle
        AppTourStep.SETTINGS -> TankobunIcons.Settings
    }

@StringRes
private fun AppTourStep.titleRes(): Int =
    when (this) {
        AppTourStep.HOME -> R.string.app_tour_home_title
        AppTourStep.LIBRARY -> R.string.app_tour_library_title
        AppTourStep.BROWSE -> R.string.app_tour_browse_title
        AppTourStep.PROFILE -> R.string.app_tour_profile_title
        AppTourStep.SETTINGS -> R.string.app_tour_settings_title
    }

@StringRes
private fun AppTourStep.bodyRes(): Int =
    when (this) {
        AppTourStep.HOME -> R.string.app_tour_home_body
        AppTourStep.LIBRARY -> R.string.app_tour_library_body
        AppTourStep.BROWSE -> R.string.app_tour_browse_body
        AppTourStep.PROFILE -> R.string.app_tour_profile_body
        AppTourStep.SETTINGS -> R.string.app_tour_settings_body
    }

private fun AppTourStep.pointRes(): Pair<Int, Int> =
    when (this) {
        AppTourStep.HOME -> R.string.app_tour_home_point_1 to R.string.app_tour_home_point_2
        AppTourStep.LIBRARY -> R.string.app_tour_library_point_1 to R.string.app_tour_library_point_2
        AppTourStep.BROWSE -> R.string.app_tour_browse_point_1 to R.string.app_tour_browse_point_2
        AppTourStep.PROFILE -> R.string.app_tour_profile_point_1 to R.string.app_tour_profile_point_2
        AppTourStep.SETTINGS -> R.string.app_tour_settings_point_1 to R.string.app_tour_settings_point_2
    }

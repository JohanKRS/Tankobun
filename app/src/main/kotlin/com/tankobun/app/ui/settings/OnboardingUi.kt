package com.tankobun.app.ui.settings

import com.tankobun.app.ui.icons.TankobunIcons

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    LIBRARY,
    BROWSE,
    TRACKING,
    QUICK_ACTIONS,
    SOURCES,
    READER,
    BACKUPS,
    PROFILE,
}

private val AppTourSteps = AppTourStep.entries.toList()

@Composable
internal fun OnboardingDialog(
    initialLibraryMode: LibraryMode,
    initialThemePreference: TankobunThemePreference,
    onPrepareBrowse: () -> Unit,
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
        onPrepareBrowse()
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OnboardingTopBar(
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onClose = finish,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OnboardingHeroIcon(
                        icon = if (pageIndex == 0) TankobunIcons.LibraryBooks else TankobunIcons.Palette,
                    )
                    when (pageIndex) {
                        0 -> OnboardingLibraryModeStep(
                            selected = selectedLibraryMode,
                            onSelected = { selectedLibraryMode = it },
                        )
                        else -> OnboardingThemeStep(
                            selected = selectedThemePreference,
                            onSelected = { preference ->
                                selectedThemePreference = preference
                                onThemeSelected(preference)
                            },
                        )
                    }
                }
                OnboardingPageDots(pageIndex = pageIndex, pageCount = pageCount)
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
    libraryMode: LibraryMode,
    tourExampleMediaId: Int?,
    onStepChanged: (AppTourStep) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = AppTourSteps[stepIndex]
    val isFirst = stepIndex == 0
    val isLast = stepIndex == AppTourSteps.lastIndex
    val exampleStepKey = if (step.usesTourExampleMedia()) tourExampleMediaId else null

    LaunchedEffect(step, exampleStepKey) {
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
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f))
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(16.dp),
    ) {
        TourFocusHint(
            step = step,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        TourCoachCard(
            step = step,
            stepIndex = stepIndex,
            stepCount = AppTourSteps.size,
            libraryMode = libraryMode,
            tourExampleLoaded = tourExampleMediaId != null,
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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun OnboardingTopBar(
    pageIndex: Int,
    pageCount: Int,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tankobunString(R.string.onboarding_welcome), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                tankobunString(R.string.onboarding_step_count, pageIndex + 1, pageCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClose) {
            Icon(TankobunIcons.Close, contentDescription = tankobunString(R.string.onboarding_close_tutorial))
        }
    }
}

@Composable
private fun OnboardingHeroIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = LocalTankobunStyle.current.themeShapes.panel,
        color = LocalTankobunStyle.current.colors.selectedChip,
        contentColor = LocalTankobunStyle.current.colors.selectedChipContent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp))
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingStepHeader(
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingStepHeader(
            title = tankobunString(R.string.onboarding_theme_title),
            body = tankobunString(R.string.onboarding_theme_body),
        )
        ThemePicker(selected = selected, onSelect = onSelected)
    }
}

@Composable
private fun OnboardingStepHeader(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
private fun OnboardingPageDots(pageIndex: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == pageIndex
            Box(
                modifier = Modifier
                    .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun TourFocusHint(step: AppTourStep, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(step.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(tankobunString(step.focusRes()), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TourCoachCard(
    step: AppTourStep,
    stepIndex: Int,
    stepCount: Int,
    libraryMode: LibraryMode,
    tourExampleLoaded: Boolean,
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    Text(tankobunString(step.titleRes()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                tankobunString(step.bodyRes(libraryMode)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            step.panelRes(libraryMode, tourExampleLoaded)?.let { panelRes ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LocalTankobunStyle.current.themeShapes.control,
                    color = LocalTankobunTokens.current.elevatedSurface,
                    contentColor = LocalTankobunStyle.current.colors.panelContent,
                ) {
                    Text(
                        tankobunString(panelRes),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    TankobunActionButton(label = tankobunString(R.string.common_back), onClick = onBack, filled = false)
                }
                TankobunActionButton(
                    label = if (isLast) tankobunString(R.string.common_finish) else tankobunString(R.string.common_next),
                    onClick = onNext,
                )
            }
        }
    }
}

private fun AppTourStep.icon(): ImageVector =
    when (this) {
        AppTourStep.LIBRARY -> TankobunIcons.LibraryBooks
        AppTourStep.BROWSE -> TankobunIcons.Search
        AppTourStep.TRACKING -> TankobunIcons.Tag
        AppTourStep.QUICK_ACTIONS -> TankobunIcons.Tune
        AppTourStep.SOURCES -> TankobunIcons.Extension
        AppTourStep.READER -> TankobunIcons.MenuBook
        AppTourStep.BACKUPS -> TankobunIcons.Backup
        AppTourStep.PROFILE -> TankobunIcons.AccountCircle
    }

@StringRes
private fun AppTourStep.focusRes(): Int =
    when (this) {
        AppTourStep.LIBRARY -> R.string.app_tour_focus_library
        AppTourStep.BROWSE -> R.string.app_tour_focus_browse
        AppTourStep.TRACKING -> R.string.app_tour_focus_tracking
        AppTourStep.QUICK_ACTIONS -> R.string.app_tour_focus_quick_actions
        AppTourStep.SOURCES -> R.string.app_tour_focus_sources
        AppTourStep.READER -> R.string.app_tour_focus_reader
        AppTourStep.BACKUPS -> R.string.app_tour_focus_backups
        AppTourStep.PROFILE -> R.string.app_tour_focus_profile
    }

@StringRes
private fun AppTourStep.titleRes(): Int =
    when (this) {
        AppTourStep.LIBRARY -> R.string.app_tour_library_title
        AppTourStep.BROWSE -> R.string.app_tour_browse_title
        AppTourStep.TRACKING -> R.string.app_tour_tracking_title
        AppTourStep.QUICK_ACTIONS -> R.string.app_tour_quick_actions_title
        AppTourStep.SOURCES -> R.string.app_tour_sources_title
        AppTourStep.READER -> R.string.app_tour_reader_title
        AppTourStep.BACKUPS -> R.string.app_tour_backups_title
        AppTourStep.PROFILE -> R.string.app_tour_profile_title
    }

@StringRes
private fun AppTourStep.bodyRes(libraryMode: LibraryMode): Int =
    when (this) {
        AppTourStep.LIBRARY -> R.string.app_tour_library_body
        AppTourStep.BROWSE -> R.string.app_tour_browse_body
        AppTourStep.TRACKING -> if (libraryMode == LibraryMode.LOCAL) {
            R.string.app_tour_tracking_body_local
        } else {
            R.string.app_tour_tracking_body_anilist
        }
        AppTourStep.QUICK_ACTIONS -> R.string.app_tour_quick_actions_body
        AppTourStep.SOURCES -> R.string.app_tour_sources_body
        AppTourStep.READER -> R.string.app_tour_reader_body
        AppTourStep.BACKUPS -> R.string.app_tour_backups_body
        AppTourStep.PROFILE -> R.string.app_tour_profile_body
    }

@StringRes
private fun AppTourStep.panelRes(libraryMode: LibraryMode, tourExampleLoaded: Boolean): Int? =
    when (this) {
        AppTourStep.TRACKING -> if (!tourExampleLoaded) {
            R.string.app_tour_example_loading_panel
        } else if (libraryMode == LibraryMode.LOCAL) {
            R.string.app_tour_tracking_panel_local
        } else {
            R.string.app_tour_tracking_panel_anilist
        }
        AppTourStep.QUICK_ACTIONS -> if (tourExampleLoaded) {
            R.string.app_tour_quick_actions_panel
        } else {
            R.string.app_tour_example_loading_panel
        }
        AppTourStep.SOURCES -> R.string.app_tour_sources_panel
        AppTourStep.READER -> R.string.app_tour_reader_panel
        AppTourStep.BACKUPS -> R.string.app_tour_backups_panel
        else -> null
    }

private fun AppTourStep.usesTourExampleMedia(): Boolean =
    this == AppTourStep.TRACKING ||
        this == AppTourStep.QUICK_ACTIONS ||
        this == AppTourStep.READER

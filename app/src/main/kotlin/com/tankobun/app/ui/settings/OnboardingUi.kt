package com.tankobun.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.LibraryMode
import com.tankobun.app.R
import com.tankobun.app.TankobunThemeMode
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.components.TankobunActionButton

private data class OnboardingPage(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val pointRes: List<Int>,
)

private val TankobunOnboardingPages = listOf(
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        titleRes = R.string.onboarding_page_shelf_title,
        bodyRes = R.string.onboarding_page_shelf_body,
        pointRes = listOf(
            R.string.onboarding_page_shelf_bullet_1,
            R.string.onboarding_page_shelf_bullet_2,
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Download,
        titleRes = R.string.onboarding_page_extensions_title,
        bodyRes = R.string.onboarding_page_extensions_body,
        pointRes = listOf(
            R.string.onboarding_page_extensions_bullet_1,
            R.string.onboarding_page_extensions_bullet_2,
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Search,
        titleRes = R.string.onboarding_page_source_match_title,
        bodyRes = R.string.onboarding_page_source_match_body,
        pointRes = listOf(
            R.string.onboarding_page_source_match_bullet_1,
            R.string.onboarding_page_source_match_bullet_2,
        ),
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        titleRes = R.string.onboarding_page_read_title,
        bodyRes = R.string.onboarding_page_read_body,
        pointRes = listOf(
            R.string.onboarding_page_read_bullet_1,
            R.string.onboarding_page_read_bullet_2,
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Settings,
        titleRes = R.string.onboarding_page_customize_title,
        bodyRes = R.string.onboarding_page_customize_body,
        pointRes = listOf(
            R.string.onboarding_page_customize_bullet_1,
            R.string.onboarding_page_customize_bullet_2,
        ),
    ),
)

@Composable
internal fun OnboardingDialog(
    initialLibraryMode: LibraryMode,
    initialThemeMode: TankobunThemeMode,
    onComplete: (LibraryMode, TankobunThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var selectedLibraryMode by remember(initialLibraryMode) { androidx.compose.runtime.mutableStateOf(initialLibraryMode) }
    var selectedThemeMode by remember(initialThemeMode) { androidx.compose.runtime.mutableStateOf(initialThemeMode) }
    val pageCount = 3
    val isLastPage = pageIndex == pageCount - 1
    val finish = { onComplete(selectedLibraryMode, selectedThemeMode) }

    Dialog(
        onDismissRequest = finish,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tankobunString(R.string.onboarding_welcome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            tankobunString(R.string.onboarding_step_count, pageIndex + 1, pageCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = finish) {
                        Icon(Icons.Default.Close, contentDescription = tankobunString(R.string.onboarding_close_tutorial))
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(78.dp),
                    shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
                    color = LocalTankobunStyle.current.colors.selectedChip,
                    contentColor = LocalTankobunStyle.current.colors.selectedChipContent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when (pageIndex) {
                                0 -> Icons.AutoMirrored.Filled.LibraryBooks
                                1 -> Icons.Default.Settings
                                else -> Icons.AutoMirrored.Filled.MenuBook
                            },
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }

                when (pageIndex) {
                    0 -> OnboardingLibraryModeStep(
                        selected = selectedLibraryMode,
                        onSelected = { selectedLibraryMode = it },
                    )
                    1 -> OnboardingThemeStep(
                        selected = selectedThemeMode,
                        onSelected = { selectedThemeMode = it },
                    )
                    else -> OnboardingTourStep()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pageCount) { index ->
                        val selected = index == pageIndex
                        Box(
                            modifier = Modifier
                                .size(width = if (selected) 18.dp else 7.dp, height = 7.dp)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = finish) {
                        Text(if (isLastPage) tankobunString(R.string.common_close) else tankobunString(R.string.onboarding_skip))
                    }
                    Spacer(Modifier.weight(1f))
                    if (pageIndex > 0) {
                        TankobunActionButton(label = tankobunString(R.string.common_back), onClick = { pageIndex -= 1 }, filled = false)
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
private fun OnboardingLibraryModeStep(
    selected: LibraryMode,
    onSelected: (LibraryMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingStepHeader(
            title = tankobunString(R.string.onboarding_library_mode_title),
            body = tankobunString(R.string.onboarding_library_mode_body),
        )
        OnboardingChoiceButton(
            selected = selected == LibraryMode.LOCAL,
            title = tankobunString(R.string.onboarding_library_mode_local),
            body = tankobunString(R.string.onboarding_library_mode_local_desc),
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            onClick = { onSelected(LibraryMode.LOCAL) },
        )
        OnboardingChoiceButton(
            selected = selected == LibraryMode.ANILIST,
            title = tankobunString(R.string.onboarding_library_mode_anilist),
            body = tankobunString(R.string.onboarding_library_mode_anilist_desc),
            icon = Icons.Default.Link,
            onClick = { onSelected(LibraryMode.ANILIST) },
        )
    }
}

@Composable
private fun OnboardingThemeStep(
    selected: TankobunThemeMode,
    onSelected: (TankobunThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingStepHeader(
            title = tankobunString(R.string.onboarding_theme_title),
            body = tankobunString(R.string.onboarding_theme_body),
        )
        listOf(
            TankobunThemeMode.SYSTEM to tankobunString(R.string.onboarding_theme_system),
            TankobunThemeMode.LIGHT to tankobunString(R.string.onboarding_theme_light),
            TankobunThemeMode.DARK to tankobunString(R.string.onboarding_theme_dark),
        ).forEach { (mode, label) ->
            OnboardingChoiceButton(
                selected = selected == mode,
                title = label,
                body = "",
                icon = Icons.Default.Settings,
                onClick = { onSelected(mode) },
            )
        }
    }
}

@Composable
private fun OnboardingTourStep() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingStepHeader(
            title = tankobunString(R.string.onboarding_tour_title),
            body = tankobunString(R.string.onboarding_tour_body),
        )
        listOf(
            R.string.onboarding_tour_bullet_library,
            R.string.onboarding_tour_bullet_browse,
            R.string.onboarding_tour_bullet_sources,
            R.string.onboarding_tour_bullet_reader,
            R.string.onboarding_tour_bullet_backups,
            R.string.onboarding_tour_bullet_profile,
        ).forEach { pointRes ->
            OnboardingBullet(pointRes)
        }
    }
}

@Composable
private fun OnboardingStepHeader(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
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
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (body.isNotBlank()) {
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun OnboardingBullet(pointRes: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(7.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
        )
        Text(
            tankobunString(pointRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

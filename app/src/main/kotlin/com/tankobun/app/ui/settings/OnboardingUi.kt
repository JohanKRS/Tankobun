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
import com.tankobun.app.R
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = TankobunOnboardingPages[pageIndex]
    val isLastPage = pageIndex == TankobunOnboardingPages.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
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
                            tankobunString(R.string.onboarding_step_count, pageIndex + 1, TankobunOnboardingPages.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
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
                        Icon(page.icon, contentDescription = null, modifier = Modifier.size(38.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tankobunString(page.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        tankobunString(page.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    page.pointRes.forEach { pointRes ->
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TankobunOnboardingPages.indices.forEach { index ->
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
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
                                onDismiss()
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

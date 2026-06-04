package com.tankobun.app.ui.settings

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
import com.tankobun.app.ui.components.TankobunActionButton

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val points: List<String>,
)

private val TankobunOnboardingPages = listOf(
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        title = "Build your manga shelf",
        body = "Tankobun keeps your AniList library, browsing, reading progress, and downloads in one place.",
        points = listOf(
            "Connect AniList when you want your lists and progress synced.",
            "Browse also works for discovering manga before you sign in.",
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Download,
        title = "Add source extensions",
        body = "Sources are installed by you and stay outside the app until you enable them.",
        points = listOf(
            "Use Settings > Sources to install or manage extension languages.",
            "Keep only the sources you actually want active.",
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Search,
        title = "Pick a readable source",
        body = "Open a manga, tap Source, and Tankobun checks enabled sources for readable matches.",
        points = listOf(
            "Choose Change anytime if a source is slow or missing chapters.",
            "Some sources need direct URLs or may fail without blocking the rest.",
        ),
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        title = "Read your way",
        body = "Use paged or webtoon reading, resume from the chapter list, and let progress update as you finish chapters.",
        points = listOf(
            "Reader settings control spacing and reading mode.",
            "Downloads can queue all, unread, next 10, or hand-picked chapters.",
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Settings,
        title = "Make it yours",
        body = "Themes, backups, source filters, downloads, and AniList behavior all live in Settings.",
        points = listOf(
            "Replay this tutorial from Settings > About.",
            "Export backups when you want a portable AniList/MAL-friendly file.",
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
                        Text("Welcome to Tankobun", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Step ${pageIndex + 1} of ${TankobunOnboardingPages.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close tutorial")
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
                        page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    page.points.forEach { point ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(7.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
                            )
                            Text(
                                point,
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
                        Text(if (isLastPage) "Close" else "Skip")
                    }
                    Spacer(Modifier.weight(1f))
                    if (pageIndex > 0) {
                        TankobunActionButton(label = "Back", onClick = { pageIndex -= 1 }, filled = false)
                    }
                    TankobunActionButton(
                        label = if (isLastPage) "Start" else "Next",
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

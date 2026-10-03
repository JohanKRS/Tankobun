package com.tankobun.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.R
import com.tankobun.app.state.LibraryUpdateGroup
import com.tankobun.app.tankobunQuantityString
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.app.ui.media.CoverImage
import com.tankobun.app.ui.media.chapterDateLabel
import com.tankobun.core.model.AnilistMedia

private const val VisibleChaptersPerManga = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryUpdatesSheet(
    groups: List<LibraryUpdateGroup>,
    seenAtEpochMillis: Long,
    checksEnabled: Boolean,
    onOpenMedia: (AnilistMedia) -> Unit,
    onCheckNow: () -> Unit,
    onTurnOnChecks: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
    ) {
        LibraryUpdatesContent(
            groups = groups,
            seenAtEpochMillis = seenAtEpochMillis,
            checksEnabled = checksEnabled,
            onOpenMedia = onOpenMedia,
            onCheckNow = onCheckNow,
            onTurnOnChecks = onTurnOnChecks,
            onClear = onClear,
        )
    }
}

@Composable
internal fun LibraryUpdatesContent(
    groups: List<LibraryUpdateGroup>,
    seenAtEpochMillis: Long,
    checksEnabled: Boolean,
    onOpenMedia: (AnilistMedia) -> Unit,
    onCheckNow: () -> Unit,
    onTurnOnChecks: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tankobunString(R.string.library_updates_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (checksEnabled) {
                IconButton(onClick = onCheckNow) {
                    Icon(TankobunIcons.Refresh, contentDescription = tankobunString(R.string.library_updates_check_now))
                }
            }
            if (groups.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(TankobunIcons.Delete, contentDescription = tankobunString(R.string.library_updates_clear))
                }
            }
        }
        if (!checksEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = LocalTankobunStyle.current.themeShapes.panel,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(tankobunString(R.string.library_updates_checks_off), style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(onClick = onTurnOnChecks, modifier = Modifier.align(Alignment.End)) {
                        Icon(TankobunIcons.Bell, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tankobunString(R.string.library_updates_turn_on))
                    }
                }
            }
        }
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    TankobunIcons.Bell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    tankobunString(R.string.library_updates_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    tankobunString(R.string.library_updates_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
            ) {
                items(groups, key = { it.media.id }) { group ->
                    LibraryUpdateGroupRow(
                        group = group,
                        seenAtEpochMillis = seenAtEpochMillis,
                        onClick = { onOpenMedia(group.media) },
                    )
                }
            }
        }
        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun LibraryUpdateGroupRow(
    group: LibraryUpdateGroup,
    seenAtEpochMillis: Long,
    onClick: () -> Unit,
) {
    val unseen = group.chapters.any { !it.read && it.foundAtEpochMillis > seenAtEpochMillis }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CoverImage(
            url = group.media.coverImage,
            title = group.media.title.userPreferred,
            modifier = Modifier.size(width = 48.dp, height = 70.dp),
            cornerRadius = 8.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (unseen) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
                Text(
                    group.media.title.userPreferred,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val meta = listOfNotNull(
                group.newChapterCount.takeIf { it > 0 }
                    ?.let { count -> tankobunQuantityString(R.plurals.library_updates_new_chapters, count, count) },
                chapterDateLabel(group.newestFoundAtEpochMillis),
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            group.chapters.take(VisibleChaptersPerManga).forEach { item ->
                Text(
                    item.chapter.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.read) 0.55f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val hidden = group.chapters.size - VisibleChaptersPerManga
            if (hidden > 0) {
                Text(
                    tankobunQuantityString(R.plurals.library_updates_more, hidden, hidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

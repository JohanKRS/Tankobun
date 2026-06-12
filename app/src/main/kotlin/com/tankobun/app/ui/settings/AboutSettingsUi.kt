package com.tankobun.app.ui.settings

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets as AndroidWindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.sourceSettingsKey
import com.tankobun.app.state.DownloadStorageItem
import com.tankobun.app.state.ExtensionInstallRequest
import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.LibrarySection
import com.tankobun.app.state.RecentReadingProgress
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.updates.AppUpdateInfo
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.model.AnilistMedia
import com.tankobun.core.model.AnilistMediaTag
import com.tankobun.core.model.AnilistRecommendation
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.ReadingProgress
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.ReaderMode
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt


import com.tankobun.app.*
import com.tankobun.app.logic.*
import com.tankobun.app.state.*
import com.tankobun.app.ui.browse.*
import com.tankobun.app.ui.components.*
import com.tankobun.app.ui.downloads.*
import com.tankobun.app.ui.library.*
import com.tankobun.app.ui.media.*
import com.tankobun.app.ui.reader.*
import com.tankobun.app.ui.settings.*
import com.tankobun.app.ui.shell.*

@Composable
internal fun AboutSettingsScreen(
    state: TankobunUiState,
    viewModel: MainViewModel,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var launchedUpdateRequest by remember { mutableStateOf<AppUpdateInstallRequest?>(null) }
    val updateInstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launchedUpdateRequest = null
    }
    LaunchedEffect(state.appUpdateInstallRequest?.apkUri) {
        val installRequest = state.appUpdateInstallRequest ?: return@LaunchedEffect
        launchedUpdateRequest = installRequest
        updateInstallLauncher.launch(downloadedAppUpdateInstallIntent(installRequest))
        viewModel.consumeAppUpdateInstallRequest()
    }
    SettingsDetailPanel(
        title = tankobunString(R.string.common_about),
        subtitle = tankobunString(R.string.settings_about_subtitle),
        modifier = modifier,
    ) {
        TankobunPanel(
            modifier = Modifier.fillMaxWidth(),
            color = LocalTankobunStyle.current.colors.panel,
            contentColor = LocalTankobunStyle.current.colors.panelContent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Tankobun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    tankobunString(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TankobunActionButton(
                    label = tankobunString(R.string.about_github),
                    iconPainter = painterResource(R.drawable.ic_github),
                    onClick = { uriHandler.openUri(TankobunGithubUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                )
                TankobunActionButton(
                    label = tankobunString(R.string.about_anilist_website),
                    icon = Icons.Default.Link,
                    onClick = { uriHandler.openUri(TankobunAniListUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                )
                TankobunActionButton(
                    label = tankobunString(R.string.about_replay_tutorial),
                    icon = Icons.Default.Replay,
                    onClick = onReplayOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                )
                Text(
                    tankobunString(R.string.about_unofficial),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    tankobunString(R.string.about_anilist_thanks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    tankobunString(R.string.about_anilist_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    tankobunString(R.string.about_sources),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppUpdatesContent(
                    state = state,
                    viewModel = viewModel,
                    context = context,
                    onOpenRelease = { url -> uriHandler.openUri(url) },
                )
                AboutChangelogContent(state = state)
            }
        }
    }
}

@Composable
private fun AppUpdatesContent(
    state: TankobunUiState,
    viewModel: MainViewModel,
    context: Context,
    onOpenRelease: (String) -> Unit,
) {
    val update = state.appUpdateInfo
    val availableUpdate = update?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    val updateAvailable = availableUpdate != null
    val statusText = when {
        state.appUpdateCheckInProgress -> tankobunString(R.string.about_update_checking)
        availableUpdate != null -> tankobunString(R.string.about_update_available, availableUpdate.versionName)
        update != null -> tankobunString(R.string.about_update_current)
        else -> tankobunString(R.string.about_update_not_checked)
    }
    Text(tankobunString(R.string.about_updates), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(statusText, style = MaterialTheme.typography.bodyMedium)
    if (state.appUpdateLastCheckedAtEpochMillis > 0L) {
        Text(
            tankobunString(R.string.about_update_last_checked, cacheAgeLabel(state.appUpdateLastCheckedAtEpochMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    update?.sizeBytes?.takeIf { it > 0L }?.let { size ->
        Text(
            tankobunString(R.string.about_update_size, size.formatFileSize()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        tankobunString(R.string.about_update_policy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.appUpdateCheckInProgress || state.appUpdateDownloadInProgress) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    TankobunActionButton(
        label = if (state.appUpdateCheckInProgress) {
            tankobunString(R.string.about_update_checking)
        } else {
            tankobunString(R.string.about_check_updates)
        },
        icon = Icons.Default.Refresh,
        onClick = viewModel::checkForAppUpdate,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.appUpdateCheckInProgress && !state.appUpdateDownloadInProgress,
        filled = false,
    )
    if (updateAvailable) {
        TankobunActionButton(
            label = if (state.appUpdateDownloadInProgress) {
                tankobunString(R.string.about_downloading_update)
            } else {
                tankobunString(R.string.about_download_update)
            },
            icon = Icons.Default.Download,
            onClick = { requestAppUpdateDownload(context, viewModel) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.appUpdateCheckInProgress && !state.appUpdateDownloadInProgress,
        )
        availableUpdate.releaseUrl?.takeIf { it.isNotBlank() }?.let { releaseUrl ->
            TankobunActionButton(
                label = tankobunString(R.string.about_open_release),
                icon = Icons.Default.Link,
                onClick = { onOpenRelease(releaseUrl) },
                modifier = Modifier.fillMaxWidth(),
                filled = false,
            )
        }
    }
}

@Composable
private fun AboutChangelogContent(state: TankobunUiState) {
    val context = LocalContext.current
    val update = state.appUpdateInfo
    val availableUpdate = update?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    val translationLanguage = state.appLanguage.resolvedChangelogTranslationLanguage()
    val updateChangelogEnglish = availableUpdate?.englishChangelog().orEmpty()
    val updateChangelogTranslated = availableUpdate?.translatedChangelog(translationLanguage).orEmpty()
    val currentChangelogEnglish = currentVersionChangelog(context, AppLanguage.ENGLISH)
    val currentChangelogTranslated = translationLanguage
        ?.let { currentVersionChangelog(context, it) }
        .orEmpty()
        .takeIf { it != currentChangelogEnglish }
        .orEmpty()
    Text(tankobunString(R.string.about_changelog), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (availableUpdate != null && updateChangelogEnglish.isNotEmpty()) {
        ChangelogList(
            title = tankobunString(R.string.about_changelog_update_english_title, availableUpdate.versionName),
            items = updateChangelogEnglish,
        )
    }
    if (availableUpdate != null && updateChangelogTranslated.isNotEmpty() && updateChangelogTranslated != updateChangelogEnglish) {
        ChangelogList(
            title = tankobunString(
                R.string.about_changelog_translated_title,
                translationLanguage?.let { tankobunString(it.changelogLanguageLabelRes()) }.orEmpty(),
            ),
            items = updateChangelogTranslated,
        )
    }
    ChangelogList(
        title = tankobunString(R.string.about_changelog_current_english_title, BuildConfig.VERSION_NAME),
        items = currentChangelogEnglish,
    )
    if (translationLanguage != null && currentChangelogTranslated.isNotEmpty()) {
        ChangelogList(
            title = tankobunString(
                R.string.about_changelog_translated_title,
                tankobunString(translationLanguage.changelogLanguageLabelRes()),
            ),
            items = currentChangelogTranslated,
        )
    }
}

@Composable
private fun ChangelogList(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        items.forEach { item ->
            Text(
                text = tankobunString(R.string.about_changelog_bullet, item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun currentVersionChangelog(context: Context, language: AppLanguage): List<String> {
    val localized = context.withAppLanguage(language)
    return listOf(
        localized.getString(R.string.about_changelog_v2_local_library),
        localized.getString(R.string.about_changelog_v2_onboarding),
        localized.getString(R.string.about_changelog_v2_backups),
        localized.getString(R.string.about_changelog_v2_updates),
    )
}

private fun AppUpdateInfo.englishChangelog(): List<String> =
    changelog["en"].orEmpty()

private fun AppUpdateInfo.translatedChangelog(language: AppLanguage?): List<String> {
    val preferredKeys = when (language) {
        AppLanguage.PORTUGUESE_BRAZIL -> listOf("pt-BR", "pt")
        AppLanguage.SPANISH -> listOf("es")
        else -> emptyList()
    }
    return preferredKeys.firstNotNullOfOrNull { key ->
        changelog[key]?.takeIf { it.isNotEmpty() }
    }.orEmpty()
}

private fun AppLanguage.resolvedChangelogTranslationLanguage(): AppLanguage? =
    when (this) {
        AppLanguage.PORTUGUESE_BRAZIL -> AppLanguage.PORTUGUESE_BRAZIL
        AppLanguage.SPANISH -> AppLanguage.SPANISH
        AppLanguage.ENGLISH -> null
        AppLanguage.SYSTEM -> when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "pt" -> AppLanguage.PORTUGUESE_BRAZIL
            "es" -> AppLanguage.SPANISH
            else -> null
        }
    }

private fun AppLanguage.changelogLanguageLabelRes(): Int =
    when (this) {
        AppLanguage.PORTUGUESE_BRAZIL -> R.string.settings_language_portuguese_brazil
        AppLanguage.SPANISH -> R.string.settings_language_spanish
        AppLanguage.ENGLISH,
        AppLanguage.SYSTEM -> R.string.settings_language_english
    }

internal fun requestAppUpdateDownload(
    context: Context,
    viewModel: MainViewModel,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        viewModel.requireAppUpdateInstallPermission()
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ),
        )
        return
    }
    viewModel.downloadAppUpdate()
}

internal fun downloadedAppUpdateInstallIntent(installRequest: AppUpdateInstallRequest): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(installRequest.apkUri), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

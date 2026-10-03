package com.tankobun.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tankobun.app.ui.shell.LocalTankobunChromeInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Pull to refresh for screens that scroll under the glass top bar. The spinner stays up until
 * [working] settles, with a short minimum so a cache hit still reads as a refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TankobunPullToRefresh(
    working: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val latestWorking by rememberUpdatedState(working)
    LaunchedEffect(refreshing) {
        if (!refreshing) return@LaunchedEffect
        delay(MinimumRefreshMillis)
        snapshotFlow { latestWorking }.first { !it }
        refreshing = false
    }
    val pullState = rememberPullToRefreshState()
    val topInset = LocalTankobunChromeInsets.current.top
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
        },
        modifier = modifier,
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        content = content,
    )
}

private const val MinimumRefreshMillis = 600L

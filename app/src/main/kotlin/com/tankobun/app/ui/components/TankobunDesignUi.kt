package com.tankobun.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.TankobunDisplayFontFamily
import java.util.Locale

/*
 * Tankobun UI style guide:
 * Prefer these semantic primitives in screens instead of raw colors/radii.
 * Use raw RoundedCornerShape only for image-specific masks or intentionally circular badges.
 */

@Composable
internal fun TankobunScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedPadding = contentPadding ?: PaddingValues(LocalTankobunStyle.current.spacing.compactScreenPadding)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalTankobunStyle.current.colors.backdrop)
            .padding(resolvedPadding),
        verticalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.section),
        content = content,
    )
}

@Composable
internal fun TankobunPanel(
    modifier: Modifier = Modifier,
    color: Color = LocalTankobunStyle.current.colors.panel,
    contentColor: Color = LocalTankobunStyle.current.colors.panelContent,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content,
    )
}

@Composable
internal fun TankobunSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.item),
    ) {
        val titleModifier = if (actionLabel != null && onAction != null) {
            Modifier.weight(1f)
        } else {
            Modifier.weight(1f, fill = false)
        }
        Text(
            text = title.uppercase(Locale.getDefault()),
            modifier = titleModifier,
            style = LocalTankobunStyle.current.typography.sectionLabel,
            color = LocalTankobunStyle.current.colors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = LocalTankobunStyle.current.colors.mutedContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun TankobunMessageBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    TankobunPanel(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun TankobunEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    TankobunPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.item),
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = LocalTankobunStyle.current.colors.accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalTankobunStyle.current.colors.mutedContent,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TankobunActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control)
    val content: @Composable () -> Unit = {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = LocalTankobunStyle.current.sizes.iconAction),
            shape = shape,
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalTankobunStyle.current.colors.action,
                contentColor = LocalTankobunStyle.current.colors.actionContent,
            ),
            content = { content() },
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = LocalTankobunStyle.current.sizes.iconAction),
            shape = shape,
            contentPadding = PaddingValues(horizontal = 14.dp),
            content = { content() },
        )
    }
}

@Composable
internal fun TankobunIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val size = LocalTankobunStyle.current.sizes.iconAction
    val shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control)
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(size),
            shape = shape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalTankobunStyle.current.colors.action,
                contentColor = LocalTankobunStyle.current.colors.actionContent,
            ),
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(19.dp))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(size),
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun TankobunSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    showSearchAction: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            Row {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
                if (showSearchAction) {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TankobunFilterRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.dense),
        verticalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.dense),
    ) {
        content()
    }
}

@Composable
internal fun TankobunChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        colors = tankobunFilterChipColors(),
        leadingIcon = leadingIcon,
        label = label,
    )
}

@Composable
internal fun TankobunTag(
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = onClick?.let { modifier.clickable(onClick = it) } ?: modifier
    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.control),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 6.dp else 7.dp,
            ),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun tankobunFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = LocalTankobunStyle.current.colors.chip,
    labelColor = LocalTankobunStyle.current.colors.chipContent,
    iconColor = LocalTankobunStyle.current.colors.mutedContent,
    selectedContainerColor = LocalTankobunStyle.current.colors.selectedChip,
    selectedLabelColor = LocalTankobunStyle.current.colors.selectedChipContent,
    selectedLeadingIconColor = LocalTankobunStyle.current.colors.selectedChipContent,
    selectedTrailingIconColor = LocalTankobunStyle.current.colors.selectedChipContent,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
)

@Composable
internal fun TankobunDialogSurface(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 560.dp,
    maxHeight: Dp = 640.dp,
    fillMaxHeightFraction: Float? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val heightModifier = fillMaxHeightFraction?.let { Modifier.fillMaxHeight(it) } ?: Modifier.heightIn(max = maxHeight)
    val columnModifier = if (scrollable) {
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    } else {
        Modifier.padding(18.dp)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = maxWidth)
            .then(heightModifier),
        shape = RoundedCornerShape(LocalTankobunStyle.current.radii.panel),
        color = LocalTankobunStyle.current.colors.panel,
        contentColor = LocalTankobunStyle.current.colors.panelContent,
        tonalElevation = 3.dp,
    ) {
        Column(
            columnModifier,
            verticalArrangement = Arrangement.spacedBy(LocalTankobunStyle.current.spacing.item),
            content = content,
        )
    }
}

@Composable
internal fun TankobunDialog(
    onDismiss: () -> Unit,
    maxWidth: Dp = 560.dp,
    maxHeight: Dp = 640.dp,
    fillMaxHeightFraction: Float? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TankobunDialogSurface(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            fillMaxHeightFraction = fillMaxHeightFraction,
            scrollable = scrollable,
            content = content,
        )
    }
}

@Composable
internal fun TankobunDialogHeader(title: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
internal fun TankobunMediaStatusLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(Locale.getDefault()),
        modifier = modifier,
        style = LocalTankobunStyle.current.typography.compactStatus,
        color = LocalTankobunStyle.current.colors.accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun tankobunMangaTitleTextStyle(fontSize: Float): TextStyle =
    TextStyle(
        fontFamily = TankobunDisplayFontFamily,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 0.84f).sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        hyphens = Hyphens.None,
        lineBreak = LineBreak.Heading,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

@Composable
internal fun FlowRowCompat(content: @Composable () -> Unit) {
    TankobunFilterRow(content = content)
}

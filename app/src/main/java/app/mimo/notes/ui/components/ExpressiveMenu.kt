package app.mimo.notes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.theme.LocalInterfaceBlur

private val LocalMenuBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/**
 * Material 3 Expressive menus are split into rounded panels. The frost is a
 * blurred snapshot of the app, clipped to each panel, so the panel background
 * is blurred while the app around the menu stays sharp.
 */
@Composable
fun ExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appView = LocalView.current
    val glassRoot = LocalGlassRoot.current ?: appView.rootView
    val blurEnabled = LocalInterfaceBlur.current
    var backdrop by remember { mutableStateOf<GlassBackdrop?>(null) }
    LaunchedEffect(expanded, glassRoot, blurEnabled) {
        if (expanded && blurEnabled) {
            backdrop = captureGlassBackdrop(glassRoot)
        } else {
            backdrop = null
        }
    }
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 220.dp, max = 244.dp),
    ) {
        CompositionLocalProvider(LocalMenuBackdrop provides backdrop) {
            content()
        }
    }
}

@Composable
fun ExpressiveMenuGroup(
    index: Int,
    count: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = groupShape(index, count)
    val base = MenuDefaults.groupStandardContainerColor
    val glowPrimary = MaterialTheme.colorScheme.primary
    val glowSecondary = MaterialTheme.colorScheme.secondary
    val backdrop = LocalMenuBackdrop.current
    val blurEnabled = LocalInterfaceBlur.current
    val popupView = LocalView.current
    var panelScreen by remember { mutableStateOf<Rect?>(null) }

    DropdownMenuGroup(
        shapes = MenuDefaults.groupShapes(shape = shape, inactiveShape = shape),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .onGloballyPositioned { coordinates ->
                    val location = IntArray(2)
                    popupView.rootView.getLocationOnScreen(location)
                    val position = coordinates.positionInRoot()
                    panelScreen = Rect(
                        left = location[0] + position.x,
                        top = location[1] + position.y,
                        right = location[0] + position.x + coordinates.size.width,
                        bottom = location[1] + position.y + coordinates.size.height,
                    )
                }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            base.copy(alpha = 0.98f),
                            base.copy(alpha = 0.94f),
                        )
                    )
                )
        ) {
            val screen = panelScreen
            if (blurEnabled && backdrop != null && screen != null) {
                GlassBackdropLayer(
                    backdrop = backdrop,
                    panel = screen,
                    blurRadius = 30.dp,
                    tint = Brush.verticalGradient(
                        listOf(
                            base.copy(alpha = 0.58f),
                            base.copy(alpha = 0.70f),
                        )
                    ),
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Canvas(Modifier.matchParentSize()) {
                    val radius = size.minDimension * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowPrimary.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.10f, size.height * 0.02f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(size.width * 0.10f, size.height * 0.02f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowSecondary.copy(alpha = 0.11f), Color.Transparent),
                            center = Offset(size.width * 0.95f, size.height * 0.95f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(size.width * 0.95f, size.height * 0.95f),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(vertical = 6.dp),
                content = content,
            )
        }
    }
    if (index < count - 1) {
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun ExpressiveMenuItem(
    icon: ImageVector,
    label: String,
    index: Int,
    count: Int,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showDivider: Boolean = index > 0,
    onClick: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    val colors = if (destructive) {
        MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error,
        )
    } else {
        MenuDefaults.itemColors()
    }
    Column(Modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            )
        }
        DropdownMenuItem(
            onClick = {
                haptics.tap()
                onClick()
            },
            text = { Text(label, style = MaterialTheme.typography.labelLarge) },
            shape = itemShape(index, count),
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                )
            },
            enabled = enabled,
            colors = colors,
        )
    }
}

private val GroupRadius = 34.dp
private val GroupInnerRadius = 20.dp
private val ItemRadius = 24.dp
private val ItemInnerRadius = 14.dp

private fun groupShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(GroupRadius)
    index == 0 -> RoundedCornerShape(
        topStart = GroupRadius,
        topEnd = GroupRadius,
        bottomStart = GroupInnerRadius,
        bottomEnd = GroupInnerRadius,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = GroupInnerRadius,
        topEnd = GroupInnerRadius,
        bottomStart = GroupRadius,
        bottomEnd = GroupRadius,
    )
    else -> RoundedCornerShape(GroupInnerRadius)
}

private fun itemShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(ItemRadius)
    index == 0 -> RoundedCornerShape(
        topStart = ItemRadius,
        topEnd = ItemRadius,
        bottomStart = ItemInnerRadius,
        bottomEnd = ItemInnerRadius,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = ItemInnerRadius,
        topEnd = ItemInnerRadius,
        bottomStart = ItemRadius,
        bottomEnd = ItemRadius,
    )
    else -> RoundedCornerShape(ItemInnerRadius)
}

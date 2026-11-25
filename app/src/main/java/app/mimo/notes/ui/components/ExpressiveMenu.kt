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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.mimo.notes.ui.LocalMimoHaptics

/**
 * Material 3 Expressive menus are split into rounded, frosted panels instead
 * of one long sheet. The frost is painted inside each panel: the app behind
 * stays sharp and only the panel's own background glows are blurred.
 */
@Composable
fun ExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 220.dp, max = 244.dp),
        content = content,
    )
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lerp(base, Color.White, 0.04f),
                            lerp(base, Color.Black, 0.03f),
                        )
                    )
                )
        ) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .blur(14.dp)
            ) {
                val radius = size.minDimension * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.02f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.10f, size.height * 0.02f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.95f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.95f, size.height * 0.95f),
                )
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

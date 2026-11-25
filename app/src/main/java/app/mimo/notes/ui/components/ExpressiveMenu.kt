package app.mimo.notes.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.mimo.notes.ui.LocalMimoHaptics

val LocalNoteMenuVisibility = staticCompositionLocalOf<(Long, Boolean) -> Unit> { { _, _ -> } }

/**
 * Material 3 Expressive menus are split into rounded, translucent groups
 * instead of one long sheet. The content behind the menu is blurred by the
 * host screen, so the panels read as frosted glass.
 */
@Composable
fun ExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 220.dp, max = 244.dp),
        shape = RoundedCornerShape(0.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
    val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    DropdownMenuGroup(
        shapes = MenuDefaults.groupShapes(shape = shape, inactiveShape = shape),
        containerColor = MenuDefaults.groupStandardContainerColor.copy(
            alpha = if (blurAvailable) 0.82f else 0.96f,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        ),
        content = content,
    )
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

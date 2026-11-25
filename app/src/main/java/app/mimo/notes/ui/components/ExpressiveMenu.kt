package app.mimo.notes.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.mimo.notes.ui.LocalMimoHaptics

/**
 * Material 3 Expressive menus are split into rounded groups instead of one
 * long sheet. Each group morphs its item corners and the groups are separated
 * by a small gap.
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
        modifier = modifier.widthIn(min = 264.dp, max = 320.dp),
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
    DropdownMenuGroup(
        shapes = MenuDefaults.groupShape(index, count),
        content = content,
    )
    if (index < count - 1) {
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
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
    DropdownMenuItem(
        onClick = {
            haptics.tap()
            onClick()
        },
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = MenuDefaults.itemShape(index, count).shape,
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

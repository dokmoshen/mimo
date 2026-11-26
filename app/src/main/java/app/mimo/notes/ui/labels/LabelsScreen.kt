package app.mimo.notes.ui.labels

import app.mimo.notes.ui.components.MimoIcons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mimo.notes.R
import app.mimo.notes.data.Label
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.CircleActionButton
import app.mimo.notes.ui.components.ConfirmDialog
import app.mimo.notes.ui.components.EmptyState
import app.mimo.notes.ui.components.LabelEditDialog
import app.mimo.notes.ui.components.MimoIllustration
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.TransparentBottomFog
import app.mimo.notes.ui.components.TransparentScreenHeader
import app.mimo.notes.ui.components.TransparentTopFog
import app.mimo.notes.ui.components.transparentHeaderHeight
import app.mimo.notes.ui.notes.NotesViewModel
import app.mimo.notes.ui.theme.LabelColors
import app.mimo.notes.ui.util.formatNoteCount
import androidx.compose.ui.platform.LocalContext

@Composable
fun LabelsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onOpenLabel: (Label) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val counts = remember(notes) {
        notes.filter { !it.isArchived }.flatMap { note -> note.labels.map { it.id } }
            .groupingBy { it }.eachCount()
    }
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Label?>(null) }
    var deleting by remember { mutableStateOf<Label?>(null) }

    BackHandler(onBack = onBack)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = transparentHeaderHeight(statusBarTop, hasSubtitle = true)

    MimoBackdrop {
        Box(Modifier.fillMaxSize()) {
            if (labels.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = headerHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    EmptyState(
                        title = stringResource(R.string.labels_empty_title),
                        body = stringResource(R.string.labels_empty_body),
                        icon = MimoIcons.Label,
                        actionLabel = stringResource(R.string.new_label),
                        onAction = { showCreate = true },
                        illustration = MimoIllustration.LABELS,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = headerHeight + 8.dp,
                        bottom = 200.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(labels, key = { it.id }) { label ->
                        LabelRow(
                            label = label,
                            count = counts[label.id] ?: 0,
                            onClick = { onOpenLabel(label) },
                            onEdit = { editing = label },
                            onDelete = { deleting = label },
                        )
                    }
                    item(key = "create") {
                        val haptics = LocalMimoHaptics.current
                        Surface(
                            onClick = {
                                haptics.press()
                                showCreate = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    MimoIcons.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.new_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            TransparentBottomFog(Modifier.align(Alignment.BottomCenter))
            TransparentTopFog(headerHeight, Modifier.align(Alignment.TopCenter))
            TransparentScreenHeader(
                title = stringResource(R.string.labels_title),
                subtitle = stringResource(R.string.labels_empty_body),
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    CircleActionButton(
                        icon = MimoIcons.Add,
                        contentDescription = stringResource(R.string.new_label),
                        onClick = { showCreate = true },
                    )
                },
            )
        }
    }

    if (showCreate) {
        LabelEditDialog(
            onDismiss = { showCreate = false },
            onSave = { name, colorId ->
                if (labels.any { it.name.equals(name, ignoreCase = true) }) {
                    onMessage(context.getString(R.string.label_already_exists))
                } else {
                    viewModel.addLabel(name, colorId)
                    showCreate = false
                }
            },
        )
    }

    editing?.let { label ->
        LabelEditDialog(
            initialName = label.name,
            initialColorId = label.colorId,
            title = stringResource(R.string.rename_label),
            onDismiss = { editing = null },
            onSave = { name, colorId ->
                if (labels.any { it.id != label.id && it.name.equals(name, ignoreCase = true) }) {
                    onMessage(context.getString(R.string.label_already_exists))
                } else {
                    viewModel.updateLabel(label.id, name, colorId)
                    editing = null
                }
            },
        )
    }

    deleting?.let { label ->
        ConfirmDialog(
            title = stringResource(R.string.delete_label_confirm, label.name),
            body = stringResource(R.string.delete_label_confirm_body),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.deleteLabel(label.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun LabelRow(
    label: Label,
    count: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.press()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LabelColors.container(label.colorId), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(13.dp)
                        .background(LabelColors.accent(label.colorId), CircleShape)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    formatNoteCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = {
                    haptics.tap()
                    menu = true
                }) {
                    Icon(
                        MimoIcons.MoreHoriz,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_label)) },
                        leadingIcon = { Icon(MimoIcons.Edit, contentDescription = null) },
                        onClick = {
                            haptics.tap()
                            menu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_label)) },
                        leadingIcon = { Icon(MimoIcons.Delete, contentDescription = null) },
                        onClick = {
                            haptics.reject()
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

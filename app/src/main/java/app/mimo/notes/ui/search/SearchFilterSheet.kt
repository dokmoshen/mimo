package app.mimo.notes.ui.search

import app.mimo.notes.ui.components.MimoIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.data.Label
import app.mimo.notes.data.NoteType
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.MimoChip
import app.mimo.notes.ui.notes.NoteFilter
import app.mimo.notes.ui.theme.LabelColors
import app.mimo.notes.ui.theme.NoteColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    initial: NoteFilter,
    labels: List<app.mimo.notes.data.Label>,
    onDismiss: () -> Unit,
    onApply: (NoteFilter) -> Unit,
) {
    var types by remember { mutableStateOf(initial.types) }
    var colors by remember { mutableStateOf(initial.colorIds) }
    var labelIds by remember { mutableStateOf(initial.labelIds) }
    var withReminder by remember { mutableStateOf(initial.withReminder) }
    var withImages by remember { mutableStateOf(initial.withImages) }
    var includeArchived by remember { mutableStateOf(initial.includeArchived) }

    val haptics = LocalMimoHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.search_filters),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    haptics.tap()
                    types = emptySet()
                    colors = emptySet()
                    labelIds = emptySet()
                    withReminder = false
                    withImages = false
                    includeArchived = false
                }) { Text(stringResource(R.string.reset)) }
            }
            FilterSectionTitleLocal(stringResource(R.string.filter_type))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterTypeChipLocal(stringResource(R.string.type_text), NoteType.TEXT in types) {
                    types = types.toggle(NoteType.TEXT)
                }
                FilterTypeChipLocal(stringResource(R.string.type_checklist), NoteType.CHECKLIST in types) {
                    types = types.toggle(NoteType.CHECKLIST)
                }
                FilterTypeChipLocal(stringResource(R.string.type_drawing), NoteType.DRAWING in types) {
                    types = types.toggle(NoteType.DRAWING)
                }
                FilterTypeChipLocal(stringResource(R.string.type_audio), NoteType.AUDIO in types) {
                    types = types.toggle(NoteType.AUDIO)
                }
                FilterTypeChipLocal(stringResource(R.string.type_images), withImages) {
                    withImages = !withImages
                }
            }
            FilterSectionTitleLocal(stringResource(R.string.filter_color))
            NoteColors.all.chunked(6).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(NoteColors.background(color.id), CircleShape)
                                .border(
                                    width = if (color.id in colors) 2.dp else 1.dp,
                                    color = if (color.id in colors) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    haptics.toggle(color.id !in colors)
                                    colors = if (color.id in colors) colors - color.id else colors + color.id
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color.id in colors) {
                                Icon(
                                    MimoIcons.Check,
                                    contentDescription = stringResource(color.nameRes),
                                    tint = NoteColors.content(),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (labels.isNotEmpty()) {
                FilterSectionTitleLocal(stringResource(R.string.filter_labels))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.forEach { label ->
                        MimoChip(
                            label = label.name,
                            selected = label.id in labelIds,
                            onClick = {
                                labelIds = if (label.id in labelIds) labelIds - label.id
                                else labelIds + label.id
                            },
                            leadingColor = app.mimo.notes.ui.theme.LabelColors.accent(label.colorId),
                        )
                    }
                }
            }
            FilterToggleLocal(
                title = stringResource(R.string.filter_reminder),
                checked = withReminder,
                onCheckedChange = { withReminder = it },
            )
            FilterToggleLocal(
                title = stringResource(R.string.show_archived),
                checked = includeArchived,
                onCheckedChange = { includeArchived = it },
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    onApply(
                        NoteFilter(
                            types = types,
                            colorIds = colors,
                            labelIds = labelIds,
                            withReminder = withReminder,
                            withImages = withImages,
                            includeArchived = includeArchived,
                        )
                    )
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.apply), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterSectionTitleLocal(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun FilterTypeChipLocal(label: String, selected: Boolean, onClick: () -> Unit) {
    MimoChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun FilterToggleLocal(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptics = LocalMimoHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.toggle(it)
                onCheckedChange(it)
            },
        )
    }
}


private fun Set<NoteType>.toggle(type: NoteType): Set<NoteType> =
    if (type in this) this - type else this + type

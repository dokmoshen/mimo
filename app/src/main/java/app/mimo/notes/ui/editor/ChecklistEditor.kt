package app.mimo.notes.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.data.ChecklistItem
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.InkStampBurst
import app.mimo.notes.ui.util.formatDuration

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    content: Color,
    muted: Color,
    backgroundColor: Color,
    onTextChange: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (String?) -> String,
    onMove: (String, Int) -> Unit,
    onIndent: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusFirst: Boolean = false,
) {
    var focusId by remember {
        mutableStateOf<String?>(if (autoFocusFirst) items.firstOrNull()?.id else null)
    }
    val checked = items.count { it.isChecked }
    val progress by animateFloatAsState(
        targetValue = if (items.isEmpty()) 0f else checked.toFloat() / items.size,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "checklistProgress",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp),
                    color = content.copy(alpha = 0.55f),
                    trackColor = content.copy(alpha = 0.12f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "$checked / ${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }
        items.forEach { item ->
            ChecklistRow(
                item = item,
                content = content,
                muted = muted,
                backgroundColor = backgroundColor,
                requestFocus = focusId == item.id,
                onFocusHandled = { if (focusId == item.id) focusId = null },
                onTextChange = { onTextChange(item.id, it) },
                onToggle = { onToggle(item.id) },
                onRemove = {
                    onRemove(item.id)
                    focusId = null
                },
                onAddAfter = {
                    focusId = onAdd(item.id)
                },
                onMoveUp = { onMove(item.id, -1) },
                onMoveDown = { onMove(item.id, 1) },
                onIndent = { onIndent(item.id) },
            )
            Spacer(Modifier.height(4.dp))
        }
        val addHaptics = LocalMimoHaptics.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .border(
                    1.dp,
                    content.copy(alpha = 0.16f),
                    RoundedCornerShape(16.dp),
                )
                .clickable {
                    addHaptics.tap()
                    focusId = onAdd(items.lastOrNull()?.id)
                }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .border(1.3.dp, content.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = content.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.add_checklist_item),
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    content: Color,
    muted: Color,
    backgroundColor: Color,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onAddAfter: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onIndent: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalMimoHaptics.current
    val checkFill by animateFloatAsState(
        targetValue = if (item.isChecked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "checklistFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (item.isChecked) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checklistCheck",
    )
    val rowBackground by animateColorAsState(
        targetValue = content.copy(alpha = if (item.isChecked) 0.075f else 0.045f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checklistRowBackground",
    )

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            runCatching { focusRequester.requestFocus() }
            onFocusHandled()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.indent * 18).dp)
            .background(rowBackground, RoundedCornerShape(16.dp))
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(21.dp)) {
            InkStampBurst(
                checked = item.isChecked,
                color = content,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .size(21.dp)
                    .border(
                        1.5.dp,
                        content.copy(alpha = if (item.isChecked) 0.8f else 0.35f),
                        CircleShape,
                    )
                    .toggleable(
                        value = item.isChecked,
                        onValueChange = {
                            haptics.toggle(!item.isChecked)
                            onToggle()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = checkFill
                            scaleY = checkFill
                        }
                        .background(content.copy(alpha = 0.88f), CircleShape)
                )
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = backgroundColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                        .alpha(checkFill),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.isChecked) muted else content,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(content),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { onAddAfter() },
                onDone = { onAddAfter() },
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box {
                    if (item.text.isEmpty()) {
                        Text(
                            stringResource(R.string.add_checklist_item),
                            style = MaterialTheme.typography.bodyLarge,
                            color = muted.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
        )
        Box {
            IconButton(
                onClick = {
                    haptics.tap()
                    menuExpanded = true
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    tint = muted.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_up)) },
                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        menuExpanded = false
                        onMoveUp()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_down)) },
                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        menuExpanded = false
                        onMoveDown()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.indent)) },
                    leadingIcon = { Icon(Icons.Rounded.FormatIndentIncrease, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        menuExpanded = false
                        onIndent()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_item)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = {
                        haptics.reject()
                        menuExpanded = false
                        onRemove()
                    },
                )
            }
        }
    }
}

package app.mimo.notes.ui.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mimo.notes.R
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.EmptyState
import app.mimo.notes.ui.components.LocalImage
import app.mimo.notes.ui.components.MimoIllustration
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.SectionHeader
import app.mimo.notes.ui.components.TransparentBottomFog
import app.mimo.notes.ui.components.TransparentScreenHeader
import app.mimo.notes.ui.components.TransparentTopFog
import app.mimo.notes.ui.components.transparentHeaderHeight
import app.mimo.notes.ui.notes.NotesViewModel
import app.mimo.notes.ui.theme.NoteColors
import app.mimo.notes.ui.util.formatReminder
import app.mimo.notes.ui.util.isOverdue
import app.mimo.notes.ui.util.previewLine
import app.mimo.notes.ui.util.reminderDay
import app.mimo.notes.ui.util.startOfToday
import java.time.LocalDate

@Composable
fun RemindersScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val reminders = remember(notes) {
        notes.filter { !it.isArchived && it.reminderAt != null }
            .sortedBy { it.reminderAt }
    }
    val today = startOfToday()
    val overdue = reminders.filter { isOverdue(it.reminderAt!!) }
    val todayNotes = reminders.filter {
        !isOverdue(it.reminderAt!!) && reminderDay(it.reminderAt!!) == today
    }
    val tomorrowNotes = reminders.filter {
        !isOverdue(it.reminderAt!!) && reminderDay(it.reminderAt!!) == today.plusDays(1)
    }
    val later = reminders.filter {
        val day = reminderDay(it.reminderAt!!)
        day > today.plusDays(1)
    }
    val groups = listOf(
        Triple(stringResource(R.string.overdue), overdue, true),
        Triple(stringResource(R.string.today), todayNotes, false),
        Triple(stringResource(R.string.tomorrow), tomorrowNotes, false),
        Triple(stringResource(R.string.later), later, false),
    ).filter { it.second.isNotEmpty() }

    BackHandler(onBack = onBack)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val remindersSubtitle = if (reminders.isEmpty()) {
        stringResource(R.string.reminders_empty_body)
    } else {
        app.mimo.notes.ui.util.formatRemindersCount(reminders.size)
    }
    val headerHeight = transparentHeaderHeight(statusBarTop, hasSubtitle = true)

    MimoBackdrop {
        Box(Modifier.fillMaxSize()) {
            if (reminders.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = headerHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    EmptyState(
                        title = stringResource(R.string.reminders_empty_title),
                        body = stringResource(R.string.reminders_empty_body),
                        icon = Icons.Rounded.NotificationsNone,
                        illustration = MimoIllustration.REMINDERS,
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
                    groups.forEach { (title, groupNotes, isOverdueGroup) ->
                        item(key = "header_$title") {
                            SectionHeader(
                                title = title,
                                icon = if (isOverdueGroup) Icons.Rounded.NotificationsNone
                                else Icons.Rounded.Notifications,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(groupNotes, key = { "rem_${it.id}" }) { note ->
                            ReminderRow(
                                note = note,
                                overdue = isOverdue(note.reminderAt!!),
                                onOpen = { onOpenNote(note.id) },
                                onDone = { viewModel.clearReminder(note) },
                            )
                        }
                    }
                }
            }

            TransparentBottomFog(Modifier.align(Alignment.BottomCenter))
            TransparentTopFog(headerHeight, Modifier.align(Alignment.TopCenter))
            TransparentScreenHeader(
                title = stringResource(R.string.reminders_title),
                subtitle = remindersSubtitle,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun ReminderRow(
    note: Note,
    overdue: Boolean,
    onOpen: () -> Unit,
    onDone: () -> Unit,
) {
    val background = NoteColors.background(note.colorId)
    val content = NoteColors.content()
    val muted = NoteColors.mutedContent()
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.press()
            onOpen()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = background,
        border = BorderStroke(1.dp, NoteColors.cardBorder()),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(44.dp)
                    .background(
                        if (overdue) MaterialTheme.colorScheme.error else content.copy(alpha = 0.35f),
                        CircleShape,
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (note.isLocked) stringResource(R.string.locked_note)
                    else note.title.ifBlank { stringResource(R.string.no_title) },
                    style = MaterialTheme.typography.titleMedium,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (note.isLocked) stringResource(R.string.locked_note_hint)
                    else previewLine(note, maxLines = 1).ifBlank {
                        noteTypePlaceholder(note.type)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = if (overdue) MaterialTheme.colorScheme.error else muted,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatReminder(
                            androidx.compose.ui.platform.LocalContext.current,
                            note.reminderAt!!,
                            note.reminderRepeat,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else muted,
                    )
                }
            }
            if (note.images.isNotEmpty()) {
                LocalImage(
                    path = note.images.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    haptics.confirm()
                    onDone()
                },
                modifier = Modifier.size(42.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = content.copy(alpha = 0.08f),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.mark_done),
                            tint = content.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun noteTypePlaceholder(type: NoteType): String = when (type) {
    NoteType.CHECKLIST -> stringResource(R.string.type_checklist)
    NoteType.DRAWING -> stringResource(R.string.type_drawing)
    NoteType.AUDIO -> stringResource(R.string.type_audio)
    NoteType.TEXT -> ""
}

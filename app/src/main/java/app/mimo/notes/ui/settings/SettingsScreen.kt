package app.mimo.notes.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mimo.notes.BuildConfig
import app.mimo.notes.R
import app.mimo.notes.importer.KeepImportResult
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.preferences.NoteLayout
import app.mimo.notes.preferences.PreferencesRepository
import app.mimo.notes.preferences.SortMode
import app.mimo.notes.preferences.ThemeMode
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.ConfirmDialog
import app.mimo.notes.ui.components.MimoChip
import app.mimo.notes.ui.components.MimoLogo
import app.mimo.notes.ui.components.PinEntryDialog
import app.mimo.notes.ui.components.SectionHeader
import app.mimo.notes.ui.components.TransparentBottomFog
import app.mimo.notes.ui.components.TransparentScreenHeader
import app.mimo.notes.ui.components.TransparentTopFog
import app.mimo.notes.ui.components.transparentHeaderHeight
import app.mimo.notes.util.BiometricAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun SettingsScreen(
    settings: MimoSettings,
    preferences: PreferencesRepository,
    onBack: () -> Unit,
    onImportKeep: suspend (Uri) -> KeepImportResult,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    val rawHaptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var sortDialog by remember { mutableStateOf(false) }
    var pinSetup by remember { mutableStateOf(false) }
    var pinManage by remember { mutableStateOf(false) }
    var pinRemove by remember { mutableStateOf(false) }
    var importBusy by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<KeepImportResult?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                importBusy = true
                importResult = null
                importError = null
                try {
                    importResult = onImportKeep(uri)
                    haptics.confirm()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    haptics.reject()
                    importError = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.import_keep_empty)
                } finally {
                    importBusy = false
                }
            }
        }
    }
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    var exactAlarmsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager?.canScheduleExactAlarms() == true
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactAlarmsAllowed = alarmManager?.canScheduleExactAlarms() == true
        }
    }
    BackHandler(enabled = !importBusy, onBack = onBack)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = transparentHeaderHeight(statusBarTop, hasSubtitle = false)

    MimoBackdrop {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = headerHeight + 6.dp,
                    bottom = 200.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionHeader(stringResource(R.string.settings_appearance))
                    SettingsCard {
                        SettingTitle(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.settings_theme),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeTile(
                                icon = Icons.Rounded.BrightnessAuto,
                                label = stringResource(R.string.theme_system),
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { preferences.setThemeMode(ThemeMode.SYSTEM) } },
                            )
                            ThemeTile(
                                icon = Icons.Rounded.LightMode,
                                label = stringResource(R.string.theme_light),
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { preferences.setThemeMode(ThemeMode.LIGHT) } },
                            )
                            ThemeTile(
                                icon = Icons.Rounded.DarkMode,
                                label = stringResource(R.string.theme_dark),
                                selected = settings.themeMode == ThemeMode.DARK,
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { preferences.setThemeMode(ThemeMode.DARK) } },
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SettingsDivider()
                            SwitchRow(
                                icon = Icons.Rounded.Palette,
                                title = stringResource(R.string.dynamic_color),
                                subtitle = stringResource(R.string.dynamic_color_desc),
                                checked = settings.dynamicColor,
                                onCheckedChange = {
                                    scope.launch { preferences.setDynamicColor(it) }
                                },
                            )
                        }
                        SettingsDivider()
                        SwitchRow(
                            icon = Icons.Rounded.Vibration,
                            title = stringResource(R.string.haptics),
                            subtitle = stringResource(R.string.haptics_desc),
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    rawHaptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                }
                                scope.launch { preferences.setHapticsEnabled(enabled) }
                            },
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = if (settings.layout == NoteLayout.GRID) Icons.Rounded.GridView
                            else Icons.Rounded.ViewAgenda,
                            title = stringResource(R.string.settings_layout),
                            subtitle = stringResource(
                                if (settings.layout == NoteLayout.GRID) R.string.layout_grid
                                else R.string.layout_list
                            ),
                            onClick = {
                                scope.launch {
                                    preferences.setLayout(
                                        if (settings.layout == NoteLayout.GRID) NoteLayout.LIST
                                        else NoteLayout.GRID
                                    )
                                }
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.settings_notes))
                    SettingsCard {
                        SettingRow(
                            icon = Icons.Rounded.Sort,
                            title = stringResource(R.string.settings_sort),
                            subtitle = stringResource(
                                when (settings.sortMode) {
                                    SortMode.MODIFIED -> R.string.sort_modified
                                    SortMode.CREATED -> R.string.sort_created
                                    SortMode.TITLE -> R.string.sort_title
                                }
                            ),
                            onClick = { sortDialog = true },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        SettingsDivider()
                        SwitchRow(
                            icon = Icons.Rounded.Sort,
                            title = stringResource(R.string.sort_ascending),
                            checked = settings.sortAscending,
                            onCheckedChange = {
                                scope.launch { preferences.setSortAscending(it) }
                            },
                        )
                        SettingsDivider()
                        SwitchRow(
                            icon = Icons.Rounded.Check,
                            title = stringResource(R.string.move_checked),
                            subtitle = stringResource(R.string.move_checked_desc),
                            checked = settings.moveCheckedToBottom,
                            onCheckedChange = {
                                scope.launch { preferences.setMoveCheckedToBottom(it) }
                            },
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.settings_data))
                    SettingsCard {
                        SettingRow(
                            icon = Icons.Rounded.FileOpen,
                            title = stringResource(R.string.import_keep),
                            subtitle = stringResource(R.string.import_keep_desc),
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                        "text/html",
                                        "application/json",
                                    )
                                )
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = stringResource(R.string.settings_trash_auto),
                            subtitle = stringResource(R.string.settings_trash_auto_desc),
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = Icons.Rounded.Storage,
                            title = stringResource(R.string.storage_local),
                            subtitle = stringResource(R.string.storage_local_desc),
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.settings_security))
                    SettingsCard {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SettingRow(
                                icon = Icons.Rounded.Alarm,
                                title = stringResource(R.string.exact_alarms),
                                subtitle = stringResource(
                                    if (exactAlarmsAllowed) R.string.exact_alarms_allowed
                                    else R.string.exact_alarms_not_allowed
                                ),
                                onClick = {
                                    val intent = Intent(
                                        AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                    runCatching { context.startActivity(intent) }
                                },
                                trailing = {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                            SettingsDivider()
                        }
                        SettingRow(
                            icon = Icons.Rounded.Fingerprint,
                            title = stringResource(R.string.security_note_lock),
                            subtitle = stringResource(
                                if (BiometricAuth.canAuthenticate(context) || settings.hasNotePin) {
                                    R.string.security_note_lock_desc
                                } else {
                                    R.string.unlock_unavailable
                                }
                            ),
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = Icons.Rounded.Lock,
                            title = stringResource(R.string.pin_settings_title),
                            subtitle = stringResource(
                                if (settings.hasNotePin) R.string.pin_settings_enabled
                                else R.string.pin_settings_disabled
                            ),
                            onClick = {
                                if (settings.hasNotePin) pinManage = true else pinSetup = true
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.settings_about))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        ),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(54.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MimoLogo(
                                        Modifier.size(28.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        accent = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "mimo",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 23.sp,
                                        ),
                                    )
                                    Text(
                                        stringResource(R.string.brand_tagline),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.about_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Rounded.FontDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.about_fonts),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.license_copyright),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                stringResource(R.string.license_summary),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"),
                                            )
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp),
                            ) {
                                Text(
                                    stringResource(R.string.license_open),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
            ) {
                TransparentBottomFog()
            }

            Box(
                Modifier
                    .align(Alignment.TopCenter)
            ) {
                TransparentTopFog(headerHeight)
            }

            TransparentScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (importBusy) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(30.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.import_keep)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.import_keep_running))
                }
            },
            confirmButton = {},
        )
    } else {
        importResult?.let { result ->
            AlertDialog(
                onDismissRequest = { importResult = null },
                shape = RoundedCornerShape(30.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { Text(stringResource(R.string.import_keep_done)) },
                text = {
                    Column {
                        Text(stringResource(R.string.import_keep_notes, result.notes))
                        if (result.labels > 0) {
                            Text(stringResource(R.string.import_keep_labels, result.labels))
                        }
                        if (result.attachments > 0) {
                            Text(stringResource(R.string.import_keep_attachments, result.attachments))
                        }
                        if (result.skipped > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.import_keep_skipped, result.skipped),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { importResult = null }) {
                        Text(stringResource(R.string.done))
                    }
                },
            )
        }
        importError?.let { error ->
            AlertDialog(
                onDismissRequest = { importError = null },
                shape = RoundedCornerShape(30.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { Text(stringResource(R.string.import_keep_failed)) },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { importError = null }) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }
    }

    if (pinSetup) {
        PinEntryDialog(
            title = stringResource(R.string.pin_setup_title),
            subtitle = stringResource(R.string.pin_setup_subtitle),
            requireConfirmation = true,
            onDismiss = { pinSetup = false },
            onConfirm = { pin ->
                haptics.confirm()
                scope.launch {
                    preferences.setNotePin(pin)
                    pinSetup = false
                }
            },
        )
    }

    if (pinManage) {
        AlertDialog(
            onDismissRequest = { pinManage = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.pin_settings_title)) },
            text = { Text(stringResource(R.string.pin_settings_enabled)) },
            confirmButton = {
                TextButton(onClick = {
                    pinManage = false
                    pinSetup = true
                }) { Text(stringResource(R.string.pin_settings_change)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pinManage = false
                    pinRemove = true
                }) { Text(stringResource(R.string.pin_settings_remove)) }
            },
        )
    }

    if (pinRemove) {
        ConfirmDialog(
            title = stringResource(R.string.pin_remove_title),
            body = stringResource(R.string.pin_remove_body),
            confirmLabel = stringResource(R.string.pin_settings_remove),
            destructive = true,
            onConfirm = {
                pinRemove = false
                scope.launch { preferences.clearNotePin() }
            },
            onDismiss = { pinRemove = false },
        )
    }

    if (sortDialog) {
        AlertDialog(
            onDismissRequest = { sortDialog = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.settings_sort)) },
            text = {
                Column {
                    SortMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.sortMode == mode,
                                onClick = {
                                    haptics.toggle(true)
                                    scope.launch { preferences.setSortMode(mode) }
                                    sortDialog = false
                                },
                            )
                            Text(
                                stringResource(
                                    when (mode) {
                                        SortMode.MODIFIED -> R.string.sort_modified
                                        SortMode.CREATED -> R.string.sort_created
                                        SortMode.TITLE -> R.string.sort_title
                                    }
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sortDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 14.dp), content = content)
    }
}

@Composable
private fun SettingTitle(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val haptics = LocalMimoHaptics.current
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier.clickable {
                    haptics.tap()
                    onClick()
                }
            } else {
                Modifier
            }
        )
        .padding(vertical = 8.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val haptics = LocalMimoHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && (checked == null || onCheckedChange == null)) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.toggle(it)
                    onCheckedChange(it)
                },
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ThemeTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.toggle(!selected)
            onClick()
        },
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        } else null,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp, top = 2.dp, bottom = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

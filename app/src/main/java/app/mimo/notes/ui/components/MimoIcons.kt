package app.mimo.notes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A semantic icon set. The Nothing theme swaps the rounded Material icons for
 * their outlined counterparts, giving the UI a thinner glyph-like language.
 */
interface MimoIconSet {
    val Add: ImageVector
    val Alarm: ImageVector
    val Archive: ImageVector
    val ArrowBack: ImageVector
    val ArrowDownward: ImageVector
    val ArrowUpward: ImageVector
    val AudioFile: ImageVector
    val AutoAwesome: ImageVector
    val AutoFixHigh: ImageVector
    val AutoFixOff: ImageVector
    val BlurOn: ImageVector
    val BrightnessAuto: ImageVector
    val Brush: ImageVector
    val CalendarMonth: ImageVector
    val Check: ImageVector
    val CheckCircle: ImageVector
    val Checklist: ImageVector
    val ChevronRight: ImageVector
    val Close: ImageVector
    val ColorLens: ImageVector
    val ContentCopy: ImageVector
    val DarkMode: ImageVector
    val Delete: ImageVector
    val DeleteForever: ImageVector
    val DeleteSweep: ImageVector
    val Edit: ImageVector
    val FileDownload: ImageVector
    val FileOpen: ImageVector
    val Fingerprint: ImageVector
    val FontDownload: ImageVector
    val FormatBold: ImageVector
    val FormatIndentIncrease: ImageVector
    val FormatItalic: ImageVector
    val FormatListBulleted: ImageVector
    val FormatListNumbered: ImageVector
    val FormatStrikethrough: ImageVector
    val FormatUnderlined: ImageVector
    val GraphicEq: ImageVector
    val GridView: ImageVector
    val Highlight: ImageVector
    val History: ImageVector
    val Image: ImageVector
    val Info: ImageVector
    val Inventory2: ImageVector
    val Label: ImageVector
    val LightMode: ImageVector
    val Lock: ImageVector
    val LockOpen: ImageVector
    val Menu: ImageVector
    val MenuBook: ImageVector
    val Mic: ImageVector
    val MoreHoriz: ImageVector
    val MoreVert: ImageVector
    val Notes: ImageVector
    val Notifications: ImageVector
    val NotificationsActive: ImageVector
    val NotificationsNone: ImageVector
    val Palette: ImageVector
    val Pause: ImageVector
    val PhotoCamera: ImageVector
    val PlayArrow: ImageVector
    val PushPin: ImageVector
    val Redo: ImageVector
    val Restore: ImageVector
    val Schedule: ImageVector
    val Search: ImageVector
    val SearchOff: ImageVector
    val SelectAll: ImageVector
    val Settings: ImageVector
    val Share: ImageVector
    val Sort: ImageVector
    val StickyNote2: ImageVector
    val Stop: ImageVector
    val Storage: ImageVector
    val SystemUpdate: ImageVector
    val Title: ImageVector
    val Tune: ImageVector
    val Unarchive: ImageVector
    val Undo: ImageVector
    val Vibration: ImageVector
    val ViewAgenda: ImageVector
    val Visibility: ImageVector
    val VisibilityOff: ImageVector
}

internal object RoundedMimoIcons : MimoIconSet {
    override val Add: ImageVector get() = Icons.Rounded.Add
    override val Alarm: ImageVector get() = Icons.Rounded.Alarm
    override val Archive: ImageVector get() = Icons.Rounded.Archive
    override val ArrowBack: ImageVector get() = Icons.Rounded.ArrowBack
    override val ArrowDownward: ImageVector get() = Icons.Rounded.ArrowDownward
    override val ArrowUpward: ImageVector get() = Icons.Rounded.ArrowUpward
    override val AudioFile: ImageVector get() = Icons.Rounded.AudioFile
    override val AutoAwesome: ImageVector get() = Icons.Rounded.AutoAwesome
    override val AutoFixHigh: ImageVector get() = Icons.Rounded.AutoFixHigh
    override val AutoFixOff: ImageVector get() = Icons.Rounded.AutoFixOff
    override val BlurOn: ImageVector get() = Icons.Rounded.BlurOn
    override val BrightnessAuto: ImageVector get() = Icons.Rounded.BrightnessAuto
    override val Brush: ImageVector get() = Icons.Rounded.Brush
    override val CalendarMonth: ImageVector get() = Icons.Rounded.CalendarMonth
    override val Check: ImageVector get() = Icons.Rounded.Check
    override val CheckCircle: ImageVector get() = Icons.Rounded.CheckCircle
    override val Checklist: ImageVector get() = Icons.Rounded.Checklist
    override val ChevronRight: ImageVector get() = Icons.Rounded.ChevronRight
    override val Close: ImageVector get() = Icons.Rounded.Close
    override val ColorLens: ImageVector get() = Icons.Rounded.ColorLens
    override val ContentCopy: ImageVector get() = Icons.Rounded.ContentCopy
    override val DarkMode: ImageVector get() = Icons.Rounded.DarkMode
    override val Delete: ImageVector get() = Icons.Rounded.Delete
    override val DeleteForever: ImageVector get() = Icons.Rounded.DeleteForever
    override val DeleteSweep: ImageVector get() = Icons.Rounded.DeleteSweep
    override val Edit: ImageVector get() = Icons.Rounded.Edit
    override val FileDownload: ImageVector get() = Icons.Rounded.FileDownload
    override val FileOpen: ImageVector get() = Icons.Rounded.FileOpen
    override val Fingerprint: ImageVector get() = Icons.Rounded.Fingerprint
    override val FontDownload: ImageVector get() = Icons.Rounded.FontDownload
    override val FormatBold: ImageVector get() = Icons.Rounded.FormatBold
    override val FormatIndentIncrease: ImageVector get() = Icons.Rounded.FormatIndentIncrease
    override val FormatItalic: ImageVector get() = Icons.Rounded.FormatItalic
    override val FormatListBulleted: ImageVector get() = Icons.Rounded.FormatListBulleted
    override val FormatListNumbered: ImageVector get() = Icons.Rounded.FormatListNumbered
    override val FormatStrikethrough: ImageVector get() = Icons.Rounded.FormatStrikethrough
    override val FormatUnderlined: ImageVector get() = Icons.Rounded.FormatUnderlined
    override val GraphicEq: ImageVector get() = Icons.Rounded.GraphicEq
    override val GridView: ImageVector get() = Icons.Rounded.GridView
    override val Highlight: ImageVector get() = Icons.Rounded.Highlight
    override val History: ImageVector get() = Icons.Rounded.History
    override val Image: ImageVector get() = Icons.Rounded.Image
    override val Info: ImageVector get() = Icons.Rounded.Info
    override val Inventory2: ImageVector get() = Icons.Rounded.Inventory2
    override val Label: ImageVector get() = Icons.Rounded.Label
    override val LightMode: ImageVector get() = Icons.Rounded.LightMode
    override val Lock: ImageVector get() = Icons.Rounded.Lock
    override val LockOpen: ImageVector get() = Icons.Rounded.LockOpen
    override val Menu: ImageVector get() = Icons.Rounded.Menu
    override val MenuBook: ImageVector get() = Icons.Rounded.MenuBook
    override val Mic: ImageVector get() = Icons.Rounded.Mic
    override val MoreHoriz: ImageVector get() = Icons.Rounded.MoreHoriz
    override val MoreVert: ImageVector get() = Icons.Rounded.MoreVert
    override val Notes: ImageVector get() = Icons.Rounded.Notes
    override val Notifications: ImageVector get() = Icons.Rounded.Notifications
    override val NotificationsActive: ImageVector get() = Icons.Rounded.NotificationsActive
    override val NotificationsNone: ImageVector get() = Icons.Rounded.NotificationsNone
    override val Palette: ImageVector get() = Icons.Rounded.Palette
    override val Pause: ImageVector get() = Icons.Rounded.Pause
    override val PhotoCamera: ImageVector get() = Icons.Rounded.PhotoCamera
    override val PlayArrow: ImageVector get() = Icons.Rounded.PlayArrow
    override val PushPin: ImageVector get() = Icons.Rounded.PushPin
    override val Redo: ImageVector get() = Icons.Rounded.Redo
    override val Restore: ImageVector get() = Icons.Rounded.Restore
    override val Schedule: ImageVector get() = Icons.Rounded.Schedule
    override val Search: ImageVector get() = Icons.Rounded.Search
    override val SearchOff: ImageVector get() = Icons.Rounded.SearchOff
    override val SelectAll: ImageVector get() = Icons.Rounded.SelectAll
    override val Settings: ImageVector get() = Icons.Rounded.Settings
    override val Share: ImageVector get() = Icons.Rounded.Share
    override val Sort: ImageVector get() = Icons.Rounded.Sort
    override val StickyNote2: ImageVector get() = Icons.Rounded.StickyNote2
    override val Stop: ImageVector get() = Icons.Rounded.Stop
    override val Storage: ImageVector get() = Icons.Rounded.Storage
    override val SystemUpdate: ImageVector get() = Icons.Rounded.SystemUpdate
    override val Title: ImageVector get() = Icons.Rounded.Title
    override val Tune: ImageVector get() = Icons.Rounded.Tune
    override val Unarchive: ImageVector get() = Icons.Rounded.Unarchive
    override val Undo: ImageVector get() = Icons.Rounded.Undo
    override val Vibration: ImageVector get() = Icons.Rounded.Vibration
    override val ViewAgenda: ImageVector get() = Icons.Rounded.ViewAgenda
    override val Visibility: ImageVector get() = Icons.Rounded.Visibility
    override val VisibilityOff: ImageVector get() = Icons.Rounded.VisibilityOff
}

internal object OutlinedMimoIcons : MimoIconSet {
    override val Add: ImageVector get() = Icons.Outlined.Add
    override val Alarm: ImageVector get() = Icons.Outlined.Alarm
    override val Archive: ImageVector get() = Icons.Outlined.Archive
    override val ArrowBack: ImageVector get() = Icons.Outlined.ArrowBack
    override val ArrowDownward: ImageVector get() = Icons.Outlined.ArrowDownward
    override val ArrowUpward: ImageVector get() = Icons.Outlined.ArrowUpward
    override val AudioFile: ImageVector get() = Icons.Outlined.AudioFile
    override val AutoAwesome: ImageVector get() = Icons.Outlined.AutoAwesome
    override val AutoFixHigh: ImageVector get() = Icons.Outlined.AutoFixHigh
    override val AutoFixOff: ImageVector get() = Icons.Outlined.AutoFixOff
    override val BlurOn: ImageVector get() = Icons.Outlined.BlurOn
    override val BrightnessAuto: ImageVector get() = Icons.Outlined.BrightnessAuto
    override val Brush: ImageVector get() = Icons.Outlined.Brush
    override val CalendarMonth: ImageVector get() = Icons.Outlined.CalendarMonth
    override val Check: ImageVector get() = Icons.Outlined.Check
    override val CheckCircle: ImageVector get() = Icons.Outlined.CheckCircle
    override val Checklist: ImageVector get() = Icons.Outlined.Checklist
    override val ChevronRight: ImageVector get() = Icons.Outlined.ChevronRight
    override val Close: ImageVector get() = Icons.Outlined.Close
    override val ColorLens: ImageVector get() = Icons.Outlined.ColorLens
    override val ContentCopy: ImageVector get() = Icons.Outlined.ContentCopy
    override val DarkMode: ImageVector get() = Icons.Outlined.DarkMode
    override val Delete: ImageVector get() = Icons.Outlined.Delete
    override val DeleteForever: ImageVector get() = Icons.Outlined.DeleteForever
    override val DeleteSweep: ImageVector get() = Icons.Outlined.DeleteSweep
    override val Edit: ImageVector get() = Icons.Outlined.Edit
    override val FileDownload: ImageVector get() = Icons.Outlined.FileDownload
    override val FileOpen: ImageVector get() = Icons.Outlined.FileOpen
    override val Fingerprint: ImageVector get() = Icons.Outlined.Fingerprint
    override val FontDownload: ImageVector get() = Icons.Outlined.FontDownload
    override val FormatBold: ImageVector get() = Icons.Outlined.FormatBold
    override val FormatIndentIncrease: ImageVector get() = Icons.Outlined.FormatIndentIncrease
    override val FormatItalic: ImageVector get() = Icons.Outlined.FormatItalic
    override val FormatListBulleted: ImageVector get() = Icons.Outlined.FormatListBulleted
    override val FormatListNumbered: ImageVector get() = Icons.Outlined.FormatListNumbered
    override val FormatStrikethrough: ImageVector get() = Icons.Outlined.FormatStrikethrough
    override val FormatUnderlined: ImageVector get() = Icons.Outlined.FormatUnderlined
    override val GraphicEq: ImageVector get() = Icons.Outlined.GraphicEq
    override val GridView: ImageVector get() = Icons.Outlined.GridView
    override val Highlight: ImageVector get() = Icons.Outlined.Highlight
    override val History: ImageVector get() = Icons.Outlined.History
    override val Image: ImageVector get() = Icons.Outlined.Image
    override val Info: ImageVector get() = Icons.Outlined.Info
    override val Inventory2: ImageVector get() = Icons.Outlined.Inventory2
    override val Label: ImageVector get() = Icons.Outlined.Label
    override val LightMode: ImageVector get() = Icons.Outlined.LightMode
    override val Lock: ImageVector get() = Icons.Outlined.Lock
    override val LockOpen: ImageVector get() = Icons.Outlined.LockOpen
    override val Menu: ImageVector get() = Icons.Outlined.Menu
    override val MenuBook: ImageVector get() = Icons.Outlined.MenuBook
    override val Mic: ImageVector get() = Icons.Outlined.Mic
    override val MoreHoriz: ImageVector get() = Icons.Outlined.MoreHoriz
    override val MoreVert: ImageVector get() = Icons.Outlined.MoreVert
    override val Notes: ImageVector get() = Icons.Outlined.Notes
    override val Notifications: ImageVector get() = Icons.Outlined.Notifications
    override val NotificationsActive: ImageVector get() = Icons.Outlined.NotificationsActive
    override val NotificationsNone: ImageVector get() = Icons.Outlined.NotificationsNone
    override val Palette: ImageVector get() = Icons.Outlined.Palette
    override val Pause: ImageVector get() = Icons.Outlined.Pause
    override val PhotoCamera: ImageVector get() = Icons.Outlined.PhotoCamera
    override val PlayArrow: ImageVector get() = Icons.Outlined.PlayArrow
    override val PushPin: ImageVector get() = Icons.Outlined.PushPin
    override val Redo: ImageVector get() = Icons.Outlined.Redo
    override val Restore: ImageVector get() = Icons.Outlined.Restore
    override val Schedule: ImageVector get() = Icons.Outlined.Schedule
    override val Search: ImageVector get() = Icons.Outlined.Search
    override val SearchOff: ImageVector get() = Icons.Outlined.SearchOff
    override val SelectAll: ImageVector get() = Icons.Outlined.SelectAll
    override val Settings: ImageVector get() = Icons.Outlined.Settings
    override val Share: ImageVector get() = Icons.Outlined.Share
    override val Sort: ImageVector get() = Icons.Outlined.Sort
    override val StickyNote2: ImageVector get() = Icons.Outlined.StickyNote2
    override val Stop: ImageVector get() = Icons.Outlined.Stop
    override val Storage: ImageVector get() = Icons.Outlined.Storage
    override val SystemUpdate: ImageVector get() = Icons.Outlined.SystemUpdate
    override val Title: ImageVector get() = Icons.Outlined.Title
    override val Tune: ImageVector get() = Icons.Outlined.Tune
    override val Unarchive: ImageVector get() = Icons.Outlined.Unarchive
    override val Undo: ImageVector get() = Icons.Outlined.Undo
    override val Vibration: ImageVector get() = Icons.Outlined.Vibration
    override val ViewAgenda: ImageVector get() = Icons.Outlined.ViewAgenda
    override val Visibility: ImageVector get() = Icons.Outlined.Visibility
    override val VisibilityOff: ImageVector get() = Icons.Outlined.VisibilityOff
}

val LocalMimoIcons = staticCompositionLocalOf<MimoIconSet> { RoundedMimoIcons }

object MimoIcons {
    val Add: ImageVector
        @Composable get() = LocalMimoIcons.current.Add
    val Alarm: ImageVector
        @Composable get() = LocalMimoIcons.current.Alarm
    val Archive: ImageVector
        @Composable get() = LocalMimoIcons.current.Archive
    val ArrowBack: ImageVector
        @Composable get() = LocalMimoIcons.current.ArrowBack
    val ArrowDownward: ImageVector
        @Composable get() = LocalMimoIcons.current.ArrowDownward
    val ArrowUpward: ImageVector
        @Composable get() = LocalMimoIcons.current.ArrowUpward
    val AudioFile: ImageVector
        @Composable get() = LocalMimoIcons.current.AudioFile
    val AutoAwesome: ImageVector
        @Composable get() = LocalMimoIcons.current.AutoAwesome
    val AutoFixHigh: ImageVector
        @Composable get() = LocalMimoIcons.current.AutoFixHigh
    val AutoFixOff: ImageVector
        @Composable get() = LocalMimoIcons.current.AutoFixOff
    val BlurOn: ImageVector
        @Composable get() = LocalMimoIcons.current.BlurOn
    val BrightnessAuto: ImageVector
        @Composable get() = LocalMimoIcons.current.BrightnessAuto
    val Brush: ImageVector
        @Composable get() = LocalMimoIcons.current.Brush
    val CalendarMonth: ImageVector
        @Composable get() = LocalMimoIcons.current.CalendarMonth
    val Check: ImageVector
        @Composable get() = LocalMimoIcons.current.Check
    val CheckCircle: ImageVector
        @Composable get() = LocalMimoIcons.current.CheckCircle
    val Checklist: ImageVector
        @Composable get() = LocalMimoIcons.current.Checklist
    val ChevronRight: ImageVector
        @Composable get() = LocalMimoIcons.current.ChevronRight
    val Close: ImageVector
        @Composable get() = LocalMimoIcons.current.Close
    val ColorLens: ImageVector
        @Composable get() = LocalMimoIcons.current.ColorLens
    val ContentCopy: ImageVector
        @Composable get() = LocalMimoIcons.current.ContentCopy
    val DarkMode: ImageVector
        @Composable get() = LocalMimoIcons.current.DarkMode
    val Delete: ImageVector
        @Composable get() = LocalMimoIcons.current.Delete
    val DeleteForever: ImageVector
        @Composable get() = LocalMimoIcons.current.DeleteForever
    val DeleteSweep: ImageVector
        @Composable get() = LocalMimoIcons.current.DeleteSweep
    val Edit: ImageVector
        @Composable get() = LocalMimoIcons.current.Edit
    val FileDownload: ImageVector
        @Composable get() = LocalMimoIcons.current.FileDownload
    val FileOpen: ImageVector
        @Composable get() = LocalMimoIcons.current.FileOpen
    val Fingerprint: ImageVector
        @Composable get() = LocalMimoIcons.current.Fingerprint
    val FontDownload: ImageVector
        @Composable get() = LocalMimoIcons.current.FontDownload
    val FormatBold: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatBold
    val FormatIndentIncrease: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatIndentIncrease
    val FormatItalic: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatItalic
    val FormatListBulleted: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatListBulleted
    val FormatListNumbered: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatListNumbered
    val FormatStrikethrough: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatStrikethrough
    val FormatUnderlined: ImageVector
        @Composable get() = LocalMimoIcons.current.FormatUnderlined
    val GraphicEq: ImageVector
        @Composable get() = LocalMimoIcons.current.GraphicEq
    val GridView: ImageVector
        @Composable get() = LocalMimoIcons.current.GridView
    val Highlight: ImageVector
        @Composable get() = LocalMimoIcons.current.Highlight
    val History: ImageVector
        @Composable get() = LocalMimoIcons.current.History
    val Image: ImageVector
        @Composable get() = LocalMimoIcons.current.Image
    val Info: ImageVector
        @Composable get() = LocalMimoIcons.current.Info
    val Inventory2: ImageVector
        @Composable get() = LocalMimoIcons.current.Inventory2
    val Label: ImageVector
        @Composable get() = LocalMimoIcons.current.Label
    val LightMode: ImageVector
        @Composable get() = LocalMimoIcons.current.LightMode
    val Lock: ImageVector
        @Composable get() = LocalMimoIcons.current.Lock
    val LockOpen: ImageVector
        @Composable get() = LocalMimoIcons.current.LockOpen
    val Menu: ImageVector
        @Composable get() = LocalMimoIcons.current.Menu
    val MenuBook: ImageVector
        @Composable get() = LocalMimoIcons.current.MenuBook
    val Mic: ImageVector
        @Composable get() = LocalMimoIcons.current.Mic
    val MoreHoriz: ImageVector
        @Composable get() = LocalMimoIcons.current.MoreHoriz
    val MoreVert: ImageVector
        @Composable get() = LocalMimoIcons.current.MoreVert
    val Notes: ImageVector
        @Composable get() = LocalMimoIcons.current.Notes
    val Notifications: ImageVector
        @Composable get() = LocalMimoIcons.current.Notifications
    val NotificationsActive: ImageVector
        @Composable get() = LocalMimoIcons.current.NotificationsActive
    val NotificationsNone: ImageVector
        @Composable get() = LocalMimoIcons.current.NotificationsNone
    val Palette: ImageVector
        @Composable get() = LocalMimoIcons.current.Palette
    val Pause: ImageVector
        @Composable get() = LocalMimoIcons.current.Pause
    val PhotoCamera: ImageVector
        @Composable get() = LocalMimoIcons.current.PhotoCamera
    val PlayArrow: ImageVector
        @Composable get() = LocalMimoIcons.current.PlayArrow
    val PushPin: ImageVector
        @Composable get() = LocalMimoIcons.current.PushPin
    val Redo: ImageVector
        @Composable get() = LocalMimoIcons.current.Redo
    val Restore: ImageVector
        @Composable get() = LocalMimoIcons.current.Restore
    val Schedule: ImageVector
        @Composable get() = LocalMimoIcons.current.Schedule
    val Search: ImageVector
        @Composable get() = LocalMimoIcons.current.Search
    val SearchOff: ImageVector
        @Composable get() = LocalMimoIcons.current.SearchOff
    val SelectAll: ImageVector
        @Composable get() = LocalMimoIcons.current.SelectAll
    val Settings: ImageVector
        @Composable get() = LocalMimoIcons.current.Settings
    val Share: ImageVector
        @Composable get() = LocalMimoIcons.current.Share
    val Sort: ImageVector
        @Composable get() = LocalMimoIcons.current.Sort
    val StickyNote2: ImageVector
        @Composable get() = LocalMimoIcons.current.StickyNote2
    val Stop: ImageVector
        @Composable get() = LocalMimoIcons.current.Stop
    val Storage: ImageVector
        @Composable get() = LocalMimoIcons.current.Storage
    val SystemUpdate: ImageVector
        @Composable get() = LocalMimoIcons.current.SystemUpdate
    val Title: ImageVector
        @Composable get() = LocalMimoIcons.current.Title
    val Tune: ImageVector
        @Composable get() = LocalMimoIcons.current.Tune
    val Unarchive: ImageVector
        @Composable get() = LocalMimoIcons.current.Unarchive
    val Undo: ImageVector
        @Composable get() = LocalMimoIcons.current.Undo
    val Vibration: ImageVector
        @Composable get() = LocalMimoIcons.current.Vibration
    val ViewAgenda: ImageVector
        @Composable get() = LocalMimoIcons.current.ViewAgenda
    val Visibility: ImageVector
        @Composable get() = LocalMimoIcons.current.Visibility
    val VisibilityOff: ImageVector
        @Composable get() = LocalMimoIcons.current.VisibilityOff
}

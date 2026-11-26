package app.mimo.notes.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.mimo.notes.preferences.DesignLanguage
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.ui.theme.LocalDesignLanguage
import app.mimo.notes.ui.theme.LocalInterfaceBlur

data class NoteColor(
    val id: Int,
    @StringRes val nameRes: Int,
    val light: Color,
    val dark: Color,
)

object NoteColors {

    val all = listOf(
        NoteColor(0, R.string.color_paper, Color(0xFFF0EAE0), Color(0xFF252420)),
        NoteColor(1, R.string.color_blush, Color(0xFFF7DCDA), Color(0xFF3B2525)),
        NoteColor(2, R.string.color_apricot, Color(0xFFFBE1CC), Color(0xFF3C2B1E)),
        NoteColor(3, R.string.color_butter, Color(0xFFF6E9B8), Color(0xFF35301B)),
        NoteColor(4, R.string.color_sage, Color(0xFFDDE9D2), Color(0xFF242F21)),
        NoteColor(5, R.string.color_mint, Color(0xFFCFEAE3), Color(0xFF1E302C)),
        NoteColor(6, R.string.color_sky, Color(0xFFD7E7F5), Color(0xFF1D2A35)),
        NoteColor(7, R.string.color_periwinkle, Color(0xFFE1E1F7), Color(0xFF28263A)),
        NoteColor(8, R.string.color_lilac, Color(0xFFEFDCF2), Color(0xFF322634)),
        NoteColor(9, R.string.color_rose, Color(0xFFF8DDE7), Color(0xFF3A232C)),
        NoteColor(10, R.string.color_clay, Color(0xFFEADDD3), Color(0xFF2F2823)),
        NoteColor(11, R.string.color_moss, Color(0xFFE8EACC), Color(0xFF2B2D20)),
    )

    fun byId(id: Int): NoteColor = all.getOrElse(id) { all.first() }

    private val nothingLight = listOf(
        Color(0xFFF4F4F4), Color(0xFFEDEDED), Color(0xFFE6E6E6), Color(0xFFDEDEDE),
        Color(0xFFF0F0F0), Color(0xFFE9E9E9), Color(0xFFE2E2E2), Color(0xFFDADADA),
        Color(0xFFF7F7F7), Color(0xFFEFEFEF), Color(0xFFE8E8E8), Color(0xFFE0E0E0),
    )

    private val nothingDark = listOf(
        Color(0xFF0A0A0A), Color(0xFF111111), Color(0xFF171717), Color(0xFF1D1D1D),
        Color(0xFF0D0D0D), Color(0xFF141414), Color(0xFF1A1A1A), Color(0xFF202020),
        Color(0xFF080808), Color(0xFF101010), Color(0xFF161616), Color(0xFF1C1C1C),
    )

    @Composable
    private fun baseBackground(id: Int): Color {
        val dark = LocalMimoDark.current
        return if (LocalDesignLanguage.current == DesignLanguage.NOTHING) {
            val palette = if (dark) nothingDark else nothingLight
            palette[id.mod(palette.size)]
        } else {
            val color = byId(id)
            if (dark) color.dark else color.light
        }
    }

    @Composable
    fun background(id: Int): Color {
        val base = baseBackground(id)
        return if (LocalInterfaceBlur.current) base.copy(alpha = 0.78f) else base
    }

    /** The note editor keeps an opaque paper surface even when the UI is glassy. */
    @Composable
    fun editorBackground(id: Int): Color = baseBackground(id)

    @Composable
    fun content(): Color = when {
        LocalDesignLanguage.current == DesignLanguage.NOTHING && LocalMimoDark.current ->
            Color(0xFFF4F4F4)
        LocalDesignLanguage.current == DesignLanguage.NOTHING -> Color(0xFF0A0A0A)
        LocalMimoDark.current -> Color(0xFFF2EEE5)
        else -> Color(0xFF22302A)
    }

    @Composable
    fun mutedContent(): Color = content().copy(alpha = 0.64f)

    @Composable
    fun cardBorder(): Color = when {
        LocalDesignLanguage.current == DesignLanguage.NOTHING ->
            content().copy(alpha = 0.14f)
        LocalMimoDark.current -> Color.White.copy(alpha = 0.07f)
        else -> Color(0xFF23302A).copy(alpha = 0.06f)
    }
}

val NoteCardShape = RoundedCornerShape(
    topStart = 26.dp,
    topEnd = 26.dp,
    bottomEnd = 26.dp,
    bottomStart = 9.dp,
)

val NoteEditorShape = RoundedCornerShape(32.dp)
val DockShape = RoundedCornerShape(34.dp)
val PanelShape = RoundedCornerShape(30.dp)

@Composable
fun noteCardShape(): Shape =
    if (LocalDesignLanguage.current == DesignLanguage.NOTHING) RoundedCornerShape(6.dp)
    else NoteCardShape

@Composable
fun dockShape(): Shape =
    if (LocalDesignLanguage.current == DesignLanguage.NOTHING) RoundedCornerShape(6.dp)
    else DockShape

@Composable
fun panelShape(): Shape =
    if (LocalDesignLanguage.current == DesignLanguage.NOTHING) RoundedCornerShape(8.dp)
    else PanelShape

data class LabelColor(
    val id: Int,
    @StringRes val nameRes: Int,
    val accentLight: Color,
    val accentDark: Color,
    val containerLight: Color,
    val containerDark: Color,
)

object LabelColors {

    val all = listOf(
        LabelColor(0, R.string.label_color_terracotta, Color(0xFFB65C3A), Color(0xFFFFB59A), Color(0xFFF7DCCF), Color(0xFF4A2A1E)),
        LabelColor(1, R.string.label_color_honey, Color(0xFF956A16), Color(0xFFF0C56C), Color(0xFFF7E7BE), Color(0xFF3B2F16)),
        LabelColor(2, R.string.label_color_sage, Color(0xFF507A54), Color(0xFFA5D3A8), Color(0xFFD9E9D6), Color(0xFF233625)),
        LabelColor(3, R.string.label_color_ocean, Color(0xFF2E6E86), Color(0xFF8FD3EA), Color(0xFFCFE7EF), Color(0xFF153440)),
        LabelColor(4, R.string.label_color_indigo, Color(0xFF535A9B), Color(0xFFB9BDF7), Color(0xFFDDE0F8), Color(0xFF24284C)),
        LabelColor(5, R.string.label_color_plum, Color(0xFF7E4A70), Color(0xFFE0A8CF), Color(0xFFEFD9E8), Color(0xFF3C2235)),
        LabelColor(6, R.string.label_color_rose, Color(0xFFA64D63), Color(0xFFF3A7B8), Color(0xFFF7D9E1), Color(0xFF46232D)),
        LabelColor(7, R.string.label_color_graphite, Color(0xFF5A615C), Color(0xFFBDC5BF), Color(0xFFDFE3DF), Color(0xFF2B302C)),
    )

    fun byId(id: Int): LabelColor = all.getOrElse(id) { all.first() }

    private val nothingLightShades = listOf(
        Color(0xFF1F1F1F), Color(0xFF343434), Color(0xFF474747), Color(0xFF5A5A5A),
        Color(0xFF6D6D6D), Color(0xFF808080), Color(0xFF939393), Color(0xFFA6A6A6),
    )

    private val nothingDarkShades = listOf(
        Color(0xFFF2F2F2), Color(0xFFDCDCDC), Color(0xFFC6C6C6), Color(0xFFB0B0B0),
        Color(0xFF9A9A9A), Color(0xFF848484), Color(0xFF6E6E6E), Color(0xFF585858),
    )

    @Composable
    fun accent(id: Int): Color {
        if (LocalDesignLanguage.current == DesignLanguage.NOTHING) {
            val shades = if (LocalMimoDark.current) nothingDarkShades else nothingLightShades
            return shades[id.mod(shades.size)]
        }
        val color = byId(id)
        return if (LocalMimoDark.current) color.accentDark else color.accentLight
    }

    @Composable
    fun container(id: Int): Color {
        if (LocalDesignLanguage.current == DesignLanguage.NOTHING) {
            return if (LocalMimoDark.current) Color(0xFF1C1C1C) else Color(0xFFE6E6E6)
        }
        val color = byId(id)
        return if (LocalMimoDark.current) color.containerDark else color.containerLight
    }

    @Composable
    fun spine(id: Int): Color = accent(id)
}

package app.mimo.notes.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.mimo.notes.R

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

    @Composable
    fun background(id: Int): Color {
        val color = byId(id)
        return if (LocalMimoDark.current) color.dark else color.light
    }

    @Composable
    fun content(): Color =
        if (LocalMimoDark.current) Color(0xFFF2EEE5) else Color(0xFF22302A)

    @Composable
    fun mutedContent(): Color = content().copy(alpha = 0.64f)

    @Composable
    fun cardBorder(): Color =
        if (LocalMimoDark.current) Color.White.copy(alpha = 0.07f)
        else Color(0xFF23302A).copy(alpha = 0.06f)
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

    @Composable
    fun accent(id: Int): Color {
        val color = byId(id)
        return if (LocalMimoDark.current) color.accentDark else color.accentLight
    }

    @Composable
    fun container(id: Int): Color {
        val color = byId(id)
        return if (LocalMimoDark.current) color.containerDark else color.containerLight
    }
}

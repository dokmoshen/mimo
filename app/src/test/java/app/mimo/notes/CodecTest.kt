package app.mimo.notes

import android.graphics.PointF
import app.mimo.notes.data.Drawing
import app.mimo.notes.data.DrawStroke
import app.mimo.notes.data.DrawingCodec
import app.mimo.notes.data.RichSpan
import app.mimo.notes.data.RichTextCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecTest {

    @Test
    fun richSpansRoundTrip() {
        val spans = listOf(
            RichSpan(0, 5, bold = true),
            RichSpan(6, 12, italic = true, underline = true, highlight = true),
            RichSpan(13, 20, strikethrough = true, fontScale = 1.35f),
        )
        val decoded = RichTextCodec.decodeSpans(RichTextCodec.encodeSpans(spans))
        assertEquals(spans.size, decoded.size)
        assertEquals(spans[0], decoded[0])
        assertEquals(spans[1], decoded[1])
        assertEquals(spans[2].fontScale, decoded[2].fontScale, 0.001f)
        assertTrue(decoded[1].highlight)
        assertTrue(decoded[2].strikethrough)
    }

    @Test
    fun invalidSpanJsonIsIgnored() {
        assertTrue(RichTextCodec.decodeSpans("not-json").isEmpty())
        assertTrue(RichTextCodec.decodeSpans("[]").isEmpty())
    }

    @Test
    fun drawingRoundTrip() {
        val drawing = Drawing(
            listOf(
                DrawStroke(
                    points = listOf(PointF(0.1f, 0.2f), PointF(0.3f, 0.4f)),
                    color = 0xFF223344,
                    width = 0.012f,
                ),
                DrawStroke(
                    points = listOf(PointF(0.5f, 0.5f)),
                    color = 0xFFAABBCC,
                    width = 0.02f,
                    eraser = true,
                ),
            )
        )
        val decoded = DrawingCodec.decode(DrawingCodec.encode(drawing))
        assertEquals(2, decoded?.strokes?.size)
        assertEquals(drawing.strokes[0].points.size, decoded?.strokes?.get(0)?.points?.size)
        assertEquals(0.012f, decoded!!.strokes[0].width, 0.0001f)
        assertTrue(decoded.strokes[1].eraser)
    }

    @Test
    fun emptyDrawingIsNotEncoded() {
        assertNull(DrawingCodec.encode(Drawing(emptyList())))
        assertNull(DrawingCodec.decode(null))
    }

    @Test
    fun imagePathsRoundTrip() {
        val paths = listOf("/data/a.jpg", "/data/b.png")
        assertEquals(paths, RichTextCodec.decodePaths(RichTextCodec.encodePaths(paths)))
    }
}

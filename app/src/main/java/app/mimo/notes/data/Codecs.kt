package app.mimo.notes.data

import android.graphics.PointF
import org.json.JSONArray
import org.json.JSONObject

/**
 * Compact serialization for the lightweight rich-text format used by note bodies.
 * Only the attributes Mimo can toggle are persisted, which keeps the database small
 * and makes the stored text portable between versions.
 */
object RichTextCodec {

    fun encodeSpans(spans: List<RichSpan>): String {
        val array = JSONArray()
        spans.filterNot { it.isEmpty }.forEach { span ->
            val json = JSONObject()
                .put("s", span.start)
                .put("e", span.end)
            if (span.bold) json.put("b", true)
            if (span.italic) json.put("i", true)
            if (span.underline) json.put("u", true)
            if (span.strikethrough) json.put("st", true)
            if (span.highlight) json.put("h", true)
            if (span.fontScale != 1f) json.put("f", span.fontScale.toDouble())
            array.put(json)
        }
        return array.toString()
    }

    fun decodeSpans(raw: String): List<RichSpan> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val start = json.optInt("s", 0)
                val end = json.optInt("e", 0)
                if (start < end) {
                    add(
                        RichSpan(
                            start = start,
                            end = end,
                            bold = json.optBoolean("b", false),
                            italic = json.optBoolean("i", false),
                            underline = json.optBoolean("u", false),
                            strikethrough = json.optBoolean("st", false),
                            highlight = json.optBoolean("h", false),
                            fontScale = json.optDouble("f", 1.0).toFloat(),
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    fun encodePaths(paths: List<String>): String {
        val array = JSONArray()
        paths.forEach { array.put(it) }
        return array.toString()
    }

    fun decodePaths(raw: String): List<String> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }.getOrDefault(emptyList())
}

object DrawingCodec {

    fun encode(drawing: Drawing?): String? {
        if (drawing == null || drawing.strokes.isEmpty()) return null
        val root = JSONObject()
        val strokes = JSONArray()
        drawing.strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val points = JSONArray()
            stroke.points.forEach { point ->
                points.put(JSONArray().put(point.x.toDouble()).put(point.y.toDouble()))
            }
            strokes.put(
                JSONObject()
                    .put("c", stroke.color)
                    .put("w", stroke.width.toDouble())
                    .put("e", stroke.eraser)
                    .put("p", points)
            )
        }
        root.put("strokes", strokes)
        return root.toString()
    }

    fun decode(raw: String?): Drawing? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val array = root.optJSONArray("strokes") ?: return null
            val strokes = buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val pointsArray = json.optJSONArray("p") ?: continue
                    val points = buildList {
                        for (pointIndex in 0 until pointsArray.length()) {
                            val pair = pointsArray.optJSONArray(pointIndex) ?: continue
                            add(PointF(pair.optDouble(0, 0.0).toFloat(), pair.optDouble(1, 0.0).toFloat()))
                        }
                    }
                    if (points.isNotEmpty()) {
                        val rawColor = json.optLong("c", SEMANTIC_INK)
                        val color = if (rawColor == 0xFFF2EEE5L ||
                            rawColor == 0xFF22302AL ||
                            rawColor == 0xFF26332DL
                        ) SEMANTIC_INK else rawColor
                        add(
                            DrawStroke(
                                points = points,
                                color = color,
                                width = json.optDouble("w", 4.0).toFloat(),
                                eraser = json.optBoolean("e", false),
                            )
                        )
                    }
                }
            }
            Drawing(strokes)
        }.getOrNull()
    }
}

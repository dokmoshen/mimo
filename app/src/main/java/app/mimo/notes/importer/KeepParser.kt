package app.mimo.notes.importer

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pure parsing of a single Google Keep Takeout note. The modern export ships a
 * JSON file next to the rendered HTML; the JSON is authoritative because it
 * carries labels, colours, flags and the original timestamps. Older exports
 * only contain HTML, so a best-effort HTML reader is kept as a fallback.
 */
enum class KeepColor {
    DEFAULT,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    TEAL,
    BLUE,
    GRAY,
    CERULEAN,
    PURPLE,
    PINK,
    BROWN;

    companion object {
        fun from(value: String?): KeepColor? {
            val clean = value?.trim()?.uppercase() ?: return null
            return entries.firstOrNull { it.name == clean }
        }
    }
}

/** Maps Keep's palette onto the twelve Mimo paper colours. */
fun KeepColor.toMimoColorId(): Int = when (this) {
    KeepColor.DEFAULT -> 0
    KeepColor.RED -> 1
    KeepColor.ORANGE -> 2
    KeepColor.YELLOW -> 3
    KeepColor.GREEN -> 4
    KeepColor.TEAL -> 5
    KeepColor.BLUE, KeepColor.CERULEAN -> 6
    KeepColor.PURPLE -> 8
    KeepColor.PINK -> 9
    KeepColor.BROWN -> 10
    KeepColor.GRAY -> 0
}

data class KeepAttachment(
    val path: String,
    val mimeType: String?,
)

data class KeepChecklistItem(
    val text: String,
    val isChecked: Boolean,
    val indent: Int = 0,
)

data class KeepNote(
    val title: String,
    val text: String,
    val checklist: List<KeepChecklistItem>,
    val labels: List<String>,
    val color: KeepColor,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean,
    val createdAt: Long?,
    val modifiedAt: Long?,
    val attachments: List<KeepAttachment>,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && text.isBlank() &&
            checklist.none { it.text.isNotBlank() } && attachments.isEmpty()
}

object KeepParser {

    private val htmlDatePatterns = listOf(
        "MMM d, yyyy, h:mm:ss a",
        "MMM d, yyyy, h:mm a",
        "MMMM d, yyyy, h:mm:ss a",
        "MMMM d, yyyy, h:mm a",
        "MMM d, yyyy",
        "MMMM d, yyyy",
        "d MMM yyyy, HH:mm",
        "d MMMM yyyy, HH:mm",
        "dd.MM.yyyy HH:mm:ss",
        "dd.MM.yyyy HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif",
    )
    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "3gp", "amr", "wav", "ogg", "opus", "flac",
    )

    /** Parses the `*.json` file from Takeout. Returns null for unrelated JSON. */
    fun parseJson(raw: String): KeepNote? = runCatching {
        val root = JSONObject(raw)
        val hasNoteData = root.has("title") || root.has("textContent") ||
            root.has("listContent") || root.has("attachments")
        if (!hasNoteData) return null

        val checklist = parseJsonChecklist(root.optJSONArray("listContent"))
        val text = normalizeText(root.optString("textContent", ""))
        val createdAt = timestampToMillis(root.opt("createdTimestampUsec"))
        val modifiedAt = timestampToMillis(root.opt("userEditedTimestampUsec")) ?: createdAt
        val note = KeepNote(
            title = normalizeText(root.optString("title", "")).trim(),
            text = if (checklist.isEmpty()) text else "",
            checklist = checklist,
            labels = parseJsonLabels(root.optJSONArray("labels")),
            color = KeepColor.from(root.optString("color", "")) ?: KeepColor.DEFAULT,
            isPinned = root.optBoolean("isPinned", false),
            isArchived = root.optBoolean("isArchived", false),
            isTrashed = root.optBoolean("isTrashed", false),
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            attachments = parseJsonAttachments(root.optJSONArray("attachments")),
        )
        note.takeUnless { it.isEmpty }
    }.getOrNull()

    /**
     * Parses the HTML rendering used by older Takeout archives. Real exports
     * use `div.note`, `div.heading`, `div.title`, `div.content`, list items
     * with a `bullet`/`text` pair, `chips`, and an attachments block.
     */
    fun parseHtml(raw: String, fallbackTime: Long): KeepNote? = runCatching {
        val document = Jsoup.parse(raw)
        val noteElement = document.selectFirst(".note") ?: document.body()
        val hasNoteMarkup = document.selectFirst(".note") != null ||
            noteElement.selectFirst(".content") != null ||
            noteElement.selectFirst(".title") != null
        if (!hasNoteMarkup) return null

        val checklist = parseHtmlChecklist(noteElement)
        val contentElement = noteElement.selectFirst(".content")
        val text = if (checklist.isEmpty() && contentElement != null) {
            htmlText(contentElement)
        } else {
            ""
        }

        val classes = buildSet {
            addAll(noteElement.classNames().map { it.lowercase() })
            document.body()?.classNames()?.forEach { add(it.lowercase()) }
        }
        val color = KeepColor.from(
            classes.firstOrNull { name -> KeepColor.entries.any { it.name.equals(name, true) } }
        ) ?: parseStyleColor(noteElement.attr("style"))
            ?: parseStyleColor(document.body()?.attr("style").orEmpty())
            ?: KeepColor.DEFAULT

        val createdAt = parseKeepDate(
            noteElement.selectFirst(".heading")
                ?.clone()
                ?.also { heading -> heading.select(".meta-icons").remove() }
                ?.text()
        ) ?: fallbackTime

        val note = KeepNote(
            title = normalizeText(
                noteElement.selectFirst(".title")?.text()
                    ?: document.title().takeIf { it.isNotBlank() }
                    ?: ""
            ).trim(),
            text = normalizeText(text),
            checklist = checklist,
            labels = parseHtmlLabels(noteElement),
            color = color,
            isPinned = "pinned" in classes ||
                noteElement.selectFirst(".meta-icons .pinned") != null,
            isArchived = "archived" in classes ||
                noteElement.selectFirst(".meta-icons .archived") != null,
            isTrashed = "trashed" in classes ||
                noteElement.selectFirst(".meta-icons .trashed") != null,
            createdAt = createdAt,
            modifiedAt = createdAt,
            attachments = parseHtmlAttachments(noteElement),
        )
        note.takeUnless { it.isEmpty }
    }.getOrNull()

    /** Exposed for unit tests and for parsing the HTML fragment of a list item. */
    internal fun htmlText(element: Element): String {
        val clone = element.clone()
        clone.select(".chips, .attachments, .meta-icons, script, style").remove()
        clone.select("br").forEach { it.replaceWith(TextNode("\n")) }
        clone.select("p, div, li, tr").forEach { child ->
            if (child !== clone) child.appendText("\n")
        }
        return clone.wholeText()
            .replace('\u00A0', ' ')
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }

    internal fun parseKeepDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val clean = value
            .replace('\u202F', ' ')
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        val locales = listOf(Locale.ENGLISH, Locale.getDefault())
        for (pattern in htmlDatePatterns) {
            for (locale in locales) {
                val parsed = runCatching {
                    SimpleDateFormat(pattern, locale).apply { isLenient = false }.parse(clean)
                }.getOrNull()
                if (parsed != null) return parsed.time
            }
        }
        return null
    }

    private fun parseJsonChecklist(array: JSONArray?): List<KeepChecklistItem> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = normalizeText(item.optString("text", "")).trim()
                    .ifBlank { htmlTextFragment(item.optString("textHtml", "")) }
                if (text.isBlank()) continue
                add(
                    KeepChecklistItem(
                        text = text,
                        isChecked = item.optBoolean("isChecked", false),
                        indent = item.optInt("indent", 0).coerceIn(0, 2),
                    )
                )
            }
        }
    }

    private fun parseHtmlChecklist(root: Element): List<KeepChecklistItem> {
        val items = root.select("li.listitem").takeIf { it.isNotEmpty() }
            ?: root.select(".list li")
        if (items.isEmpty()) return emptyList()
        return buildList {
            items.forEach { item ->
                val textElement = item.selectFirst(".text")
                val text = normalizeText(
                    textElement?.text()
                        ?: item.clone().also { it.select(".bullet").remove() }.text()
                ).trim()
                if (text.isBlank()) return@forEach
                val bullet = item.selectFirst(".bullet")?.text().orEmpty()
                val checked = item.hasClass("checked") ||
                    item.selectFirst("input[type=checkbox]")?.hasAttr("checked") == true ||
                    bullet.contains('☑') || bullet.contains('✓') || bullet.contains('✔')
                add(KeepChecklistItem(text = text, isChecked = checked))
            }
        }
    }

    private fun parseJsonLabels(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.opt(index)
                val name = when (item) {
                    is JSONObject -> item.optString("name", "")
                    is String -> item
                    else -> ""
                }.trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinctBy { it.lowercase(Locale.ROOT) }
    }

    private fun parseHtmlLabels(root: Element): List<String> {
        val elements = root.select(".chip.label").takeIf { it.isNotEmpty() }
            ?: root.select(".labels .label")
        return elements.map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
    }

    private fun parseJsonAttachments(array: JSONArray?): List<KeepAttachment> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val path = item.optString("filePath", "")
                    .ifBlank { item.optString("filepath", "") }
                    .trim()
                if (path.isBlank()) continue
                val mime = item.optString("mimetype", "")
                    .ifBlank { item.optString("mimeType", "") }
                    .takeIf { it.isNotBlank() }
                add(KeepAttachment(path, mime))
            }
        }.distinctBy { it.path }
    }

    private fun parseHtmlAttachments(root: Element): List<KeepAttachment> {
        val result = mutableListOf<KeepAttachment>()
        root.select(".attachments img[src], .content img[src]").forEach { image ->
            val src = image.attr("src").trim()
            if (src.isNotBlank() && !src.startsWith("data:")) {
                result += KeepAttachment(src, mimeFor(src))
            }
        }
        root.select(".attachments audio[src], .attachments source[src], .attachments a[href], .attachments [data-src]")
            .forEach { element ->
                val src = element.attr("src")
                    .ifBlank { element.attr("href") }
                    .ifBlank { element.attr("data-src") }
                    .trim()
                if (src.isNotBlank() && !src.startsWith("data:") && extensionOf(src) in audioExtensions) {
                    result += KeepAttachment(src, mimeFor(src))
                }
            }
        return result.distinctBy { it.path }
    }

    private fun parseStyleColor(style: String): KeepColor? {
        if (style.isBlank()) return null
        val rgb = Regex(
            "background-color\\s*:\\s*rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)",
            RegexOption.IGNORE_CASE,
        ).find(style) ?: return null
        val red = rgb.groupValues[1].toIntOrNull() ?: return null
        val green = rgb.groupValues[2].toIntOrNull() ?: return null
        val blue = rgb.groupValues[3].toIntOrNull() ?: return null
        return when {
            red > 245 && green > 245 && blue > 245 -> KeepColor.DEFAULT
            red > 245 && green in 90..125 && blue in 50..80 -> KeepColor.RED
            red > 245 && green in 140..170 && blue < 20 -> KeepColor.ORANGE
            red > 245 && green > 205 && blue < 20 -> KeepColor.YELLOW
            red in 130..170 && green > 200 && blue < 90 -> KeepColor.GREEN
            red < 50 && green > 215 && blue in 160..200 -> KeepColor.TEAL
            red < 90 && green in 180..210 && blue > 240 -> KeepColor.BLUE
            red in 115..145 && green in 160..190 && blue > 240 -> KeepColor.CERULEAN
            red in 160..195 && green in 120..150 && blue > 240 -> KeepColor.PURPLE
            red > 240 && green in 170..200 && blue in 195..220 -> KeepColor.PINK
            red in 195..230 && green in 190..215 && blue in 185..215 -> KeepColor.BROWN
            red in 170..200 && green in 185..205 && blue in 190..215 -> KeepColor.GRAY
            else -> null
        }
    }

    private fun htmlTextFragment(html: String): String {
        if (html.isBlank()) return ""
        return runCatching { htmlText(Jsoup.parse(html).body()) }.getOrDefault("")
    }

    private fun timestampToMillis(value: Any?): Long? {
        val raw = when (value) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        } ?: return null
        if (raw <= 0) return null
        return when {
            raw > 100_000_000_000_000L -> raw / 1000
            raw > 100_000_000_000L -> raw
            raw > 1_000_000_000L -> raw * 1000
            else -> null
        }
    }

    private fun normalizeText(value: String): String = value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace('\u2028', '\n')
        .replace('\u2029', '\n')
        .trim()

    fun mimeFor(path: String): String? = when (extensionOf(path)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "heic", "heif" -> "image/heic"
        "avif" -> "image/avif"
        "svg" -> "image/svg+xml"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "3gp" -> "audio/3gpp"
        "amr" -> "audio/amr"
        "wav" -> "audio/wav"
        "ogg", "opus" -> "audio/ogg"
        "flac" -> "audio/flac"
        else -> null
    }

    fun isImage(path: String, mimeType: String? = null): Boolean {
        if (mimeType?.startsWith("image/") == true) return mimeType != "image/svg+xml"
        return extensionOf(path) in imageExtensions
    }

    fun isAudio(path: String, mimeType: String? = null): Boolean {
        if (mimeType?.startsWith("audio/") == true) return true
        return extensionOf(path) in audioExtensions
    }

    private fun extensionOf(path: String): String {
        val clean = path.substringBefore('?').substringBefore('#')
        val name = clean.substringAfterLast('/')
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }
}

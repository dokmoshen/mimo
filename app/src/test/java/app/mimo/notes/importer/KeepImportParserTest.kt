package app.mimo.notes.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class KeepImportParserTest {

    @Test
    fun parsesJsonTextNoteWithMetadata() {
        val json = """
            {
              "attachments":[{"filePath":"photo.png","mimetype":"image/png"}],
              "color":"RED",
              "isTrashed":false,
              "isPinned":true,
              "isArchived":false,
              "textContent":"First line\r\nSecond line",
              "title":"Sample Note",
              "userEditedTimestampUsec":1746825715122000,
              "createdTimestampUsec":1746825667793000,
              "labels":[{"name":"work"},{"name":"home"},{"name":"Work"}]
            }
        """.trimIndent()

        val note = KeepParser.parseJson(json)!!

        assertEquals("Sample Note", note.title)
        assertEquals("First line\nSecond line", note.text)
        assertTrue(note.checklist.isEmpty())
        assertEquals(listOf("work", "home"), note.labels)
        assertEquals(KeepColor.RED, note.color)
        assertEquals(1, note.color.toMimoColorId())
        assertTrue(note.isPinned)
        assertFalse(note.isArchived)
        assertFalse(note.isTrashed)
        assertEquals(1746825667793L, note.createdAt!!)
        assertEquals(1746825715122L, note.modifiedAt!!)
        assertEquals(1, note.attachments.size)
        assertEquals("photo.png", note.attachments.first().path)
        assertEquals("image/png", note.attachments.first().mimeType)
    }

    @Test
    fun parsesJsonChecklist() {
        val json = """
            {
              "color":"BLUE",
              "title":"Shopping",
              "listContent":[
                {"text":"Milk","isChecked":false},
                {"text":"Bread","isChecked":true}
              ]
            }
        """.trimIndent()

        val note = KeepParser.parseJson(json)!!

        assertEquals(2, note.checklist.size)
        assertEquals("Milk", note.checklist[0].text)
        assertFalse(note.checklist[0].isChecked)
        assertTrue(note.checklist[1].isChecked)
        assertEquals("", note.text)
        assertEquals(KeepColor.BLUE, note.color)
    }

    @Test
    fun parsesLegacyHtmlChecklist() {
        val html = """
            <html><head><title>Sample List</title></head><body>
            <div class="note RED">
              <div class="heading">
                <div class="meta-icons"><span class="pinned"></span></div>
                May 10, 2025, 12:24:37 AM
              </div>
              <div class="title">Sample List</div>
              <div class="content">
                <ul class="list">
                  <li class="listitem"><span class="bullet">&#9744;</span><span class="text">Item 01</span></li>
                  <li class="listitem checked"><span class="bullet">&#9745;</span><span class="text">Item 02</span></li>
                </ul>
              </div>
              <div class="chips"><span class="chip label">errands</span></div>
              <div class="attachments"><ul><li><img src="photo.png" alt=""></li></ul></div>
            </div>
            </body></html>
        """.trimIndent()

        val parsed = KeepParser.parseHtml(html, 42L)!!

        assertEquals("Sample List", parsed.title)
        assertEquals(2, parsed.checklist.size)
        assertEquals("Item 01", parsed.checklist[0].text)
        assertFalse(parsed.checklist[0].isChecked)
        assertTrue(parsed.checklist[1].isChecked)
        assertEquals(listOf("errands"), parsed.labels)
        assertEquals(KeepColor.RED, parsed.color)
        assertTrue(parsed.isPinned)
        assertFalse(parsed.isArchived)
        assertEquals(1, parsed.attachments.size)
        assertEquals("photo.png", parsed.attachments.first().path)

        val calendar = Calendar.getInstance().apply { timeInMillis = parsed.createdAt!! }
        assertEquals(2025, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH))
        assertEquals(10, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parsesLegacyHtmlTextAndKeepsLineBreaks() {
        val html = """
            <html><head><title>Fallback title</title></head><body>
            <div class="note">
              <div class="title">Title</div>
              <div class="content">Line one<br>Line two</div>
            </div>
            </body></html>
        """.trimIndent()

        val note = KeepParser.parseHtml(html, 1234L)!!

        assertEquals("Title", note.title)
        assertEquals("Line one\nLine two", note.text)
        assertEquals(1234L, note.createdAt)
        assertFalse(note.isPinned)
        assertTrue(note.attachments.isEmpty())
    }

    @Test
    fun mapsKeepColorsToMimoPalette() {
        assertEquals(0, KeepColor.DEFAULT.toMimoColorId())
        assertEquals(1, KeepColor.RED.toMimoColorId())
        assertEquals(2, KeepColor.ORANGE.toMimoColorId())
        assertEquals(3, KeepColor.YELLOW.toMimoColorId())
        assertEquals(4, KeepColor.GREEN.toMimoColorId())
        assertEquals(5, KeepColor.TEAL.toMimoColorId())
        assertEquals(6, KeepColor.BLUE.toMimoColorId())
        assertEquals(6, KeepColor.CERULEAN.toMimoColorId())
        assertEquals(8, KeepColor.PURPLE.toMimoColorId())
        assertEquals(9, KeepColor.PINK.toMimoColorId())
        assertEquals(10, KeepColor.BROWN.toMimoColorId())
        assertEquals(0, KeepColor.GRAY.toMimoColorId())
    }

    @Test
    fun ignoresUnrelatedJsonAndHtml() {
        assertNull(KeepParser.parseJson("""{"labels":[{"name":"work"}]}"""))
        assertNull(KeepParser.parseJson("not json"))
        assertNull(KeepParser.parseHtml("<html><body><h1>Archive</h1></body></html>", 0L))
        assertNull(KeepParser.parseHtml("", 0L))
    }

    @Test
    fun parsesMultipleKeepDateFormats() {
        assertNotNull(KeepParser.parseKeepDate("May 10, 2025, 12:24:37 AM"))
        assertNotNull(KeepParser.parseKeepDate("May 10, 2025"))
        assertNotNull(KeepParser.parseKeepDate("10.05.2025 12:24"))
        assertNull(KeepParser.parseKeepDate("not a date"))
    }
}

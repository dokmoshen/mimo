package app.mimo.notes.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import app.mimo.notes.AppContainer
import app.mimo.notes.data.ChecklistItem
import app.mimo.notes.data.DrawStroke
import app.mimo.notes.data.Drawing
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.ReminderRepeat
import app.mimo.notes.data.RichSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Debug-only sample content. It exists so that the expressive layout can be evaluated
 * (and screenshotted) with realistic notes, labels, pinned cards, reminders and art.
 * Release builds never call this.
 */
object DemoSeeder {

    suspend fun seedIfNeeded(context: Context, container: AppContainer) {
        val prefs = context.getSharedPreferences("mimo_demo", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded_v2", false)) return
        val existing = withContext(Dispatchers.IO) {
            container.database.notesDao().labels()
        }
        if (existing.isNotEmpty()) {
            prefs.edit().putBoolean("seeded_v2", true).apply()
            return
        }
        val now = System.currentTimeMillis()
        val repository = container.notesRepository
        val work = Label(repository.addLabel("работа", 3), "работа", 3)
        val home = Label(repository.addLabel("дом", 0), "дом", 0)
        val ideas = Label(repository.addLabel("идеи", 4), "идеи", 4)
        val reading = Label(repository.addLabel("чтение", 2), "чтение", 2)

        val images = listOf(
            artCoast(container),
            artCup(container),
            artBotanical(container),
        )

        val welcomeText = "Mimo — тихие заметки, которые остаются только у вас.\n\n" +
            "Никаких аккаунтов, синхронизаций и лишних уведомлений. Только вы, бумага и мысли.\n\n" +
            "Попробуйте: закрепите важное, добавьте напоминание, соберите заметки метками."
        val welcomeSpans = buildList {
            add(RichSpan(0, 4, fontScale = 1.35f))
            val silent = welcomeText.indexOf("тихие заметки")
            if (silent >= 0) add(RichSpan(silent, silent + 13, bold = true, highlight = true))
            val noAccount = welcomeText.indexOf("Никаких аккаунтов")
            if (noAccount >= 0) add(RichSpan(noAccount, noAccount + 19, bold = true))
            val tryStart = welcomeText.indexOf("Попробуйте:")
            if (tryStart >= 0) add(RichSpan(tryStart, tryStart + 11, italic = true))
        }

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Добро пожаловать в Mimo",
                content = welcomeText,
                spans = welcomeSpans,
                colorId = 5,
                type = NoteType.TEXT,
                isPinned = true,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 30,
                modifiedAt = now - 1000 * 60 * 4,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(ideas),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Планы на неделю",
                content = "",
                spans = emptyList(),
                colorId = 3,
                type = NoteType.CHECKLIST,
                isPinned = true,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 12,
                modifiedAt = now - 1000 * 60 * 22,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = listOf(
                    ChecklistItem(UUID.randomUUID().toString(), "Выбрать палитру для нового проекта", true),
                    ChecklistItem(UUID.randomUUID().toString(), "Созвон в четверг в 11:00", false),
                    ChecklistItem(UUID.randomUUID().toString(), "Забронировать стол на вечер", false),
                    ChecklistItem(UUID.randomUUID().toString(), "Дочитать главу про типографику", false),
                ),
                labels = listOf(work),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Рецепт: паста с лимоном",
                content = "На 2 порции:\n\n• спагетти — 180 г\n• лимон — 1 шт.\n• пармезан — 60 г\n" +
                    "• сливочное масло — 40 г\n• чёрный перец\n\n" +
                    "Варить пасту до аль денте. Цедру и сок лимона смешать с маслом и тёртым пармезаном. " +
                    "Соединить с пастой, добавить немного воды от варки и много перца.",
                spans = listOf(
                    RichSpan(0, 20, bold = true, fontScale = 1.22f),
                    RichSpan(40, 67, italic = true),
                ),
                colorId = 2,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 50,
                modifiedAt = now - 1000 * 60 * 60 * 3,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = listOf(images[1]).filterNotNull(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(home),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Утренний берег",
                content = "",
                spans = emptyList(),
                colorId = 6,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 26,
                modifiedAt = now - 1000 * 60 * 60 * 7,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = listOfNotNull(images[0]),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(ideas),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Ботанический этюд",
                content = "",
                spans = emptyList(),
                colorId = 4,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 8,
                modifiedAt = now - 1000 * 60 * 60 * 6,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = listOfNotNull(images[2]),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = emptyList(),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Эскиз обложки",
                content = "",
                spans = emptyList(),
                colorId = 8,
                type = NoteType.DRAWING,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 6,
                modifiedAt = now - 1000 * 60 * 45,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = waveDrawing(),
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(ideas),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Книги, которые хочу прочитать",
                content = "",
                spans = emptyList(),
                colorId = 1,
                type = NoteType.CHECKLIST,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 40,
                modifiedAt = now - 1000 * 60 * 60 * 9,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = listOf(
                    ChecklistItem(UUID.randomUUID().toString(), "Хрупкие создания", true),
                    ChecklistItem(UUID.randomUUID().toString(), "Сто лет одиночества", false),
                    ChecklistItem(UUID.randomUUID().toString(), "Дизайн: форма и хаос", false),
                ),
                labels = listOf(reading),
            )
        )

        val workshop = repository.insertFullNote(
            Note(
                id = 0,
                title = "Позвонить в мастерскую",
                content = "Спросить про ремонт велосипеда — обещали перезвонить ещё в пятницу.",
                spans = emptyList(),
                colorId = 10,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 24,
                modifiedAt = now - 1000 * 60 * 90,
                reminderAt = now - 1000 * 60 * 30,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(home),
            )
        )
        val tomorrow = tomorrowNoon(10, 30)
        val laterNote = repository.insertFullNote(
            Note(
                id = 0,
                title = "Созвон по проекту",
                content = "Обсудить структуру и сроки. Подготовить три варианта первого экрана.",
                spans = listOf(RichSpan(0, 7, bold = true)),
                colorId = 7,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 14,
                modifiedAt = now - 1000 * 60 * 60 * 2,
                reminderAt = tomorrow,
                reminderRepeat = ReminderRepeat.WEEKLY,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(work),
            )
        )

        repository.setReminder(workshop, now - 1000 * 60 * 30, ReminderRepeat.NONE)
        repository.setReminder(laterNote, tomorrow, ReminderRepeat.WEEKLY)

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Старый список покупок",
                content = "Молоко, овсянка, кофе, зелёные яблоки.",
                spans = emptyList(),
                colorId = 11,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = true,
                isTrashed = false,
                trashedAt = null,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 24 * 14,
                modifiedAt = now - 1000 * 60 * 60 * 24 * 6,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = listOf(home),
            )
        )

        repository.insertFullNote(
            Note(
                id = 0,
                title = "Черновик афиши",
                content = "Вариант с крупной типографикой. Перепроверить дату и время.",
                spans = emptyList(),
                colorId = 9,
                type = NoteType.TEXT,
                isPinned = false,
                isArchived = false,
                isTrashed = true,
                trashedAt = now - 1000 * 60 * 60 * 36,
                isLocked = false,
                createdAt = now - 1000 * 60 * 60 * 24 * 5,
                modifiedAt = now - 1000 * 60 * 60 * 36,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE,
                images = emptyList(),
                drawing = null,
                audioPath = null,
                audioDurationMs = 0,
                checklist = emptyList(),
                labels = emptyList(),
            )
        )

        withContext(Dispatchers.IO) {
            container.reminderScheduler.schedule(workshop, now - 1000 * 60 * 30, ReminderRepeat.NONE)
            container.reminderScheduler.schedule(laterNote, tomorrow, ReminderRepeat.WEEKLY)
        }
        prefs.edit().putBoolean("seeded_v2", true).apply()
    }

    private fun tomorrowNoon(hour: Int, minute: Int): Long {
        val date = LocalDate.now().plusDays(1)
        return date.atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun waveDrawing(): Drawing {
        val strokes = mutableListOf<DrawStroke>()
        val ink = 0xFF26332D
        for (line in 0..3) {
            val points = mutableListOf<PointF>()
            for (step in 0..80) {
                val t = step / 80f
                val x = 0.08f + t * 0.84f
                val y = 0.34f + line * 0.11f + sin((t * 2.2f + line) * PI.toFloat()).toFloat() * 0.045f
                points.add(PointF(x, y))
            }
            strokes.add(DrawStroke(points, ink, 0.011f))
        }
        val sun = mutableListOf<PointF>()
        for (step in 0..60) {
            val angle = (step / 60f * 2f * PI).toFloat()
            sun.add(PointF(0.72f + cos(angle) * 0.075f, 0.19f + sin(angle) * 0.075f))
        }
        strokes.add(DrawStroke(sun, 0xFFC77A45, 0.012f))
        return Drawing(strokes)
    }

    private suspend fun artCoast(container: AppContainer): String? {
        val width = 1000
        val height = 1150
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 0f, height * 0.62f,
            intArrayOf(0xFFF4D7BD.toInt(), 0xFFE9E4CC.toInt(), 0xFFC6D9D1.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.62f, paint)
        paint.shader = null
        paint.color = 0xFFE7A15E.toInt()
        canvas.drawCircle(width * 0.7f, height * 0.3f, width * 0.13f, paint)
        paint.color = 0x33C27A4C
        canvas.drawCircle(width * 0.7f, height * 0.3f, width * 0.2f, paint)
        paint.shader = LinearGradient(
            0f, height * 0.6f, 0f, height.toFloat(),
            intArrayOf(0xFF8FAE9D.toInt(), 0xFF57796F.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        val sea = Path().apply {
            moveTo(0f, height * 0.58f)
            cubicTo(width * 0.25f, height * 0.56f, width * 0.6f, height * 0.63f, width.toFloat(), height * 0.59f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(sea, paint)
        paint.shader = null
        paint.color = 0xFFF1EBDD.toInt()
        paint.strokeWidth = 3f
        for (i in 0..7) {
            val y = height * 0.65f + i * 42f
            canvas.drawLine(width * 0.12f, y, width * (0.3f + (i % 3) * 0.18f), y, paint)
        }
        paint.color = 0xFF2E4A40.toInt()
        val foreground = Path().apply {
            moveTo(0f, height * 0.86f)
            cubicTo(width * 0.18f, height * 0.8f, width * 0.32f, height * 0.95f, width * 0.5f, height * 0.88f)
            cubicTo(width * 0.7f, height * 0.8f, width * 0.86f, height * 0.9f, width.toFloat(), height * 0.84f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        paint.color = 0xFF233630.toInt()
        canvas.drawPath(foreground, paint)
        val random = Random(9)
        paint.color = 0x10FFFFFF
        repeat(700) {
            canvas.drawCircle(
                random.nextFloat() * width,
                random.nextFloat() * height,
                random.nextFloat() * 2.2f + 0.4f,
                paint,
            )
        }
        return container.attachments.saveBitmap(bitmap, Bitmap.CompressFormat.JPEG)
    }

    private suspend fun artCup(container: AppContainer): String? {
        val width = 1000
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(0xFFF1E1C9.toInt(), 0xFFE4CBA9.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.color = 0xFFD8BE9A.toInt()
        canvas.drawCircle(width * 0.5f, height * 0.52f, width * 0.33f, paint)
        paint.color = 0xFFF8F0DF.toInt()
        canvas.drawCircle(width * 0.5f, height * 0.52f, width * 0.29f, paint)
        paint.color = 0xFF345347.toInt()
        canvas.drawCircle(width * 0.5f, height * 0.52f, width * 0.245f, paint)
        paint.shader = RadialGradient(
            width * 0.44f, height * 0.44f, width * 0.2f,
            intArrayOf(0xFF8DAF94.toInt(), 0xFF345347.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(width * 0.5f, height * 0.52f, width * 0.245f, paint)
        paint.shader = null
        paint.color = 0xFFF5EAD5.toInt()
        paint.strokeWidth = 7f
        paint.style = Paint.Style.STROKE
        for (i in 0..2) {
            val path = Path().apply {
                moveTo(width * 0.36f, height * (0.47f + i * 0.045f))
                cubicTo(
                    width * 0.43f, height * (0.42f + i * 0.04f),
                    width * 0.55f, height * (0.58f + i * 0.03f),
                    width * 0.64f, height * (0.51f + i * 0.03f),
                )
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = 0xFF9D6A42.toInt()
        canvas.drawOval(width * 0.42f, height * 0.16f, width * 0.58f, height * 0.28f, paint)
        paint.color = 0xFF6E4326.toInt()
        canvas.drawOval(width * 0.47f, height * 0.19f, width * 0.53f, height * 0.24f, paint)
        paint.color = 0x14000000
        canvas.drawCircle(width * 0.74f, height * 0.79f, width * 0.15f, paint)
        return container.attachments.saveBitmap(bitmap, Bitmap.CompressFormat.JPEG)
    }

    private suspend fun artBotanical(container: AppContainer): String? {
        val width = 1000
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFF2EBDD.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = 0xFFDDE5D2.toInt()
        canvas.drawCircle(width * 0.22f, height * 0.2f, width * 0.3f, paint)
        paint.color = 0x22A65F3A
        canvas.drawCircle(width * 0.8f, height * 0.75f, width * 0.36f, paint)
        paint.strokeWidth = 5f
        paint.color = 0xFF3B5E48.toInt()
        paint.style = Paint.Style.STROKE
        val stem = Path().apply {
            moveTo(width * 0.48f, height * 0.92f)
            cubicTo(width * 0.5f, height * 0.6f, width * 0.56f, height * 0.45f, width * 0.62f, height * 0.18f)
        }
        canvas.drawPath(stem, paint)
        paint.style = Paint.Style.FILL
        val leafColor = 0xFF6E8B68.toInt()
        val random = Random(4)
        for (i in 0..5) {
            val t = 0.3f + i * 0.1f
            val cx = width * (0.5f + t * 0.12f)
            val cy = height * (0.85f - t * 0.7f)
            val leafWidth = width * 0.16f
            val leafHeight = height * 0.13f
            paint.color = if (i % 2 == 0) leafColor else 0xFF8BA27F.toInt()
            val leaf = Path().apply {
                moveTo(cx, cy)
                cubicTo(
                    cx - leafWidth, cy - leafHeight * 0.5f,
                    cx - leafWidth * 0.7f, cy - leafHeight * 1.4f,
                    cx, cy - leafHeight * 1.6f,
                )
                cubicTo(
                    cx + leafWidth * 0.7f, cy - leafHeight * 1.2f,
                    cx + leafWidth, cy - leafHeight * 0.4f,
                    cx, cy,
                )
                close()
            }
            canvas.drawPath(leaf, paint)
            paint.color = 0xFF4A6B52.toInt()
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(cx, cy, cx, cy - leafHeight * 1.4f, paint)
            paint.style = Paint.Style.FILL
        }
        paint.color = 0x18FFFFFF
        repeat(500) {
            canvas.drawCircle(
                random.nextFloat() * width,
                random.nextFloat() * height,
                random.nextFloat() * 2f + 0.5f,
                paint,
            )
        }
        return container.attachments.saveBitmap(bitmap, Bitmap.CompressFormat.JPEG)
    }
}

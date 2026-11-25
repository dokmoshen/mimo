package app.mimo.notes.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owns every binary attachment. Photos are copied into the app's private storage and
 * downscaled so that a note can be reopened years later even when the original picker
 * permission is gone.
 */
class AttachmentStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    private val audioDir: File
        get() = File(context.filesDir, "audio").apply { mkdirs() }

    private val cameraDir: File
        get() = File(context.cacheDir, "camera").apply { mkdirs() }

    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 2000)
            }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@runCatching null

            val rotated = applyExifRotation(resolver, uri, bitmap)
            saveBitmapInternal(rotated, "img", Bitmap.CompressFormat.JPEG)
        }.getOrNull()
    }

    suspend fun importImageFile(file: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 2000)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                ?: return@runCatching null
            val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
            val rotated = exif?.let { applyRotation(bitmap, it) } ?: bitmap
            saveBitmapInternal(rotated, "img", Bitmap.CompressFormat.JPEG)
        }.getOrNull()
    }

    suspend fun saveBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): String? =
        withContext(Dispatchers.IO) {
            runCatching { saveBitmapInternal(bitmap, "draw", format) }.getOrNull()
        }

    suspend fun copyImage(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(path)
            if (!source.exists()) return@runCatching null
            val extension = source.extension.ifBlank { "jpg" }
            val destination = File(imagesDir, "img_${UUID.randomUUID()}.$extension")
            source.copyTo(destination, overwrite = false)
            destination.absolutePath
        }.getOrNull()
    }

    suspend fun importAudioFile(file: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists() || file.length() == 0L) return@runCatching null
            val extension = file.extension.lowercase().ifBlank { "m4a" }
            val destination = File(audioDir, "audio_${UUID.randomUUID()}.$extension")
            file.copyTo(destination, overwrite = false)
            destination.absolutePath
        }.getOrNull()
    }

    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        if (!path.isNullOrBlank()) runCatching { File(path).delete() }
    }

    fun audioFile(): File = File(audioDir, "audio_${UUID.randomUUID()}.m4a")

    fun cameraFile(): File = File(cameraDir, "camera_${UUID.randomUUID()}.jpg")

    fun deleteCameraFile(file: File) {
        runCatching { file.delete() }
    }

    private fun saveBitmapInternal(bitmap: Bitmap, prefix: String, format: Bitmap.CompressFormat): String? =
        runCatching {
            val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
            val file = File(imagesDir, "${prefix}_${UUID.randomUUID()}.$extension")
            FileOutputStream(file).use { out -> bitmap.compress(format, 90, out) }
            file.absolutePath
        }.getOrNull()

    private fun applyExifRotation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val exif = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull() ?: return bitmap
        return applyRotation(bitmap, exif)
    }

    private fun applyRotation(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val matrix = Matrix()
        when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxDimension || currentHeight / 2 >= maxDimension) {
            sample *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sample
    }
}

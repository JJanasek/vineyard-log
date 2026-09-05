package cz.janek.vineyardlog.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/** Keeps entry photos as downscaled JPEGs in the app's private storage. */
class PhotoStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    /** Copy an image the user picked or shot into [dir], downscaled to [maxSide] px and EXIF-rotated. */
    suspend fun import(uri: Uri, maxSide: Int = 1600): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longest / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Cannot decode image")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val out = if (scale < 1f || rotation != 0f) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else decoded
        val name = "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        FileOutputStream(file(name)).use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (out !== decoded) decoded.recycle()
        out.recycle()
        name
    }

    fun delete(name: String) { file(name).delete() }

    /** Temporary target for the camera app; caller imports it afterwards and deletes the temp file. */
    fun newCaptureTarget(): Pair<File, Uri> {
        val captureDir = File(context.cacheDir, "capture").apply { mkdirs() }
        val f = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        return f to uri
    }

    /** Remove files no longer referenced by any row (e.g. after a backup restore). */
    fun pruneUnreferenced(referenced: Set<String>) {
        dir.listFiles()?.forEach { if (it.name !in referenced) it.delete() }
    }
}

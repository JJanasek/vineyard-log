package cz.janek.vineyardlog.data.photos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Keeps entry attachments (PDF lab reports, invoices, scans) as-is in the app's private storage. */
class FileStore(private val context: Context) {
    data class Imported(val fileName: String, val displayName: String, val mime: String)

    private val dir: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    /** Copy a document the user picked; the original name and MIME type are kept for display and opening. */
    suspend fun import(uri: Uri, maxBytes: Long = 25L * 1024 * 1024): Imported = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var display = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cur ->
            if (cur.moveToFirst()) {
                cur.getString(0)?.let { display = it }
                val sizeIdx = 1
                if (!cur.isNull(sizeIdx) && cur.getLong(sizeIdx) > maxBytes) error("File larger than ${maxBytes / 1024 / 1024} MB")
            }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val ext = display.substringAfterLast('.', "").take(8).lowercase().ifBlank { if (mime == "application/pdf") "pdf" else "bin" }
        val name = "a_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
        resolver.openInputStream(uri)?.use { input -> FileOutputStream(file(name)).use { input.copyTo(it) } } ?: error("Cannot open file")
        Imported(name, display, mime)
    }

    fun delete(name: String) { file(name).delete() }

    /** Content URI for viewers (FileProvider path "attachments"). */
    fun shareUri(name: String): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(name))

    fun pruneUnreferenced(referenced: Set<String>) {
        dir.listFiles()?.forEach { if (it.name !in referenced) it.delete() }
    }
}

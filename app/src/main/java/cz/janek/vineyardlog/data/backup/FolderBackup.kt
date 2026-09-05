package cz.janek.vineyardlog.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import cz.janek.vineyardlog.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes the JSON backup and a photos zip into a user-chosen SAF folder (synced by another app). */
class FolderBackup(private val context: Context, private val c: AppContainer) {
    data class Result(val rows: Int, val photos: Int)

    suspend fun backupNow(): Result {
        val settings = c.settings.settings.first()
        val tree = settings.backupFolder.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: error("no folder")
        val data = c.backupDao.dump()
        val json = BackupCodec.encode(data)
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val jsonUri = findOrCreate(tree, "vineyard-log-latest.json", "application/json")
            resolver.openOutputStream(jsonUri, "wt")?.use { it.write(json.toByteArray()) } ?: error("cannot write json")
            if (data.photos.isNotEmpty()) {
                val zipUri = findOrCreate(tree, "vineyard-log-photos.zip", "application/zip")
                resolver.openOutputStream(zipUri, "wt")?.use { out ->
                    ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                        data.photos.forEach { p ->
                            val f = c.photos.file(p.fileName)
                            if (!f.exists()) return@forEach
                            zip.putNextEntry(ZipEntry("photos/${p.fileName}"))
                            FileInputStream(f).use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                } ?: error("cannot write zip")
            }
        }
        c.settings.update { it.copy(lastFolderBackupAt = System.currentTimeMillis()) }
        return Result(data.totalRows, data.photos.size)
    }

    /** Back up if a folder is set and the last backup is older than [maxAgeMs]; errors are swallowed. */
    suspend fun backupIfStale(maxAgeMs: Long = 24L * 3600_000) {
        val s = c.settings.settings.first()
        if (s.backupFolder.isBlank()) return
        if (System.currentTimeMillis() - s.lastFolderBackupAt < maxAgeMs) return
        runCatching { backupNow() }
    }

    private fun findOrCreate(tree: Uri, name: String, mime: String): Uri {
        val resolver = context.contentResolver
        val dirId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cur ->
            while (cur.moveToNext()) {
                if (cur.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, cur.getString(0))
            }
        }
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(tree, dirId)
        return DocumentsContract.createDocument(resolver, dirUri, mime, name) ?: error("cannot create $name")
    }
}

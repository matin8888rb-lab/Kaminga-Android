package com.kaminga.reader

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

internal object ZipComicImporter {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private const val MAX_TOTAL_BYTES = 400L * 1024L * 1024L
    private const val MAX_ENTRY_BYTES = 80L * 1024L * 1024L
    private const val MAX_IMAGES = 2000

    fun import(context: Context, uri: Uri, destination: File): Int {
        val id = UUID.randomUUID().toString()
        val temp = File(context.filesDir, "zip_import_$id")
        val backup = File(context.filesDir, "zip_backup_$id")
        var committed = false
        return try {
            if (!temp.mkdirs()) return 0
            val names = mutableListOf<String>()
            var totalBytes = 0L
            val stream = context.contentResolver.openInputStream(uri) ?: return 0
            stream.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory) {
                            val safeName = entry.name.replace('\\', '/')
                            val ext = safeName.substringAfterLast('.', "").lowercase()
                            if (safeName.split('/').none { it == ".." } && ext in imageExtensions) {
                                require(names.size < MAX_IMAGES)
                                val source = File(temp, "entry_${names.size}.bin")
                                var entryBytes = 0L
                                FileOutputStream(source).use { output ->
                                    val buffer = ByteArray(32 * 1024)
                                    while (true) {
                                        val n = zip.read(buffer)
                                        if (n < 0) break
                                        entryBytes += n
                                        totalBytes += n
                                        require(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_TOTAL_BYTES)
                                        output.write(buffer, 0, n)
                                    }
                                }
                                if (entryBytes > 0L && isValidImage(source)) names += safeName
                                else source.delete()
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            if (names.isEmpty()) return 0
            val sorted = names.indices.sortedWith { a, b -> naturalCompare(names[a], names[b]) }
            sorted.forEachIndexed { index, sourceIndex ->
                val source = File(temp, "entry_$sourceIndex.bin")
                val ext = names[sourceIndex].substringAfterLast('.', "img").lowercase()
                val target = File(temp, "page_%05d.%s".format(index + 1, ext))
                if (!source.renameTo(target)) source.copyTo(target, overwrite = true)
                source.delete()
            }
            if (destination.exists() && !destination.renameTo(backup)) return 0
            if (!temp.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                return 0
            }
            committed = true
            names.size
        } catch (_: Exception) {
            if (!destination.exists() && backup.exists()) backup.renameTo(destination)
            0
        } finally {
            temp.deleteRecursively()
            if (committed) backup.deleteRecursively()
        }
    }

    private fun isValidImage(file: File): Boolean = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.outWidth > 0 && options.outHeight > 0
    } catch (_: Exception) { false }

    private fun naturalCompare(first: String, second: String): Int {
        val a = Regex("\\d+|\\D+").findAll(first.lowercase()).map { it.value }.toList()
        val b = Regex("\\d+|\\D+").findAll(second.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(a.size, b.size)) {
            val x = a[i]; val y = b[i]
            val result = if (x.firstOrNull()?.isDigit() == true && y.firstOrNull()?.isDigit() == true)
                (x.toLongOrNull() ?: Long.MAX_VALUE).compareTo(y.toLongOrNull() ?: Long.MAX_VALUE)
            else x.compareTo(y)
            if (result != 0) return result
        }
        return a.size.compareTo(b.size)
    }
}

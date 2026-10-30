package com.kaminga.reader

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

internal object ZipComicImporter {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun import(context: Context, uri: Uri, destination: File): Int {
        val temp = File(context.filesDir, "zip_import_tmp")
        val backup = File(context.filesDir, "zip_backup_tmp")
        temp.deleteRecursively()
        backup.deleteRecursively()
        return try {
            temp.mkdirs()
            val entries = mutableListOf<Pair<String, ByteArray>>()
            var totalBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val safeName = entry.name.replace('\\', '/')
                            val extension = safeName.substringAfterLast('.', "").lowercase()
                            if (extension in imageExtensions && safeName.split('/').none { it == ".." }) {
                                val bytes = zip.readBytes()
                                totalBytes += bytes.size
                                require(totalBytes <= 400L * 1024L * 1024L)
                                require(entries.size < 2000)
                                if (bytes.isNotEmpty()) entries += safeName to bytes
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return 0

            val sorted = entries.sortedWith(Comparator { a, b -> naturalCompare(a.first, b.first) })
            sorted.forEachIndexed { index, item ->
                val ext = item.first.substringAfterLast('.', "img").lowercase()
                File(temp, "page_%05d.%s".format(index + 1, ext)).writeBytes(item.second)
            }
            if (sorted.isEmpty()) return 0
            if (destination.exists() && !destination.renameTo(backup)) return 0
            if (!temp.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                return 0
            }
            backup.deleteRecursively()
            sorted.size
        } catch (_: Exception) {
            if (!destination.exists() && backup.exists()) backup.renameTo(destination)
            0
        } finally {
            temp.deleteRecursively()
            if (destination.exists()) backup.deleteRecursively()
        }
    }

    private fun naturalCompare(first: String, second: String): Int {
        val a = Regex("\\d+|\\D+").findAll(first.lowercase()).map { it.value }.toList()
        val b = Regex("\\d+|\\D+").findAll(second.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(a.size, b.size)) {
            val x = a[i]
            val y = b[i]
            val result = if (x.firstOrNull()?.isDigit() == true && y.firstOrNull()?.isDigit() == true) {
                (x.toLongOrNull() ?: Long.MAX_VALUE).compareTo(y.toLongOrNull() ?: Long.MAX_VALUE)
            } else x.compareTo(y)
            if (result != 0) return result
        }
        return a.size.compareTo(b.size)
    }
}

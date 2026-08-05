package dev.voicejournal.data.backup.v1

import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupZipWriter {

    /**
     * Streams all backup JSON manifests and media binaries into [outputStream] in ZIP format.
     */
    fun writeBackup(
        outputStream: OutputStream,
        manifestJsonBytes: ByteArray,
        entriesJsonBytes: ByteArray,
        tagsJsonBytes: ByteArray,
        attachmentsJsonBytes: ByteArray,
        preferencesJsonBytes: ByteArray,
        collectedMedia: List<CollectedMedia>
    ) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            // 1. Write root JSON entries
            writeZipEntry(zipOut, "manifest.json", manifestJsonBytes)
            writeZipEntry(zipOut, "entries.json", entriesJsonBytes)
            writeZipEntry(zipOut, "tags.json", tagsJsonBytes)
            writeZipEntry(zipOut, "attachments.json", attachmentsJsonBytes)
            writeZipEntry(zipOut, "preferences.json", preferencesJsonBytes)

            // 2. Stream binary media files under media/
            val writtenPaths = mutableSetOf<String>()
            collectedMedia.forEach { media ->
                val archivePath = media.attachment.archivePath
                if (archivePath !in writtenPaths) {
                    writtenPaths.add(archivePath)
                    writeMediaEntry(zipOut, archivePath, media.sourceFile)
                }
            }

            zipOut.finish()
        }
    }

    private fun writeZipEntry(zipOut: ZipOutputStream, entryName: String, dataBytes: ByteArray) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        zipOut.write(dataBytes)
        zipOut.closeEntry()
    }

    private fun writeMediaEntry(zipOut: ZipOutputStream, archivePath: String, file: File) {
        if (!file.exists() || !file.isFile) return
        val entry = ZipEntry(archivePath)
        zipOut.putNextEntry(entry)
        file.inputStream().use { isStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (isStream.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
        }
        zipOut.closeEntry()
    }
}

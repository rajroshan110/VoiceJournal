package dev.voicejournal.data.backup.v1

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class BackupUnpacker(
    private val context: Context
) {

    fun unpack(inputStream: InputStream): File {
        val stagingDir = File(context.cacheDir, "import_staging_${System.currentTimeMillis()}")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        val canonicalStagingPath = stagingDir.canonicalPath
        
        var totalSize = 0L
        var entryCount = 0
        val MAX_TOTAL_SIZE = 5L * 1024 * 1024 * 1024 // 5 GB
        val MAX_ENTRY_COUNT = 50_000
        val MAX_FILE_SIZE = 500L * 1024 * 1024 // 500 MB

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw SecurityException("Too many entries in zip file")
                }

                val outFile = File(stagingDir, entry.name)

                // Zip Slip Protection
                if (!outFile.canonicalPath.startsWith(canonicalStagingPath)) {
                    throw SecurityException("Zip entry is trying to break out of target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { os ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var currentFileSize = 0L
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            currentFileSize += bytesRead
                            totalSize += bytesRead
                            
                            if (currentFileSize > MAX_FILE_SIZE) {
                                throw SecurityException("File size exceeds limit")
                            }
                            if (totalSize > MAX_TOTAL_SIZE) {
                                throw SecurityException("Total extracted size exceeds limit")
                            }
                            os.write(buffer, 0, bytesRead)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }

        return stagingDir
    }

    fun cleanup(stagingDir: File?) {
        try {
            if (stagingDir != null && stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
        } catch (e: Exception) {
            // Ignore cleanup failure
        }
    }
}

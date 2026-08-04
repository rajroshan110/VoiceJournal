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

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
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
                        zis.copyTo(os)
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

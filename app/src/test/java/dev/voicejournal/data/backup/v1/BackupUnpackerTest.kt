package dev.voicejournal.data.backup.v1

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue

class BackupUnpackerTest {

    private lateinit var mockContext: Context
    private lateinit var tempCacheDir: File
    private lateinit var unpacker: BackupUnpacker

    @Before
    fun setup() {
        tempCacheDir = Files.createTempDirectory("vj_test_cache").toFile()
        mockContext = mockk()
        every { mockContext.cacheDir } returns tempCacheDir
        unpacker = BackupUnpacker(mockContext)
    }

    @After
    fun teardown() {
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun `test successful unpack`() {
        val zipBytes = createZip { zos ->
            zos.putNextEntry(ZipEntry("db.sqlite"))
            zos.write("database_content".toByteArray())
            zos.closeEntry()
        }

        val stagingDir = unpacker.unpack(ByteArrayInputStream(zipBytes))
        assertTrue(stagingDir.exists())
        assertTrue(File(stagingDir, "db.sqlite").exists())
    }

    @Test
    fun `test zip slip protection throws exception`() {
        val zipBytes = createZip { zos ->
            zos.putNextEntry(ZipEntry("../malicious.sh"))
            zos.write("echo pwned".toByteArray())
            zos.closeEntry()
        }

        assertThrows(SecurityException::class.java) {
            unpacker.unpack(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `test zip bomb extraction limits`() {
        val zipBytes = createZip { zos ->
            for (i in 1..50005) {
                zos.putNextEntry(ZipEntry("file$i.txt"))
                zos.write("a".toByteArray())
                zos.closeEntry()
            }
        }

        assertThrows(SecurityException::class.java) {
            unpacker.unpack(ByteArrayInputStream(zipBytes))
        }
    }

    private fun createZip(block: (ZipOutputStream) -> Unit): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        block(zos)
        zos.close()
        return baos.toByteArray()
    }
}

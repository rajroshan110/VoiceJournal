package dev.voicejournal.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.dao.EntryImageDao
import dev.voicejournal.data.local.db.dao.JournalEntryDao
import dev.voicejournal.data.local.db.dao.TagDao
import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.relation.EntryWithTagsAndImages
import dev.voicejournal.data.storage.MediaStorageManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class JournalRefreshHealingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var recordingsDir: File

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        recordingsDir = File(filesDir, "recordings").apply { mkdirs() }

        mockContext = mockk(relaxed = true) {
            every { this@mockk.filesDir } returns this@JournalRefreshHealingTest.filesDir
            every { this@mockk.cacheDir } returns this@JournalRefreshHealingTest.cacheDir
        }

        mockkObject(MediaStorageManager)
        every { MediaStorageManager.getAllAudioDirs(mockContext) } returns listOf(recordingsDir)
        every { MediaStorageManager.getTranscribeCacheDir(mockContext) } returns File(cacheDir, "audio_transcribe_cache").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testForceRepairFixesCorruptedWavHeader() {
        val corruptedWav = File(recordingsDir, "test_corrupt.wav")
        val pcmData = ByteArray(3200) { 1 } // 3200 bytes of mock PCM data
        val totalFileSize = 44 + pcmData.size

        val raf = RandomAccessFile(corruptedWav, "rw")
        // Write RIFF header with corrupted chunk size (36) and data size (0)
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array()) // Corrupt ChunkSize
        raf.write("WAVEfmt ".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // PCM
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // Mono
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16000).array()) // 16kHz
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32000).array()) // Byte rate
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array()) // Block align
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array()) // Bits per sample
        raf.write("data".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()) // Corrupt DataSize = 0
        raf.write(pcmData)
        raf.close()

        assertEquals(totalFileSize.toLong(), corruptedWav.length())

        // Run forceRepair
        AudioFileRepair.forceRepair(mockContext)

        // Verify that headers have been repaired based on actual file size
        val checkRaf = RandomAccessFile(corruptedWav, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val repairedChunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36 + pcmData.size, repairedChunkSize)

        checkRaf.seek(40)
        val dataSizeBuf = ByteArray(4)
        checkRaf.readFully(dataSizeBuf)
        val repairedDataSize = ByteBuffer.wrap(dataSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(pcmData.size, repairedDataSize)

        checkRaf.close()
    }

    @Test
    fun testForceRepairIgnoresNonWavRiffFiles() {
        val nonWavRiff = File(recordingsDir, "test_webp.wav")
        val content = ByteArray(100) { 0 }
        val raf = RandomAccessFile(nonWavRiff, "rw")
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array())
        raf.write("WEBP".toByteArray()) // Not WAVE!
        raf.write(content)
        raf.close()

        AudioFileRepair.forceRepair(mockContext)

        // Verify ChunkSize was NOT touched
        val checkRaf = RandomAccessFile(nonWavRiff, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val chunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36, chunkSize)
        checkRaf.close()
    }

    @Test
    fun testForceRepairIgnoresNonStandardDataMarker() {
        val nonStandardWav = File(recordingsDir, "test_nonstandard.wav")
        val content = ByteArray(100) { 0 }
        val raf = RandomAccessFile(nonStandardWav, "rw")
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array())
        raf.write("WAVEfmt ".toByteArray())
        raf.write(ByteArray(24) { 0 })
        raf.write("junk".toByteArray()) // Not 'data' marker at byte 36
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())
        raf.write(content)
        raf.close()

        AudioFileRepair.forceRepair(mockContext)

        val checkRaf = RandomAccessFile(nonStandardWav, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val chunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36, chunkSize)
        checkRaf.close()
    }

    @Test
    fun testForceRepairHandlesTruncatedFileGracefully() {
        val truncated = File(recordingsDir, "truncated.wav")
        val raf = RandomAccessFile(truncated, "rw")
        raf.write("RIFF1234".toByteArray()) // 8 bytes only
        raf.close()

        // Should not throw or crash
        AudioFileRepair.forceRepair(mockContext)
        assertEquals(8L, truncated.length())
    }

    @Test
    fun testRefreshAndHealDataPurgesExpiredTrashMediaAndSweepsOrphanedDirs() = runBlocking {
        mockkStatic("androidx.room.RoomDatabaseKt")

        val entryDao = mockk<JournalEntryDao>(relaxed = true)
        val tagDao = mockk<TagDao>(relaxed = true)
        val imageDao = mockk<EntryImageDao>(relaxed = true)
        val prefs = mockk<UserPreferencesManager>(relaxed = true)
        val appDatabase = mockk<AppDatabase>(relaxed = true)

        coEvery { appDatabase.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            val block = secondArg<suspend () -> Any?>()
            block()
        }

        // Expired trashed entry with internal audio and image files
        val expiredAudio = File(recordingsDir, "expired.wav").apply { writeText("audio") }
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val expiredImage = File(imagesDir, "expired.jpg").apply { writeText("img") }
        val activeAudio = File(recordingsDir, "active.wav").apply { writeText("active") }

        val expiredEntity = JournalEntryEntity(
            id = 42L,
            createdAt = 1000L,
            updatedAt = 1000L,
            audioPath = expiredAudio.absolutePath,
            audioFormat = "WAV_16KHZ",
            duration = 100L,
            deletedAt = 1000L
        )
        val expiredDetails = EntryWithTagsAndImages(
            entry = expiredEntity,
            tags = emptyList(),
            images = listOf(EntryImageEntity(id = 1L, entryId = 42L, imagePath = expiredImage.absolutePath, displayOrder = 0))
        )

        coEvery { entryDao.getExpiredTrashEntryIds(any()) } returns listOf(42L)
        coEvery { entryDao.getEntryByIdSync(42L) } returns expiredDetails
        coEvery { entryDao.getAllRawEntriesSync() } returns emptyList()

        // Create orphaned staging/backup dirs (> 1 hour old) and recent ones (< 1 hour old)
        val twoHoursAgo = System.currentTimeMillis() - 7_200_000L
        val oldOrphans = listOf(
            File(filesDir, "recordings_staging_123").apply { mkdirs(); File(this, "a.wav").writeText("x"); setLastModified(twoHoursAgo) },
            File(filesDir, "images_staging_123").apply { mkdirs(); setLastModified(twoHoursAgo) },
            File(filesDir, "recordings_backup_123").apply { mkdirs(); setLastModified(twoHoursAgo) },
            File(cacheDir, "images_backup_456").apply { mkdirs(); setLastModified(twoHoursAgo) }
        )
        val recentOrphan = File(filesDir, "recordings_staging_recent").apply { mkdirs() }

        val repository = JournalRepositoryImpl(
            context = mockContext,
            journalEntryDao = entryDao,
            tagDao = tagDao,
            entryImageDao = imageDao,
            prefs = prefs,
            appDatabase = appDatabase
        )

        repository.refreshAndHealData()

        // Expired trash entry deleted from DB and disk; active media untouched
        coVerify(exactly = 1) { entryDao.deleteEntry(42L) }
        coVerify(exactly = 0) { entryDao.deleteExpiredTrashEntries(any()) }
        assertFalse(expiredAudio.exists())
        assertFalse(expiredImage.exists())
        assertTrue(activeAudio.exists())

        // Old orphan dirs deleted; recent orphan & active dirs preserved
        oldOrphans.forEach { dir ->
            assertFalse("Expected orphaned dir ${dir.name} to be deleted", dir.exists())
        }
        assertTrue(recentOrphan.exists())
        assertTrue(recordingsDir.exists())
        assertTrue(imagesDir.exists())
    }

    @Test
    fun testDeleteTagRemovesCrossRefsAndTagInTransaction() = runBlocking {
        mockkStatic("androidx.room.RoomDatabaseKt")

        val entryDao = mockk<JournalEntryDao>(relaxed = true)
        val tagDao = mockk<TagDao>(relaxed = true)
        val imageDao = mockk<EntryImageDao>(relaxed = true)
        val prefs = mockk<UserPreferencesManager>(relaxed = true)
        val appDatabase = mockk<AppDatabase>(relaxed = true)

        coEvery { appDatabase.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            val block = secondArg<suspend () -> Any?>()
            block()
        }

        val repository = JournalRepositoryImpl(
            context = mockContext,
            journalEntryDao = entryDao,
            tagDao = tagDao,
            entryImageDao = imageDao,
            prefs = prefs,
            appDatabase = appDatabase
        )

        repository.deleteTag(99L)

        coVerify(exactly = 1) { tagDao.deleteTagCrossRefsByTagId(99L) }
        coVerify(exactly = 1) { tagDao.deleteTagInternal(99L) }
    }
}

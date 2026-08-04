package dev.voicejournal.data.backup.v1

import android.content.Context
import dev.voicejournal.data.backup.v1.dto.BackupManifest
import dev.voicejournal.data.backup.v1.dto.ChecksumMap
import dev.voicejournal.data.backup.v1.dto.DataCounts
import dev.voicejournal.data.backup.v1.dto.GeneratorInfo
import java.security.MessageDigest
import java.time.ZoneId
import java.util.UUID

class BackupManifestBuilder(
    private val context: Context
) {

    fun buildManifest(
        entriesCount: Int,
        tagsCount: Int,
        attachmentsCount: Int,
        mediaFilesCount: Int,
        entriesJsonBytes: ByteArray,
        tagsJsonBytes: ByteArray,
        attachmentsJsonBytes: ByteArray,
        preferencesJsonBytes: ByteArray
    ): String {
        val backupUuid = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id ?: "UTC"

        val generatorInfo = getGeneratorInfo()
        val dataCounts = DataCounts(
            entries = entriesCount,
            tags = tagsCount,
            attachments = attachmentsCount,
            mediaFiles = mediaFilesCount
        )

        val checksumMap = ChecksumMap(
            algorithm = "SHA-256",
            entriesJson = computeSha256(entriesJsonBytes),
            tagsJson = computeSha256(tagsJsonBytes),
            attachmentsJson = computeSha256(attachmentsJsonBytes),
            preferencesJson = computeSha256(preferencesJsonBytes)
        )

        val manifest = BackupManifest(
            formatName = "dev.voicejournal.backup",
            formatVersion = 1,
            minReaderVersion = 1,
            backupUuid = backupUuid,
            createdAt = createdAt,
            timezoneId = timezoneId,
            generator = generatorInfo,
            counts = dataCounts,
            features = emptyList(),
            checksums = checksumMap
        )

        return manifest.toJson().toString(2)
    }

    private fun getGeneratorInfo(): GeneratorInfo {
        var versionName = "0.1.0"
        var versionCode = 1
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName ?: "0.1.0"
            @Suppress("DEPRECATION")
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
        } catch (e: Exception) {
            // Fallback default
        }

        return GeneratorInfo(
            appId = context.packageName,
            appVersionName = versionName,
            appVersionCode = versionCode,
            platform = "android"
        )
    }

    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}

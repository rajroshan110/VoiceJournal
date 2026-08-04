package dev.voicejournal.data.backup.v1.dto

data class RestoreStats(
    val entriesRestored: Int,
    val tagsRestored: Int,
    val attachmentsRestored: Int,
    val mediaFilesRestored: Int
)

data class ImportResult(
    val success: Boolean,
    val validationErrors: List<String> = emptyList(),
    val stats: RestoreStats = RestoreStats(0, 0, 0, 0)
)

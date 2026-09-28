package dev.voicejournal.domain.model

import androidx.compose.runtime.Stable
import java.util.UUID

@Stable
data class JournalEntry(
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val title: String? = null,
    val audioPath: String = "",
    val audioFormat: AudioFormat = AudioFormat.WAV_16KHZ,
    val duration: Long = 0L,
    val transcript: String? = null,
    val transcriptCreatedAt: Long? = null,
    val transcriptModel: String? = null,
    val transcriptLanguage: String? = null,
    val transcriptVersion: String? = null,
    val userText: String? = null,
    val moodEmoji: String? = null,
    val moodLevel: Int? = null,
    val moodCategory: String? = null,
    val hasTranscript: Boolean = transcript != null,
    val tags: List<Tag> = emptyList(),
    val images: List<EntryImage> = emptyList(),
    val rawWaveformAmplitudes: List<Byte> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val deletedAt: Long? = null,
    val isArchived: Boolean = false,
    val isDraft: Boolean = false,
    val uuid: String = UUID.randomUUID().toString()
) {
    val isDeleted: Boolean get() = deletedAt != null
    val isEmpty: Boolean get() = title.isNullOrBlank() && userText.isNullOrBlank() && transcript.isNullOrBlank() && audioTracks.isEmpty() && images.isEmpty() && audioPath.isEmpty()

    val daysUntilPermanentDeletion: Int get() {
        val delAt = deletedAt ?: return 7
        val elapsedMs = (System.currentTimeMillis() - delAt).coerceAtLeast(0L)
        val totalMs = 7 * 24 * 60 * 60 * 1000L
        val remainingMs = (totalMs - elapsedMs).coerceAtLeast(0L)
        return kotlin.math.ceil(remainingMs / (24.0 * 60.0 * 60.0 * 1000.0)).toInt().coerceIn(0, 7)
    }

    val isExpiringSoon: Boolean get() = isDeleted && daysUntilPermanentDeletion <= 1
    // Dynamic people derived from PERSON tags
    val people: List<String> get() = tags.filter { it.type == TagType.PERSON }.map { it.name.removePrefix("#").removePrefix("@").trim() }

    // UI convenience & spec properties
    val timestamp: Long get() = createdAt
    val mood: String get() = moodEmoji ?: "😊"

    val plainUserText: String?
        get() = userText?.let {
            dev.voicejournal.ui.notedetail.editor.serializer.RichTextHtmlSerializer.toPlainText(
                dev.voicejournal.ui.notedetail.editor.serializer.RichTextHtmlSerializer.fromHtml(it)
            )
        }

    // All active audio tracks (Max 3)
    val allAudioTracks: List<AudioTrack> get() {
        return if (audioTracks.isNotEmpty()) {
            audioTracks.take(3)
        } else if (audioPath.isNotEmpty()) {
            listOf(AudioTrack(id = "legacy_$id", path = audioPath, durationMs = duration, transcript = transcript, rawWaveformAmplitudes = rawWaveformAmplitudes))
        } else {
            emptyList()
        }
    }

    // First created audio track used ONLY for Journal & Calendar feed preview UI
    val firstAudioTrack: AudioTrack? get() = allAudioTracks.firstOrNull()

    val audioUri: String? get() = firstAudioTrack?.path
    val audioDurationMs: Long get() = firstAudioTrack?.durationMs ?: duration

    // Cache these to avoid allocating new lists on every composition access
    val imagePaths: List<String> by lazy { images.map { it.imagePath } }
    val mediaThumbnails: List<String> get() = imagePaths

    val waveformAmplitudes: List<Byte> by lazy {
        firstAudioTrack?.waveformAmplitudes ?: run {
            if (rawWaveformAmplitudes.size == 32) rawWaveformAmplitudes
            else List(32) { i ->
                val base = kotlin.math.sin((i + id).toDouble() * 0.5) * 40 + 50
                base.coerceIn(10.0, 100.0).toInt().toByte()
            }
        }
    }
}

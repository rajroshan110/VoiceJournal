package dev.voicejournal.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

sealed class PlayerState {
    object Idle : PlayerState()
    data class Playing(val entryId: Long?, val currentPosition: Long, val totalDuration: Long, val audioPath: String? = null) : PlayerState()
    data class Paused(val entryId: Long?, val currentPosition: Long, val totalDuration: Long, val audioPath: String? = null) : PlayerState()
    object Ended : PlayerState()
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : PlayerState()
}

class AudioPlayerManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private val _playbackState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playbackState: StateFlow<PlayerState> = _playbackState

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentTitle: String = "Voice Note Playback"
    var currentEntryId: Long? = null
        private set
    var currentAudioPath: String? = null
        private set

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            when (playbackState) {
                Player.STATE_ENDED -> {
                    _playbackState.value = PlayerState.Ended
                    progressJob?.cancel()
                    AudioPlaybackService.stop(context)
                }
                Player.STATE_READY -> {
                    if (!player.playWhenReady && !player.isPlaying && player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                        val dur = player.duration.coerceAtLeast(0)
                        _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
                        progressJob?.cancel()
                        AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
                    } else if (player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                        handlePlaybackSuppression(player.playbackSuppressionReason)
                    }
                }
                Player.STATE_IDLE -> {
                    val error = player.playerError
                    if (error != null) {
                        handlePlayerError(error)
                    }
                }
                Player.STATE_BUFFERING -> {
                    if (player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                        handlePlaybackSuppression(player.playbackSuppressionReason)
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val player = exoPlayer ?: return
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) return
            if (!playWhenReady && !player.isPlaying) {
                val dur = player.duration.coerceAtLeast(0)
                _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
                progressJob?.cancel()
                AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = exoPlayer ?: return
            val dur = player.duration.coerceAtLeast(0)
            if (isPlaying) {
                _playbackState.value = PlayerState.Playing(currentEntryId, player.currentPosition, dur, currentAudioPath)
                startProgressTracking()
                AudioPlaybackService.updateState(context, true, currentTitle, currentEntryId)
            } else {
                if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) return
                _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
                progressJob?.cancel()
                AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
            }
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            handlePlaybackSuppression(playbackSuppressionReason)
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError(error)
        }
    }

    private fun handlePlaybackSuppression(reason: Int) {
        if (reason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            exoPlayer?.let { player ->
                val dur = player.duration.coerceAtLeast(0)
                _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
            }
            progressJob?.cancel()
            AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
            return
        }
        if (reason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
            val errorMessage = when (reason) {
                Player.PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_OUTPUT ->
                    "Audio output is currently unavailable."
                else ->
                    "Audio playback paused by system."
            }
            _playbackState.value = PlayerState.Error(errorMessage)
            progressJob?.cancel()
            AudioPlaybackService.stop(context)
            try {
                exoPlayer?.pause()
            } catch (ignored: Throwable) {}
        }
    }

    private fun handlePlayerError(error: PlaybackException) {
        val msg = when (error.errorCode) {
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ->
                "Audio output is in use by another app or phone call."
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ->
                "Audio output unavailable."
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Audio file does not exist or was deleted."
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "Corrupt audio file or unsupported audio format."
            else ->
                "Playback error: ${error.message ?: "Audio playback failed"}"
        }
        _playbackState.value = PlayerState.Error(msg)
        progressJob?.cancel()
        AudioPlaybackService.stop(context)
    }


    val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: 0L

    val duration: Long
        get() = exoPlayer?.duration?.coerceAtLeast(0) ?: -1L

    fun play(audioPath: String, title: String = "Voice Note Playback", entryId: Long? = null) {
        if (audioPath.isEmpty()) {
            _playbackState.value = PlayerState.Error("Audio path is empty")
            return
        }

        val audioFile = if (audioPath.startsWith("/")) File(audioPath) else null
        if (audioFile != null && (!audioFile.exists() || audioFile.length() == 0L)) {
            _playbackState.value = PlayerState.Error("Audio file does not exist or is empty")
            return
        }

        // Cancel progress tracking and stop existing playback before switching audio paths
        progressJob?.cancel()
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (ignored: Exception) {}

        currentTitle = title.ifBlank { "Voice Note Playback" }
        currentEntryId = entryId
        currentAudioPath = audioPath

        val mediaUri: Uri = if (audioFile != null) {
            Uri.fromFile(audioFile)
        } else {
            Uri.parse(audioPath)
        }

        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(playerListener)
                }
        }

        // Start the foreground service notification first
        AudioPlaybackService.start(context, currentTitle, currentEntryId)

        try {
            exoPlayer?.apply {
                setMediaItem(MediaItem.fromUri(mediaUri))
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            _playbackState.value = PlayerState.Error(e.message ?: "Failed to start audio playback")
            progressJob?.cancel()
            AudioPlaybackService.stop(context)
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val duration = player.duration.coerceAtLeast(0)
                    if (player.isPlaying) {
                        _playbackState.value = PlayerState.Playing(currentEntryId, player.currentPosition, duration, currentAudioPath)
                    }
                }
                delay(250)
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun skipForward(offsetMs: Long = 10000L) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + offsetMs).coerceAtMost(player.duration)
            seekTo(target)
        }
    }

    fun skipBackward(offsetMs: Long = 10000L) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - offsetMs).coerceAtLeast(0L)
            seekTo(target)
        }
    }

    fun pause() {
        try {
            exoPlayer?.pause()
        } catch (ignored: Exception) {}
        exoPlayer?.let { player ->
            val dur = player.duration.coerceAtLeast(0)
            _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
        }
        AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
    }

    fun resume() {
        try {
            exoPlayer?.play()
        } catch (e: Exception) {
            _playbackState.value = PlayerState.Error(e.message ?: "Failed to resume audio playback")
            AudioPlaybackService.stop(context)
            return
        }

        exoPlayer?.let { player ->
            if (player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                handlePlaybackSuppression(player.playbackSuppressionReason)
            } else if (player.isPlaying) {
                val dur = player.duration.coerceAtLeast(0)
                _playbackState.value = PlayerState.Playing(currentEntryId, player.currentPosition, dur, currentAudioPath)
                AudioPlaybackService.updateState(context, true, currentTitle, currentEntryId)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            exoPlayer?.seekTo(positionMs)
        } catch (ignored: Exception) {}
        exoPlayer?.let { player ->
            val duration = player.duration.coerceAtLeast(0)
            if (player.isPlaying) {
                _playbackState.value = PlayerState.Playing(currentEntryId, positionMs, duration, currentAudioPath)
            } else if (_playbackState.value !is PlayerState.Error) {
                _playbackState.value = PlayerState.Paused(currentEntryId, positionMs, duration, currentAudioPath)
            }
        }
    }

    fun clearError() {
        if (_playbackState.value is PlayerState.Error) {
            _playbackState.value = PlayerState.Idle
        }
    }

    fun stop() {
        try {
            exoPlayer?.stop()
        } catch (ignored: Exception) {}
        currentAudioPath = null
        _playbackState.value = PlayerState.Idle
        progressJob?.cancel()
        AudioPlaybackService.stop(context)
    }

    fun release() {
        try {
            exoPlayer?.release()
        } catch (ignored: Exception) {}
        exoPlayer = null
        currentAudioPath = null
        _playbackState.value = PlayerState.Idle
        progressJob?.cancel()
        AudioPlaybackService.stop(context)
    }
}

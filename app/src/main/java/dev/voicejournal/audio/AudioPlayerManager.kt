package dev.voicejournal.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    data class Error(val message: String) : PlayerState()
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

    init {
        instance = this
    }

    val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: 0L

    val duration: Long
        get() = exoPlayer?.duration?.coerceAtLeast(0) ?: -1L

    fun play(audioPath: String, title: String = "Voice Note Playback", entryId: Long? = null) {
        currentTitle = title.ifBlank { "Voice Note Playback" }
        currentEntryId = entryId
        currentAudioPath = audioPath

        if (audioPath.isEmpty()) {
            _playbackState.value = PlayerState.Idle
            return
        }

        val audioFile = if (audioPath.startsWith("/")) File(audioPath) else null
        if (audioFile != null && !audioFile.exists()) {
            _playbackState.value = PlayerState.Idle
            return
        }

        val mediaUri: Uri = if (audioFile != null) {
            Uri.fromFile(audioFile)
        } else {
            Uri.parse(audioPath)
        }

        // Stop any existing playback and cancel progress tracking before starting new media
        progressJob?.cancel()
        exoPlayer?.stop()

        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _playbackState.value = PlayerState.Ended
                            progressJob?.cancel()
                            AudioPlaybackService.stop(context)
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
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _playbackState.value = PlayerState.Error("Corrupt audio file or unsupported format")
                        progressJob?.cancel()
                        AudioPlaybackService.stop(context)
                    }
                })
            }
        }

        // Start the foreground service notification first
        AudioPlaybackService.start(context, currentTitle, currentEntryId)

        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
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
        exoPlayer?.pause()
        exoPlayer?.let { player ->
            val dur = player.duration.coerceAtLeast(0)
            _playbackState.value = PlayerState.Paused(currentEntryId, player.currentPosition, dur, currentAudioPath)
        }
        AudioPlaybackService.updateState(context, false, currentTitle, currentEntryId)
    }

    fun resume() {
        exoPlayer?.play()
        exoPlayer?.let { player ->
            val dur = player.duration.coerceAtLeast(0)
            _playbackState.value = PlayerState.Playing(currentEntryId, player.currentPosition, dur, currentAudioPath)
        }
        AudioPlaybackService.updateState(context, true, currentTitle, currentEntryId)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        exoPlayer?.let { player ->
            val duration = player.duration.coerceAtLeast(0)
            if (player.isPlaying) {
                _playbackState.value = PlayerState.Playing(currentEntryId, positionMs, duration, currentAudioPath)
            } else {
                _playbackState.value = PlayerState.Paused(currentEntryId, positionMs, duration, currentAudioPath)
            }
        }
    }

    fun stop() {
        exoPlayer?.stop()
        currentAudioPath = null
        _playbackState.value = PlayerState.Idle
        progressJob?.cancel()
        AudioPlaybackService.stop(context)
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        currentAudioPath = null
        _playbackState.value = PlayerState.Idle
        progressJob?.cancel()
        AudioPlaybackService.stop(context)
        if (instance == this) instance = null
    }

    companion object {
        var instance: AudioPlayerManager? = null
            private set
    }
}

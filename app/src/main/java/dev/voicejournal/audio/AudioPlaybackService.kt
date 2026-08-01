package dev.voicejournal.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import dev.voicejournal.MainActivity
import dev.voicejournal.R

class AudioPlaybackService : Service() {

    private val binder = PlaybackBinder()

    inner class PlaybackBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "AudioPlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onSeekTo(pos: Long) {
                    AudioPlayerManager.instance?.seekTo(pos)
                }
                override fun onPlay() {
                    AudioPlayerManager.instance?.resume()
                }
                override fun onPause() {
                    AudioPlayerManager.instance?.pause()
                }
                override fun onStop() {
                    AudioPlayerManager.instance?.stop()
                }
                override fun onSkipToNext() {
                    AudioPlayerManager.instance?.skipForward()
                }
                override fun onSkipToPrevious() {
                    AudioPlayerManager.instance?.skipBackward()
                }
            })
            isActive = true
        }
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Voice Note Playback"

        when (action) {
            ACTION_START -> {
                val notification = buildNotification(title, isPlaying = true)
                startForegroundCompat(NOTIFICATION_ID, notification)
            }
            ACTION_PAUSE -> {
                val notification = buildNotification(title, isPlaying = false)
                startForegroundCompat(NOTIFICATION_ID, notification)
            }
            ACTION_RESUME -> {
                val notification = buildNotification(title, isPlaying = true)
                startForegroundCompat(NOTIFICATION_ID, notification)
            }
            ACTION_TOGGLE -> {
                AudioPlayerManager.instance?.togglePlayPause()
            }
            ACTION_SKIP_FORWARD -> {
                AudioPlayerManager.instance?.skipForward(10000L)
            }
            ACTION_SKIP_BACKWARD -> {
                AudioPlayerManager.instance?.skipBackward(10000L)
            }
            ACTION_STOP -> {
                AudioPlayerManager.instance?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                id,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        } else {
            startForeground(id, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls for voice note playback"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, isPlaying: Boolean): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Control PendingIntents
        val rewindIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_SKIP_BACKWARD }
        val rewindPendingIntent = PendingIntent.getService(this, 1, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_TOGGLE }
        val togglePendingIntent = PendingIntent.getService(this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val ffIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_SKIP_FORWARD }
        val ffPendingIntent = PendingIntent.getService(this, 3, ffIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val position = AudioPlayerManager.instance?.currentPosition ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        val duration = AudioPlayerManager.instance?.duration ?: -1L

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, position, 1.0f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or 
                    PlaybackStateCompat.ACTION_PLAY or 
                    PlaybackStateCompat.ACTION_PAUSE or 
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )

        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build()
        )

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.let {
            mediaStyle.setMediaSession(it.sessionToken)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing voice note" else "Playback paused")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(mediaStyle)
            .addAction(android.R.drawable.ic_media_rew, "-10s", rewindPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "+10s", ffPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "voice_journal_playback"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "dev.voicejournal.action.START_PLAYBACK"
        const val ACTION_PAUSE = "dev.voicejournal.action.PAUSE_PLAYBACK"
        const val ACTION_RESUME = "dev.voicejournal.action.RESUME_PLAYBACK"
        const val ACTION_TOGGLE = "dev.voicejournal.action.TOGGLE_PLAYBACK"
        const val ACTION_SKIP_FORWARD = "dev.voicejournal.action.SKIP_FORWARD"
        const val ACTION_SKIP_BACKWARD = "dev.voicejournal.action.SKIP_BACKWARD"
        const val ACTION_STOP = "dev.voicejournal.action.STOP_PLAYBACK"
        const val EXTRA_TITLE = "extra_audio_title"

        fun start(context: Context, title: String) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    try { context.startService(intent) } catch (ignored: Exception) {}
                }
            } else {
                try { context.startService(intent) } catch (ignored: Exception) {}
            }
        }

        fun updateState(context: Context, isPlaying: Boolean, title: String) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_RESUME else ACTION_PAUSE
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    try { context.startService(intent) } catch (ignored: Exception) {}
                }
            } else {
                try { context.startService(intent) } catch (ignored: Exception) {}
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            try {
                context.stopService(intent)
            } catch (ignored: Exception) {}
        }
    }
}

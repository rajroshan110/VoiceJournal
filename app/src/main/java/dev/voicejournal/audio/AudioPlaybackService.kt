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

    private var currentEntryId: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Voice Note Playback"
        if (intent.hasExtra(EXTRA_ENTRY_ID)) {
            val id = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
            if (id != -1L) {
                currentEntryId = id
            }
        }

        when (action) {
            ACTION_START -> {
                val notification = buildNotification(title, isPlaying = true)
                startForegroundCompat(NOTIFICATION_ID, notification)
            }
            ACTION_PAUSE -> {
                val notification = buildNotification(title, isPlaying = false)
                try {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                } catch (e: Throwable) {
                    try {
                        startForegroundCompat(NOTIFICATION_ID, notification)
                    } catch (ignored: Throwable) {}
                }
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
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (ignored: Throwable) {}
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(id: Int, notification: android.app.Notification) {
        try {
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
        } catch (e: Throwable) {
            try {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(id, notification)
            } catch (ignored: Throwable) {}
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
        val activeEntryId = AudioPlayerManager.instance?.currentEntryId ?: currentEntryId ?: -1L
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "dev.voicejournal.action.NOTIFICATION_CLICK"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            if (activeEntryId != -1L) {
                putExtra("open_entry_id", activeEntryId)
            }
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val toggleIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_TOGGLE }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val rewindIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_SKIP_BACKWARD }
        val rewindPendingIntent = PendingIntent.getService(
            this,
            2,
            rewindIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val ffIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_SKIP_FORWARD }
        val ffPendingIntent = PendingIntent.getService(
            this,
            3,
            ffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val duration = AudioPlayerManager.instance?.duration ?: -1L
        val currentPosition = AudioPlayerManager.instance?.currentPosition ?: 0L

        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    currentPosition,
                    1f
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
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
        const val EXTRA_ENTRY_ID = "extra_audio_entry_id"

        fun start(context: Context, title: String, entryId: Long? = null) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                entryId?.let { putExtra(EXTRA_ENTRY_ID, it) }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                try { context.startService(intent) } catch (ignored: Throwable) {}
            }
        }

        fun updateState(context: Context, isPlaying: Boolean, title: String, entryId: Long? = null) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_RESUME else ACTION_PAUSE
                putExtra(EXTRA_TITLE, title)
                entryId?.let { putExtra(EXTRA_ENTRY_ID, it) }
            }
            try {
                if (isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                // Safely handle background service start restrictions
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            try {
                context.stopService(intent)
            } catch (ignored: Throwable) {}
        }
    }
}

# VoiceJournal Proguard / R8 Optimization & Obfuscation Rules

# ==============================================================================
# JNI & Whisper.cpp Bindings (CRITICAL)
# ==============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class dev.voicejournal.transcription.WhisperLib { *; }
-keep interface dev.voicejournal.transcription.WhisperLib$* { *; }
-keepclassmembers class dev.voicejournal.transcription.WhisperLib$SegmentCallback {
    public void onNewSegment(java.lang.String);
}

# ==============================================================================
# Room SQLite Persistence
# ==============================================================================
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomOpenHelper
-keepclassmembers class * {
    @androidx.room.Dao *;
}
-keep class dev.voicejournal.data.local.db.entity.** { *; }
-keep class dev.voicejournal.data.local.db.relation.** { *; }

# ==============================================================================
# Domain Models & Backup DTOs
# ==============================================================================
-keep class dev.voicejournal.data.backup.v1.dto.** { *; }
-keep class dev.voicejournal.domain.model.** { *; }

# ==============================================================================
# Dagger Hilt
# ==============================================================================
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.hilt.** { *; }

# ==============================================================================
# Media3 ExoPlayer & Audio Service
# ==============================================================================
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }
-keep class dev.voicejournal.audio.AudioPlaybackService { *; }

# ==============================================================================
# Biometric Authentication
# ==============================================================================
-dontwarn androidx.biometric.**

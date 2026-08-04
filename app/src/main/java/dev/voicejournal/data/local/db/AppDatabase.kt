package dev.voicejournal.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.voicejournal.data.local.db.dao.EntryImageDao
import dev.voicejournal.data.local.db.dao.JournalEntryDao
import dev.voicejournal.data.local.db.dao.TagDao
import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        JournalEntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        EntryImageEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun tagDao(): TagDao
    abstract fun entryImageDao(): EntryImageDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN transcriptCreatedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN transcriptModel TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN transcriptLanguage TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN transcriptVersion TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. journal_entries table
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                val entriesCursor = db.query("SELECT id FROM journal_entries WHERE uuid = '' OR uuid IS NULL")
                while (entriesCursor.moveToNext()) {
                    val id = entriesCursor.getLong(0)
                    val newUuid = java.util.UUID.randomUUID().toString()
                    db.execSQL("UPDATE journal_entries SET uuid = '$newUuid' WHERE id = $id")
                }
                entriesCursor.close()
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_uuid ON journal_entries(uuid)")

                // 2. tags table
                db.execSQL("ALTER TABLE tags ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                val tagsCursor = db.query("SELECT id FROM tags WHERE uuid = '' OR uuid IS NULL")
                while (tagsCursor.moveToNext()) {
                    val id = tagsCursor.getLong(0)
                    val newUuid = java.util.UUID.randomUUID().toString()
                    db.execSQL("UPDATE tags SET uuid = '$newUuid' WHERE id = $id")
                }
                tagsCursor.close()
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_uuid ON tags(uuid)")

                // 3. entry_images table
                db.execSQL("ALTER TABLE entry_images ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                val imagesCursor = db.query("SELECT id FROM entry_images WHERE uuid = '' OR uuid IS NULL")
                while (imagesCursor.moveToNext()) {
                    val id = imagesCursor.getLong(0)
                    val newUuid = java.util.UUID.randomUUID().toString()
                    db.execSQL("UPDATE entry_images SET uuid = '$newUuid' WHERE id = $id")
                }
                imagesCursor.close()
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_entry_images_uuid ON entry_images(uuid)")
            }
        }
    }
}

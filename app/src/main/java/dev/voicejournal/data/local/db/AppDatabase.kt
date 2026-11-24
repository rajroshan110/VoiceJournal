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
    exportSchema = true
)
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
                backfillTableUuids(db, "journal_entries")
                backfillTableUuids(db, "tags")
                backfillTableUuids(db, "entry_images")
            }

            private fun backfillTableUuids(db: SupportSQLiteDatabase, table: String) {
                db.execSQL("ALTER TABLE $table ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.query("SELECT id FROM $table WHERE uuid = '' OR uuid IS NULL").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val newUuid = java.util.UUID.randomUUID().toString()
                        db.execSQL("UPDATE $table SET uuid = ? WHERE id = ?", arrayOf<Any>(newUuid, id))
                    }
                }
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON $table(uuid)")
            }
        }
    }
}

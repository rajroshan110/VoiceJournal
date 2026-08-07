package dev.voicejournal.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.dao.EntryImageDao
import dev.voicejournal.data.local.db.dao.JournalEntryDao
import dev.voicejournal.data.local.db.dao.TagDao
import dev.voicejournal.data.repository.JournalRepositoryImpl
import dev.voicejournal.domain.repository.JournalRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_journal_db"
        ).addMigrations(
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideJournalEntryDao(db: AppDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideEntryImageDao(db: AppDatabase): EntryImageDao = db.entryImageDao()

    @Provides
    @Singleton
    fun provideJournalRepository(
        @ApplicationContext context: Context,
        journalEntryDao: JournalEntryDao,
        tagDao: TagDao,
        entryImageDao: EntryImageDao,
        prefs: UserPreferencesManager,
        appDatabase: AppDatabase
    ): JournalRepository {
        return JournalRepositoryImpl(context, journalEntryDao, tagDao, entryImageDao, prefs, appDatabase)
    }
}

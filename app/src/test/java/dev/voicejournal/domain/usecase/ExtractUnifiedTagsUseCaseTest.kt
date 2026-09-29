package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtractUnifiedTagsUseCaseTest {

    private lateinit var useCase: ExtractUnifiedTagsUseCase

    @Before
    fun setUp() {
        useCase = ExtractUnifiedTagsUseCase()
    }

    @Test
    fun testTopicsCaseInsensitiveDeduplicationAndSanitization() {
        // Tag in DB with prefix "#Songs" and ID 42
        val dbTags = listOf(
            Tag(id = 42L, name = "#Songs", type = TagType.TOPIC)
        )
        // Entry with tag "songs" (lowercase, no prefix)
        val entries = listOf(
            JournalEntry(
                id = 1L,
                tags = listOf(Tag(id = 0L, name = "songs", type = TagType.TOPIC))
            )
        )

        val result = useCase(entries, dbTags)

        // Must produce exactly ONE topic tag
        assertEquals(1, result.allTopics.size)
        val topic = result.allTopics.first()
        // ID should match the DB tag ID
        assertEquals(42L, topic.id)
        // Name should be sanitized (no '#')
        assertEquals("Songs", topic.name)
        assertEquals(TagType.TOPIC, topic.type)
    }

    @Test
    fun testNoDuplicateTagIdsInLazyGridKeys() {
        // DB tag has "#Songs", entry has "songs" and "#SONGS"
        val dbTags = listOf(
            Tag(id = 10L, name = "#Songs", type = TagType.TOPIC),
            Tag(id = 20L, name = "Ideas", type = TagType.TOPIC)
        )
        val entries = listOf(
            JournalEntry(
                id = 1L,
                tags = listOf(
                    Tag(id = 0L, name = "songs", type = TagType.TOPIC),
                    Tag(id = 0L, name = "#SONGS", type = TagType.TOPIC),
                    Tag(id = 0L, name = "ideas", type = TagType.TOPIC)
                )
            )
        )

        val result = useCase(entries, dbTags)

        // Verify distinct topics
        assertEquals(2, result.allTopics.size)
        // Verify all IDs are completely unique (no Compose key crash)
        val topicIds = result.allTopics.map { it.id }
        assertEquals(topicIds.distinct().size, topicIds.size)
    }

    @Test
    fun testPeopleCaseInsensitiveDeduplication() {
        val dbTags = listOf(
            Tag(id = 100L, name = "@Alice", type = TagType.PERSON)
        )
        val entries = listOf(
            JournalEntry(
                id = 1L,
                tags = listOf(
                    Tag(id = 0L, name = "alice", type = TagType.PERSON),
                    Tag(id = 0L, name = "@Bob", type = TagType.PERSON)
                )
            )
        )

        val result = useCase(entries, dbTags)

        // People strings sorted case-insensitively
        assertEquals(listOf("Alice", "Bob"), result.allPeople)
        // Person tags
        assertEquals(2, result.allPersonTags.size)
        val aliceTag = result.allPersonTags.find { it.name.equals("Alice", ignoreCase = true) }
        assertEquals(100L, aliceTag?.id)
        assertEquals("Alice", aliceTag?.name)
    }

    @Test
    fun testFoldersCaseInsensitiveDeduplication() {
        val dbTags = listOf(
            Tag(id = 50L, name = "Personal", type = TagType.FOLDER)
        )
        val entries = listOf(
            JournalEntry(
                id = 1L,
                tags = listOf(
                    Tag(id = 0L, name = "personal", type = TagType.FOLDER),
                    Tag(id = 0L, name = "WORK", type = TagType.FOLDER)
                )
            )
        )

        val result = useCase(entries, dbTags)

        assertEquals(2, result.allFolders.size)
        val personalFolder = result.allFolders.find { it.name.equals("Personal", ignoreCase = true) }
        assertEquals(50L, personalFolder?.id)
        assertEquals("Personal", personalFolder?.name)
    }

    @Test
    fun testEmptyAndBlankTagsFilteredOut() {
        val entries = listOf(
            JournalEntry(
                id = 1L,
                tags = listOf(
                    Tag(id = 0L, name = "   ", type = TagType.TOPIC),
                    Tag(id = 0L, name = "#", type = TagType.TOPIC),
                    Tag(id = 0L, name = "@", type = TagType.PERSON)
                )
            )
        )

        val result = useCase(entries, emptyList())

        assertTrue(result.allTopics.isEmpty())
        assertTrue(result.allPersonTags.isEmpty())
        assertTrue(result.allPeople.isEmpty())
    }
}

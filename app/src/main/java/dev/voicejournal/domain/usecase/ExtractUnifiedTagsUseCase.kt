package dev.voicejournal.domain.usecase

import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractUnifiedTagsUseCase @Inject constructor() {

    data class ExtractedData(
        val allTags: List<Tag>,
        val allPeople: List<String>,
        val allTopics: List<Tag>,
        val allFolders: List<Tag>,
        val allPersonTags: List<Tag>
    )

    private fun String.sanitize(): String = this.trim().trimStart('#', '@').trim()

    operator fun invoke(rawEntries: List<JournalEntry>, rawTags: List<Tag>): ExtractedData {
        // People
        val personTagNames = rawTags.filter { it.type == TagType.PERSON }.map { it.name.sanitize() }
        val entryPersonTags = rawEntries.flatMap { entry ->
            entry.tags.filter { it.type == TagType.PERSON }.map { it.name.sanitize() }
        }
        val entryPeopleFields = rawEntries.flatMap { it.people.map { p -> p.sanitize() } }
        
        val extractedPeople = (personTagNames + entryPersonTags + entryPeopleFields)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            
        val allPersonTags = extractedPeople.mapIndexed { i, name ->
            val match = rawTags.find { it.type == TagType.PERSON && it.name.sanitize().equals(name, ignoreCase = true) }
            match?.copy(name = name) ?: Tag(i.toLong() + 30000, name, TagType.PERSON)
        }

        // Topics
        val topicTagNames = rawTags.filter { it.type == TagType.TOPIC }.map { it.name.sanitize() }
        val entryTopicNames = rawEntries.flatMap { entry -> 
            entry.tags.filter { it.type == TagType.TOPIC }.map { it.name.sanitize() } 
        }
        val allTopicNames = (topicTagNames + entryTopicNames)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            
        val allTopics = allTopicNames.mapIndexed { i, name ->
            val match = rawTags.find { it.type == TagType.TOPIC && it.name.sanitize().equals(name, ignoreCase = true) }
            match?.copy(name = name) ?: Tag(i.toLong() + 20000, name, TagType.TOPIC)
        }

        // Folders/Things
        val folderTagNames = rawTags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }.map { it.name.sanitize() }
        val entryFolderTags = rawEntries.flatMap { entry ->
            entry.tags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }.map { it.name.sanitize() }
        }
        val allFolderNames = (folderTagNames + entryFolderTags)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            
        val allFolders = allFolderNames.mapIndexed { i, name ->
            val match = rawTags.find { (it.type == TagType.FOLDER || it.type == TagType.THING) && it.name.sanitize().equals(name, ignoreCase = true) }
            match?.copy(name = name) ?: Tag(i.toLong() + 10000, name, TagType.FOLDER)
        }

        val allTagsList = allTopics + allFolders + allPersonTags

        return ExtractedData(
            allTags = allTagsList,
            allPeople = extractedPeople,
            allTopics = allTopics,
            allFolders = allFolders,
            allPersonTags = allPersonTags
        )
    }
}

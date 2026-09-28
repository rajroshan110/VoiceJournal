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

    private fun String.sanitize(): String = this.removePrefix("#").removePrefix("@").trim()

    operator fun invoke(rawEntries: List<JournalEntry>, rawTags: List<Tag>): ExtractedData {
        // People
        val personTagNames = rawTags.filter { it.type == TagType.PERSON }.map { it.name.sanitize() }
        val entryPersonTags = rawEntries.flatMap { entry ->
            entry.tags.filter { it.type == TagType.PERSON }.map { it.name.sanitize() }
        }
        val entryPeopleFields = rawEntries.flatMap { it.people.map { p -> p.sanitize() } }
        
        val extractedPeople = (personTagNames + entryPersonTags + entryPeopleFields)
            .distinct()
            .filter { it.isNotBlank() }
            .sorted()
            
        val allPersonTags = extractedPeople.mapIndexed { i, name ->
            rawTags.find { it.type == TagType.PERSON && it.name.sanitize().equals(name, ignoreCase = true) }
                ?: Tag(i.toLong() + 30000, name, TagType.PERSON)
        }

        // Topics
        val topicTagNames = rawTags.filter { it.type == TagType.TOPIC }.map { it.name.sanitize() }
        val entryTopicNames = rawEntries.flatMap { entry -> 
            entry.tags.filter { it.type == TagType.TOPIC }.map { it.name.sanitize() } 
        }
        val allTopicNames = (topicTagNames + entryTopicNames)
            .distinct()
            .filter { it.isNotBlank() }
            .sorted()
            
        val allTopics = allTopicNames.mapIndexed { i, name ->
            rawTags.find { it.type == TagType.TOPIC && it.name.sanitize().equals(name, ignoreCase = true) }
                ?: Tag(i.toLong() + 20000, name, TagType.TOPIC)
        }

        // Folders/Things
        val folderTagNames = rawTags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }.map { it.name.sanitize() }
        val entryFolderTags = rawEntries.flatMap { entry ->
            entry.tags.filter { it.type == TagType.FOLDER || it.type == TagType.THING }.map { it.name.sanitize() }
        }
        val allFolderNames = (folderTagNames + entryFolderTags)
            .distinct()
            .filter { it.isNotBlank() }
            .sorted()
            
        val allFolders = allFolderNames.mapIndexed { i, name ->
            rawTags.find { (it.type == TagType.FOLDER || it.type == TagType.THING) && it.name.sanitize().equals(name, ignoreCase = true) }
                ?: Tag(i.toLong() + 10000, name, TagType.FOLDER)
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

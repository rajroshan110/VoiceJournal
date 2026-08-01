package dev.voicejournal.ui.journal.components

import androidx.compose.runtime.Composable
import dev.voicejournal.domain.model.Tag

@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    tags: List<Tag>,
    onTagSelect: (Tag) -> Unit
) {
    TopicFilterBottomSheet(
        availableTags = tags,
        selectedTagNames = emptySet(),
        onTagToggle = { tagName ->
            val foundTag = tags.firstOrNull { it.name == tagName }
            if (foundTag != null) {
                onTagSelect(foundTag)
            }
        },
        onClearAll = {},
        onDismissRequest = onDismiss
    )
}

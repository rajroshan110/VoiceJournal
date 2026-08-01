package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.ui.components.TagChip

@Composable
fun CalendarFilterBar(
    tags: List<Tag>,
    activeFilter: Tag?,
    onFilterSelect: (Tag?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TagChip(
                tag = Tag(-1L, "All", dev.voicejournal.domain.model.TagType.TOPIC),
                selected = activeFilter == null,
                onClick = { onFilterSelect(null) }
            )
        }
        items(tags) { tag ->
            TagChip(
                tag = tag,
                selected = activeFilter == tag,
                onClick = { onFilterSelect(tag) }
            )
        }
    }
}

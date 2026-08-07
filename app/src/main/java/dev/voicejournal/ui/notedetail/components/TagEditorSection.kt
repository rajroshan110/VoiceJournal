package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.Tag
import dev.voicejournal.domain.model.TagType
import dev.voicejournal.ui.components.TagChip
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun TagEditorSection(
    tags: List<Tag>,
    onAddTag: (Tag) -> Unit,
    onRemoveTag: (Tag) -> Unit
) {
    val colors = AppTheme.colors
    var showInput by rememberSaveable { mutableStateOf(false) }
    var tagInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tags) { tag ->
                TagChip(
                    tag = tag,
                    onDelete = { onRemoveTag(tag) }
                )
            }

            item {
                if (!showInput) {
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceVariant, RoundedCornerShape(20.dp))
                            .clickable { showInput = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add tags", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (showInput) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tagInput,
                onValueChange = { input ->
                    if (input.endsWith(" ") || input.endsWith("\n")) {
                        val trimmed = input.trim()
                        if (trimmed.isNotEmpty()) {
                            val type = when {
                                trimmed.startsWith("#") -> TagType.TOPIC
                                trimmed.startsWith("@") -> TagType.PERSON
                                else -> TagType.FOLDER
                            }
                            onAddTag(Tag(name = trimmed.removePrefix("#").removePrefix("@"), type = type))
                        }
                        tagInput = ""
                        showInput = false
                    } else {
                        tagInput = input
                    }
                },
                placeholder = { Text("Enter tag (#topic, @person)...", color = colors.textSecondary, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colors.border,
                    focusedBorderColor = colors.primary,
                    unfocusedContainerColor = colors.surface,
                    focusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )
        }
    }
}

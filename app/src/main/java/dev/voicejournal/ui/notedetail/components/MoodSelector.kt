package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MoodSelector(
    onMoodSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("😊", "😌", "😔", "😤", "🤔", "😴").forEach { mood ->
            Text(text = mood, modifier = Modifier.padding(8.dp))
        }
    }
}

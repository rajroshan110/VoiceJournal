package dev.voicejournal.ui.journal.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.voicejournal.ui.designsystem.components.buttons.AppFab

@Composable
fun MicFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppFab(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create new journal entry"
        )
    }
}

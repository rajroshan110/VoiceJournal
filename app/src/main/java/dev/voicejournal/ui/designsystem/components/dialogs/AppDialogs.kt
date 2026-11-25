package dev.voicejournal.ui.designsystem.components.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.DialogProperties
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.ComponentDimens
import dev.voicejournal.ui.designsystem.tokens.Radius

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(ComponentDimens.DialogCornerRadius),
    containerColor: Color = AppTheme.colors.surface,
    properties: DialogProperties = DialogProperties()
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = colors.primary,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        properties = properties
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.textPrimary,
    dragHandle: (@Composable () -> Unit)? = {
        BottomSheetDefaults.DragHandle(color = AppTheme.colors.divider)
    },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = Radius.RadiusSheet,
            topEnd = Radius.RadiusSheet
        ),
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = dragHandle,
        content = content
    )
}

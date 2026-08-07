package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.renderer.RichTextVisualTransformation
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun UserTextInput(
    richTextState: RichTextState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val visualTransformation = remember(richTextState.document) {
        RichTextVisualTransformation { richTextState.document }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 450.dp)
            .clickable { focusRequester.requestFocus() }
            .padding(vertical = 4.dp)
    ) {
        if (richTextState.document.isEmpty) {
            Text(
                text = "Add your thoughts, questions, or notes...",
                color = colors.textSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
        BasicTextField(
            value = richTextState.textFieldValue,
            onValueChange = { newValue ->
                richTextState.onTextFieldValueChange(newValue)
            },
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily.SansSerif
            ),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .focusRequester(focusRequester)
        )
    }
}

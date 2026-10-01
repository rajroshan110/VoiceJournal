package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.renderer.RichTextVisualTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserTextInput(
    richTextState: RichTextState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val visualTransformation = remember(richTextState.document, colors.isLight) {
        RichTextVisualTransformation(
            linkColor = colors.primary,
            quoteColor = colors.textSecondary,
            codeBackground = if (colors.isLight) Color(0xFFE5DACB) else Color(0xFF383838),
            codeTextColor = colors.textPrimary
        ) { richTextState.document }
    }

    val textSelectionColors = remember(colors.primary) {
        TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = colors.primary.copy(alpha = 0.35f)
        )
    }

    var isTextFieldFocused by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorBringIntoViewRequester = remember { BringIntoViewRequester() }

    val currentTextLayoutResult by rememberUpdatedState(textLayoutResult)
    val currentSelection by rememberUpdatedState(richTextState.textFieldValue.selection)
    val currentText by rememberUpdatedState(richTextState.textFieldValue.text)

    val imeInsets = WindowInsets.ime
    val imeBottom = imeInsets.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    suspend fun bringCursorIntoView(source: String) {
        val layout = currentTextLayoutResult ?: return
        val rawText = currentText
        val selection = currentSelection
        val rawOffset = selection.start.coerceIn(0, rawText.length)
        val transformed = visualTransformation.filter(AnnotatedString(rawText))
        val transformedOffset = transformed.offsetMapping.originalToTransformed(rawOffset)
            .coerceIn(0, layout.layoutInput.text.length)
        val rawCursorRect = layout.getCursorRect(transformedOffset)
        cursorBringIntoViewRequester.bringIntoView(rawCursorRect)
    }

    // 1. When the keyboard opens or resizes, wait for the IME animation to settle
    // so the container reaches its final open height before bringing the cursor into view.
    LaunchedEffect(isTextFieldFocused) {
        if (!isTextFieldFocused) return@LaunchedEffect
        snapshotFlow { imeInsets.getBottom(density) }
            .distinctUntilChanged()
            .collectLatest { bottom ->
                if (bottom > 0 && currentSelection.collapsed) {
                    delay(50)
                    bringCursorIntoView("IME settled ($bottom px)")
                }
            }
    }

    // 2. When the selection changes, text layout is updated, or keyboard becomes open:
    // bring the cursor into view immediately only when selection is collapsed (not dragging selection range).
    LaunchedEffect(richTextState.textFieldValue.selection, textLayoutResult, isKeyboardOpen) {
        if (isTextFieldFocused && isKeyboardOpen && richTextState.textFieldValue.selection.collapsed) {
            bringCursorIntoView("selection/layout/imeOpen")
        }
    }

    // Isolate BasicTextField's internal bring-into-view so it does not evaluate
    // scroll calculations against its own unbounded height and swallow requests.
    val textFieldBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 350.dp)
            .padding(vertical = 4.dp)
            .clipToBounds()
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
        CompositionLocalProvider(
            LocalTextSelectionColors provides textSelectionColors,
            LocalBringIntoViewSpec provides textFieldBringIntoViewSpec
        ) {
            BasicTextField(
                value = richTextState.textFieldValue,
                onValueChange = { newValue ->
                    richTextState.onTextFieldValueChange(newValue)
                },
                onTextLayout = { layout ->
                    textLayoutResult = layout
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
                    .defaultMinSize(minHeight = 350.dp)
                    .bringIntoViewRequester(cursorBringIntoViewRequester)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isTextFieldFocused = focusState.isFocused
                    }
            )
        }
    }
}

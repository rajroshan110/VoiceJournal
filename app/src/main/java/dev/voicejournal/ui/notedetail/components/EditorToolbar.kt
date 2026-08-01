package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.theme.AppTheme

@Composable
fun EditorToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showLinkDialog by remember { mutableStateOf(false) }

    // Active States
    val isBoldActive = richTextState.isSpanActive(SpanType.Bold)
    val isItalicActive = richTextState.isSpanActive(SpanType.Italic)
    val isUnderlineActive = richTextState.isSpanActive(SpanType.Underline)
    val isStrikethroughActive = richTextState.isSpanActive(SpanType.Strikethrough)
    val isHighlightActive = richTextState.isSpanActive(SpanType.Highlight())
    val isLinkActive = richTextState.isLinkActive()

    val isQuoteActive = richTextState.isParagraphActive(ParagraphType.Quote)
    val isCodeActive = richTextState.isSpanActive(SpanType.Code)
    val isBulletListActive = richTextState.isParagraphActive(ParagraphType.BulletList)
    val isNumberedListActive = richTextState.isParagraphActive(ParagraphType.NumberedList())

    val isH1Active = richTextState.isParagraphActive(ParagraphType.Heading(1))
    val isH2Active = richTextState.isParagraphActive(ParagraphType.Heading(2))
    val isH3Active = richTextState.isParagraphActive(ParagraphType.Heading(3))
    val isH4Active = richTextState.isParagraphActive(ParagraphType.Heading(4))
    val isH5Active = richTextState.isParagraphActive(ParagraphType.Heading(5))
    val isH6Active = richTextState.isParagraphActive(ParagraphType.Heading(6))

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable Tools Bar (Groups 1, 2, 3)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GROUP 1: INLINE SPANS (B, I, U, Strikethrough, Highlight, Link)
                ToolbarButton(isActive = isBoldActive, onClick = { richTextState.toggleSpan(SpanType.Bold) }) {
                    Text("B", color = if (isBoldActive) colors.primary else colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                ToolbarButton(isActive = isItalicActive, onClick = { richTextState.toggleSpan(SpanType.Italic) }) {
                    Text("I", color = if (isItalicActive) colors.primary else colors.textPrimary, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                ToolbarButton(isActive = isUnderlineActive, onClick = { richTextState.toggleSpan(SpanType.Underline) }) {
                    Text("U", color = if (isUnderlineActive) colors.primary else colors.textPrimary, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                ToolbarButton(isActive = isStrikethroughActive, onClick = { richTextState.toggleSpan(SpanType.Strikethrough) }) {
                    Text("S", color = if (isStrikethroughActive) colors.primary else colors.textPrimary, textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                ToolbarButton(isActive = isHighlightActive, onClick = { richTextState.toggleSpan(SpanType.Highlight()) }) {
                    Text("🖍", fontSize = 15.sp)
                }

                ToolbarButton(isActive = isLinkActive, onClick = { showLinkDialog = true }) {
                    Text("🔗", fontSize = 14.sp)
                }

                ToolbarDivider()

                // GROUP 2: BLOCKS & LISTS (Quote "99", Code <>, Bullet List, Numbered List)
                ToolbarButton(isActive = isQuoteActive, onClick = { richTextState.toggleParagraph(ParagraphType.Quote) }) {
                    Text("99", color = if (isQuoteActive) colors.primary else colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isCodeActive, onClick = { richTextState.toggleSpan(SpanType.Code) }) {
                    Text("<>", color = if (isCodeActive) colors.primary else colors.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isBulletListActive, onClick = { richTextState.toggleParagraph(ParagraphType.BulletList) }) {
                    Text("•≡", color = if (isBulletListActive) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isNumberedListActive, onClick = { richTextState.toggleParagraph(ParagraphType.NumberedList()) }) {
                    Text("1≡", color = if (isNumberedListActive) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                ToolbarDivider()

                // GROUP 3: HEADINGS (H1..H6)
                ToolbarButton(isActive = isH1Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(1)) }) {
                    Text("H1", color = if (isH1Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isH2Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(2)) }) {
                    Text("H2", color = if (isH2Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isH3Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(3)) }) {
                    Text("H3", color = if (isH3Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isH4Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(4)) }) {
                    Text("H4", color = if (isH4Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isH5Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(5)) }) {
                    Text("H5", color = if (isH5Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                ToolbarButton(isActive = isH6Active, onClick = { richTextState.toggleParagraph(ParagraphType.Heading(6)) }) {
                    Text("H6", color = if (isH6Active) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // GROUP 4: UNDO / REDO (Consistently placed at right end)
            VerticalDivider(
                modifier = Modifier
                    .height(22.dp)
                    .padding(horizontal = 6.dp),
                color = colors.divider
            )

            IconButton(
                onClick = { richTextState.undo() },
                enabled = richTextState.canUndo,
                modifier = Modifier.size(38.dp)
            ) {
                Text(
                    "↶",
                    color = if (richTextState.canUndo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { richTextState.redo() },
                enabled = richTextState.canRedo,
                modifier = Modifier.size(38.dp)
            ) {
                Text(
                    "↷",
                    color = if (richTextState.canRedo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Link Input Modal Dialog
    if (showLinkDialog) {
        var urlText by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Insert Link", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (urlText.isNotBlank()) {
                        richTextState.toggleLink(urlText)
                    }
                    showLinkDialog = false
                }) {
                    Text("Apply", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ToolbarButton(
    isActive: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val backgroundColor = if (isActive) colors.primaryContainer else Color.Transparent

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        content()
    }
}

@Composable
private fun ToolbarDivider() {
    val colors = AppTheme.colors
    VerticalDivider(
        modifier = Modifier
            .height(22.dp)
            .padding(horizontal = 4.dp),
        color = colors.divider
    )
}

package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.designsystem.theme.AppTheme

@Composable
fun EditorToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }

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
    val scrollState = rememberScrollState()

    val isH3Active = richTextState.isParagraphActive(ParagraphType.Heading(3))
    val isH4Active = richTextState.isParagraphActive(ParagraphType.Heading(4))
    val isH5Active = richTextState.isParagraphActive(ParagraphType.Heading(5))
    val isH6Active = richTextState.isParagraphActive(ParagraphType.Heading(6))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        LinkActionPopup(
            richTextState = richTextState,
            onEditLink = { showLinkDialog = true }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable formatting ribbon containing the 3 format groups
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // GROUP 1: Character Spans
                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Bold),
                        contentDescription = "Bold",
                        onClick = { richTextState.toggleSpan(SpanType.Bold) }
                    ) {
                        Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Italic),
                        contentDescription = "Italic",
                        onClick = { richTextState.toggleSpan(SpanType.Italic) }
                    ) {
                        Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Underline),
                        contentDescription = "Underline",
                        onClick = { richTextState.toggleSpan(SpanType.Underline) }
                    ) {
                        Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Strikethrough),
                        contentDescription = "Strikethrough",
                        onClick = { richTextState.toggleSpan(SpanType.Strikethrough) }
                    ) {
                        Text("S", textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Highlight()),
                        contentDescription = "Highlight",
                        onClick = { richTextState.toggleSpan(SpanType.Highlight()) }
                    ) {
                        Text("🎨", fontSize = 14.sp)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Code),
                        contentDescription = "Code",
                        onClick = { richTextState.toggleSpan(SpanType.Code) }
                    ) {
                        Text("</>", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isLinkActive(),
                        contentDescription = "Insert Link",
                        onClick = { showLinkDialog = true }
                    ) {
                        Text("🔗", fontSize = 14.sp)
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(22.dp)
                            .padding(horizontal = 4.dp),
                        color = colors.textSecondary.copy(alpha = 0.25f)
                    )

                    // GROUP 2: Blocks & Lists
                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.Quote),
                        contentDescription = "Quote",
                        onClick = { richTextState.toggleParagraph(ParagraphType.Quote) }
                    ) {
                        Text("❝", fontSize = 16.sp, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.BulletList),
                        contentDescription = "Bullet List",
                        onClick = { richTextState.toggleParagraph(ParagraphType.BulletList) }
                    ) {
                        Text("•≡", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }

                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.NumberedList()),
                        contentDescription = "Numbered List",
                        onClick = { richTextState.toggleParagraph(ParagraphType.NumberedList()) }
                    ) {
                        Text("1≡", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(22.dp)
                            .padding(horizontal = 4.dp),
                        color = colors.textSecondary.copy(alpha = 0.25f)
                    )

                    // GROUP 3: Headings H1–H6
                    for (level in 1..6) {
                        ToolbarButton(
                            isActive = richTextState.isParagraphActive(ParagraphType.Heading(level)),
                            contentDescription = "Heading $level",
                            onClick = { richTextState.toggleParagraph(ParagraphType.Heading(level)) }
                        ) {
                            Text(
                                text = "H$level",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 4.dp),
                    color = colors.textSecondary.copy(alpha = 0.3f)
                )

                // GROUP 4: History (Fixed right-aligned Undo/Redo)
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
    }

    // Link Input Modal Dialog
    if (showLinkDialog) {
        val activeLink = richTextState.getLinkAtCursor()
        val selStart = minOf(richTextState.selection.start, richTextState.selection.end)
        val selEnd = maxOf(richTextState.selection.start, richTextState.selection.end)

        val initialSelectedText = if (activeLink != null) {
            val s = activeLink.start.coerceIn(0, richTextState.document.length)
            val e = activeLink.end.coerceIn(0, richTextState.document.length)
            richTextState.document.text.substring(s, e)
        } else if (selStart < selEnd && selEnd <= richTextState.document.text.length) {
            richTextState.document.text.substring(selStart, selEnd)
        } else ""

        val initialUrl = if (activeLink != null) {
            (activeLink.type as? SpanType.Link)?.url ?: ""
        } else ""

        var linkText by rememberSaveable(activeLink, showLinkDialog) { mutableStateOf(initialSelectedText) }
        var urlText by rememberSaveable(activeLink, showLinkDialog) { mutableStateOf(initialUrl) }

        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(if (activeLink != null) "Edit Link" else "Insert Link", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label = { Text("Link Text") },
                        placeholder = { Text("Text to display", color = colors.textSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Link URL") },
                        placeholder = { Text("https://example.com", color = colors.textSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeLink != null) {
                        TextButton(onClick = {
                            richTextState.removeLinkAtCursor()
                            showLinkDialog = false
                        }) {
                            Text("Remove Link", color = colors.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(onClick = {
                        if (urlText.isNotBlank()) {
                            richTextState.insertLink(displayText = linkText, url = urlText.trim())
                        }
                        showLinkDialog = false
                    }) {
                        Text("Apply", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
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
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val backgroundColor = if (isActive) colors.primary.copy(alpha = if (colors.isLight) 0.18f else 0.25f) else Color.Transparent
    val borderStroke = if (isActive) BorderStroke(1.dp, colors.primary) else null

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Checkbox
            )
            .semantics {
                this.contentDescription = contentDescription
                this.selected = isActive
            },
        contentAlignment = Alignment.Center
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

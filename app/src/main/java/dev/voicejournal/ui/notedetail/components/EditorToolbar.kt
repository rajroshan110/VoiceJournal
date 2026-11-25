package dev.voicejournal.ui.notedetail.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
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
import dev.voicejournal.ui.designsystem.components.dialogs.AppAlertDialog
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Border
import dev.voicejournal.ui.designsystem.tokens.IconSize
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.notedetail.editor.engine.RichTextState
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.notedetail.editor.model.TextAlignment

@Composable
fun EditorToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val windowInfo = AppTheme.windowInfo
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

    val scrollState = rememberScrollState()
    var showHeadingBar by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = windowInfo.isCompactLandscape || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Detect active heading level (1..6) if any at the caret
    val activeHeadingLevel = (1..6).firstOrNull { level ->
        richTextState.isParagraphActive(ParagraphType.Heading(level))
    }
    val isHeadingActive = activeHeadingLevel != null
    val currentAlignment = richTextState.getAlignmentAtCursor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        LinkActionPopup(
            richTextState = richTextState,
            onEditLink = { showLinkDialog = true }
        )

        // Expandable Paragraph & Heading Selector Bar (opens above the ribbon)
        AnimatedVisibility(
            visible = showHeadingBar,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(if (isLandscape) Alignment.End else Alignment.CenterHorizontally)
        ) {
            Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(Radius.RadiusLg),
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    border = BorderStroke(Border.WidthThin, colors.textSecondary.copy(alpha = 0.15f)),
                    modifier = if (isLandscape) {
                        Modifier
                            .padding(end = 52.dp, bottom = Spacing.SpaceX2s)
                            .widthIn(max = (windowInfo.normalizedWidthDp * 0.72f).dp.coerceAtMost(420.dp))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.SpaceXs, vertical = Spacing.SpaceX2s)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.SpaceXs, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.spacedBy(Spacing.SpaceX2s)
                    ) {
                        // Normal Body Paragraph (clears active heading)
                        ToolbarButton(
                            isActive = !isHeadingActive,
                            contentDescription = "Normal Body Text",
                            modifier = Modifier
                                .defaultMinSize(minWidth = 58.dp, minHeight = 40.dp)
                                .padding(horizontal = Spacing.SpaceX3s),
                            onClick = {
                                if (activeHeadingLevel != null) {
                                    richTextState.toggleParagraph(ParagraphType.Heading(activeHeadingLevel))
                                }
                            }
                        ) {
                            Text(
                                text = "¶ Body",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isHeadingActive) FontWeight.Bold else FontWeight.Normal,
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = Spacing.SpaceX3s),
                            color = colors.textSecondary.copy(alpha = 0.25f)
                        )

                        // Headings H1 through H6
                        for (level in 1..6) {
                            val isLevelActive = activeHeadingLevel == level
                            ToolbarButton(
                                isActive = isLevelActive,
                                contentDescription = "Heading $level",
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                    .padding(horizontal = 1.dp),
                                onClick = {
                                    richTextState.toggleParagraph(ParagraphType.Heading(level))
                                }
                            ) {
                                Text(
                                    text = "H$level",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLevelActive) FontWeight.Bold else FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f, fill = false))

                        IconButton(
                            onClick = { showHeadingBar = false },
                            modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Heading Selector",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(IconSize.IconSm)
                            )
                        }
                    }
                }
            }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.SpaceXs, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable formatting ribbon containing the 3 format groups
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceX2s)
                ) {
                    // GROUP 1: Character Spans
                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Bold),
                        contentDescription = "Bold",
                        onClick = { richTextState.toggleSpan(SpanType.Bold) }
                    ) {
                        Text("B", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Italic),
                        contentDescription = "Italic",
                        onClick = { richTextState.toggleSpan(SpanType.Italic) }
                    ) {
                        Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Underline),
                        contentDescription = "Underline",
                        onClick = { richTextState.toggleSpan(SpanType.Underline) }
                    ) {
                        Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Strikethrough),
                        contentDescription = "Strikethrough",
                        onClick = { richTextState.toggleSpan(SpanType.Strikethrough) }
                    ) {
                        Text("S", textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Highlight()),
                        contentDescription = "Highlight",
                        onClick = { richTextState.toggleSpan(SpanType.Highlight()) }
                    ) {
                        Text("🎨", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isSpanActive(SpanType.Code),
                        contentDescription = "Code",
                        onClick = { richTextState.toggleSpan(SpanType.Code) }
                    ) {
                        Text("</>", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isLinkActive(),
                        contentDescription = "Insert Link",
                        onClick = { showLinkDialog = true }
                    ) {
                        Text("🔗", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(22.dp)
                            .padding(horizontal = Spacing.SpaceX2s),
                        color = colors.textSecondary.copy(alpha = 0.25f)
                    )

                    // GROUP 2: Blocks & Lists
                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.Quote),
                        contentDescription = "Quote",
                        onClick = { richTextState.toggleParagraph(ParagraphType.Quote) }
                    ) {
                        Text("❝", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.BulletList),
                        contentDescription = "Bullet List",
                        onClick = { richTextState.toggleParagraph(ParagraphType.BulletList) }
                    ) {
                        Text("•≡", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    ToolbarButton(
                        isActive = richTextState.isParagraphActive(ParagraphType.NumberedList()),
                        contentDescription = "Numbered List",
                        onClick = { richTextState.toggleParagraph(ParagraphType.NumberedList()) }
                    ) {
                        Text("1≡", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(22.dp)
                            .padding(horizontal = Spacing.SpaceX2s),
                        color = colors.textSecondary.copy(alpha = 0.25f)
                    )

                    // GROUP 3: Structure & Formatting (Paragraph/Heading & Alignment)
                    // Consolidates H1–H6 into a single adaptive button that displays the active heading
                    // and opens the heading bar above the ribbon with an upward pointer (▴).
                    val headingButtonText = if (activeHeadingLevel != null) "H$activeHeadingLevel ▴" else "¶ ▴"
                    ToolbarButton(
                        isActive = isHeadingActive || showHeadingBar,
                        contentDescription = if (activeHeadingLevel != null) "Heading $activeHeadingLevel (Tap to change)" else "Paragraph Style (Tap to change)",
                        modifier = Modifier
                            .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                            .padding(horizontal = Spacing.SpaceX3s),
                        onClick = { showHeadingBar = !showHeadingBar }
                    ) {
                        Text(
                            text = headingButtonText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Traditional Line Alignment Button: click-only cycle (Left -> Center -> Right -> Reset)
                    val isAlignActive = currentAlignment != null
                    val activeAlign = currentAlignment ?: TextAlignment.Start
                    ToolbarButton(
                        isActive = isAlignActive,
                        contentDescription = when (currentAlignment) {
                            TextAlignment.Start -> "Text Alignment: Left"
                            TextAlignment.Center -> "Text Alignment: Center"
                            TextAlignment.End -> "Text Alignment: Right"
                            null -> "Text Alignment: Default (Tap to align)"
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                            .padding(horizontal = Spacing.SpaceX3s),
                        onClick = { richTextState.cycleAlignment() }
                    ) {
                        AlignmentIcon(
                            alignment = activeAlign,
                            tint = if (isAlignActive) colors.primary else colors.textPrimary
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = Spacing.SpaceX2s),
                    color = colors.textSecondary.copy(alpha = 0.3f)
                )

                // GROUP 4: History (Fixed right-aligned Undo/Redo)
                IconButton(
                    onClick = { richTextState.undo() },
                    enabled = richTextState.canUndo,
                    modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                ) {
                    Text(
                        "↶",
                        color = if (richTextState.canUndo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { richTextState.redo() },
                    enabled = richTextState.canRedo,
                    modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                ) {
                    Text(
                        "↷",
                        color = if (richTextState.canRedo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.titleLarge,
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

        AppAlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = {
                Text(
                    text = if (activeLink != null) "Edit Link" else "Insert Link",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                ) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label = { Text("Link Text", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("Text to display", color = colors.textSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Link URL", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("https://example.com", color = colors.textSecondary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.SpaceXs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeLink != null) {
                        TextButton(onClick = {
                            richTextState.removeLinkAtCursor()
                            showLinkDialog = false
                        }) {
                            Text("Remove Link", color = colors.error, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(onClick = {
                        if (urlText.isNotBlank()) {
                            richTextState.insertLink(displayText = linkText, url = urlText.trim())
                        }
                        showLinkDialog = false
                    }) {
                        Text("Apply", color = colors.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge)
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val backgroundColor = if (isActive) colors.primary.copy(alpha = if (colors.isLight) 0.18f else 0.25f) else Color.Transparent
    val borderStroke = if (isActive) BorderStroke(Border.WidthThin, colors.primary) else null
    val shape = RoundedCornerShape(Radius.RadiusSm)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .clip(shape)
            .background(backgroundColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .clickable(
                onClick = onClick,
                role = Role.Checkbox
            )
            .semantics {
                this.contentDescription = contentDescription
                this.selected = isActive
            }
            .padding(horizontal = Spacing.SpaceX2s, vertical = Spacing.SpaceX2s),
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

@Composable
private fun AlignmentIcon(
    alignment: TextAlignment,
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height

        val y1 = h * 0.22f
        val y2 = h * 0.50f
        val y3 = h * 0.78f

        // Top line (full width)
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, y1),
            end = Offset(w * 0.88f, y1),
            strokeWidth = strokeWidth,
            cap = cap
        )

        // Middle line (traditional staggered alignment)
        val (midStart, midEnd) = when (alignment) {
            TextAlignment.Start -> Pair(w * 0.12f, w * 0.58f)
            TextAlignment.Center -> Pair(w * 0.26f, w * 0.74f)
            TextAlignment.End -> Pair(w * 0.42f, w * 0.88f)
        }
        drawLine(
            color = tint,
            start = Offset(midStart, y2),
            end = Offset(midEnd, y2),
            strokeWidth = strokeWidth,
            cap = cap
        )

        // Bottom line (full width)
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, y3),
            end = Offset(w * 0.88f, y3),
            strokeWidth = strokeWidth,
            cap = cap
        )
    }
}

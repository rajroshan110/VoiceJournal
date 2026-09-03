package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.notedetail.editor.model.TextAlignment

class RichTextState(
    initialDocument: RichTextDocument = RichTextDocument.EMPTY,
    initialSelection: TextRange = TextRange.Zero
) {
    var document by mutableStateOf(initialDocument)
        private set

    var selection by mutableStateOf(initialSelection)
        private set

    var composition by mutableStateOf<TextRange?>(null)
        private set

    var pendingActiveStyles by mutableStateOf<Set<SpanType>>(emptySet())
        private set

    private val undoRedoManager = UndoRedoManager()

    val canUndo: Boolean get() = undoRedoManager.canUndo
    val canRedo: Boolean get() = undoRedoManager.canRedo

    val textFieldValue: TextFieldValue
        get() = TextFieldValue(text = document.text, selection = selection, composition = composition)

    fun isSpanActive(spanType: SpanType): Boolean {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        return if (start == end) {
            pendingActiveStyles.any { FormattingEngine.isMatchingSpanType(it, spanType) }
        } else {
            FormattingEngine.isSpanActive(document, spanType, start, end)
        }
    }

    fun isParagraphActive(paragraphType: ParagraphType): Boolean {
        val (pStart, pEnd) = FormattingEngine.getParagraphBounds(document.text, selection)
        return document.paragraphs.any {
            if (it.type is ParagraphType.Heading && paragraphType is ParagraphType.Heading) {
                it.type.level == paragraphType.level && it.start <= pStart && it.end >= pEnd
            } else if (it.type is ParagraphType.NumberedList && paragraphType is ParagraphType.NumberedList) {
                it.start <= pStart && it.end >= pEnd
            } else {
                it.type == paragraphType && it.start <= pStart && it.end >= pEnd
            }
        }
    }

    fun getAlignmentAtCursor(): TextAlignment? {
        val (pStart, pEnd) = FormattingEngine.getParagraphBounds(document.text, selection)
        return document.alignments.firstOrNull { it.start <= pStart && it.end >= pEnd }?.alignment
    }

    private fun commitMutation(newDocument: RichTextDocument, newSelection: TextRange, clearUndo: Boolean = false) {
        val normalized = newDocument.normalize()
        document = normalized
        val maxLen = normalized.length
        val s = minOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
        val e = maxOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
        selection = if (newSelection.reversed) TextRange(e, s) else TextRange(s, e)
        if (clearUndo) {
            undoRedoManager.clear()
            pendingActiveStyles = emptySet()
        }
        updateActiveStylesForCursor(selection)
    }

    fun getLinkAtCursor(): SpanRange? {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        return document.spans.firstOrNull { it.type is SpanType.Link && (it.intersects(start, end) || (start == end && (it.contains(start) || (start > 0 && it.contains(start - 1))))) }
    }

    fun removeLinkAtCursor() {
        val link = getLinkAtCursor() ?: return
        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val updated = FormattingEngine.removeSpan(document, link.type, link.start, link.end)
        commitMutation(updated, selection)
    }

    fun isLinkActive(): Boolean {
        return getLinkAtCursor() != null
    }

    fun toggleLink(url: String) {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        if (start == end) return

        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val existingLink = document.spans.firstOrNull { it.type is SpanType.Link && it.intersects(start, end) }
        val updated = if (existingLink != null) {
            FormattingEngine.removeSpan(document, existingLink.type, start, end)
        } else {
            FormattingEngine.applySpan(document, SpanType.Link(url), start, end)
        }
        commitMutation(updated, selection)
    }

    fun setDocument(newDocument: RichTextDocument, newSelection: TextRange = TextRange(newDocument.length)) {
        composition = null
        commitMutation(newDocument, newSelection, clearUndo = true)
    }

    fun updateSelection(newSelection: TextRange) {
        val maxLen = document.length
        val s = minOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
        val e = maxOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
        selection = if (newSelection.reversed) TextRange(e, s) else TextRange(s, e)
        updateActiveStylesForCursor(selection)
    }

    fun onTextFieldValueChange(newValue: TextFieldValue) {
        val oldText = document.text
        val newText = newValue.text
        val newSelection = newValue.selection

        if (oldText == newText) {
            // Selection, cursor position, or composition change
            val maxLen = document.length
            val s = minOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
            val e = maxOf(newSelection.start, newSelection.end).coerceIn(0, maxLen)
            selection = if (newSelection.reversed) TextRange(e, s) else TextRange(s, e)
            composition = newValue.composition
            updateActiveStylesForCursor(selection)
            return
        }

        // Save current snapshot for Undo before modifying (coalesces continuous typing/deletion sessions)
        val isNewline = newText.length > oldText.length && newText.endsWith("\n")
        val mutationKind = when {
            isNewline -> MutationKind.STRUCTURAL
            newText.length > oldText.length -> MutationKind.TYPING
            else -> MutationKind.DELETION
        }
        undoRedoManager.pushState(document, selection, mutationKind)

        // Text content changed (typing or deletion)
        val deltaLength = newText.length - oldText.length
        var updatedDoc: RichTextDocument
        var updatedSel = newSelection

        if (deltaLength > 0) {
            // Insertion
            val changePos = newValue.selection.start - deltaLength
            if (changePos >= 0 && changePos <= oldText.length) {
                updatedDoc = FormattingEngine.adjustSpansOnTextChange(
                    oldDocument = document,
                    newText = newText,
                    changePos = changePos,
                    charsDeleted = 0,
                    charsInserted = deltaLength,
                    activeStyles = pendingActiveStyles
                )
            } else {
                updatedDoc = document.copy(text = newText)
            }
        } else {
            // Deletion
            val deleteCount = -deltaLength
            val changePos = newValue.selection.start
            if (changePos >= 0 && changePos + deleteCount <= oldText.length) {
                updatedDoc = FormattingEngine.adjustSpansOnTextChange(
                    oldDocument = document,
                    newText = newText,
                    changePos = changePos,
                    charsDeleted = deleteCount,
                    charsInserted = 0,
                    activeStyles = emptySet()
                )
            } else {
                updatedDoc = document.copy(text = newText)
            }
        }

        composition = newValue.composition
        commitMutation(updatedDoc, updatedSel)
    }

    fun insertLink(displayText: String, url: String) {
        val activeLink = getLinkAtCursor()
        val isEditingExistingLink = activeLink != null && (selection.collapsed || (selection.start >= activeLink.start && selection.end <= activeLink.end))
        val start = if (isEditingExistingLink) activeLink!!.start else minOf(selection.start, selection.end)
        val end = if (isEditingExistingLink) activeLink!!.end else maxOf(selection.start, selection.end)

        val effectiveText = if (displayText.isNotBlank()) displayText else url
        val normalizedUrl = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            url
        } else {
            "https://$url"
        }

        undoRedoManager.pushState(document, selection, forceSnapshot = true)

        val oldText = document.text
        val newText = oldText.substring(0, start) + effectiveText + oldText.substring(end)
        val linkEnd = start + effectiveText.length

        // Adjust all existing spans around the replacement
        val delta = effectiveText.length - (end - start)
        val updatedSpans = mutableListOf<SpanRange>()
        for (span in document.spans) {
            if (span == activeLink) {
                continue // Replaced by new link span below
            }
            if (span.end <= start) {
                updatedSpans.add(span)
            } else if (span.start >= end) {
                updatedSpans.add(span.copy(start = span.start + delta, end = span.end + delta))
            } else {
                val newS = span.start.coerceAtMost(start)
                val newE = (span.end + delta).coerceIn(0, newText.length)
                if (newS < newE) {
                    updatedSpans.add(span.copy(start = newS, end = newE))
                }
            }
        }
        updatedSpans.add(SpanRange(SpanType.Link(normalizedUrl), start, linkEnd))

        // Adjust paragraphs
        val updatedParagraphs = mutableListOf<ParagraphRange>()
        for (para in document.paragraphs) {
            if (para.end <= start) {
                updatedParagraphs.add(para)
            } else if (para.start >= end) {
                updatedParagraphs.add(para.copy(start = para.start + delta, end = para.end + delta))
            } else {
                updatedParagraphs.add(para.copy(end = (para.end + delta).coerceIn(0, newText.length)))
            }
        }

        val newDoc = RichTextDocument(text = newText, spans = updatedSpans, paragraphs = updatedParagraphs)
        commitMutation(newDoc, TextRange(linkEnd))
    }

    fun toggleSpan(spanType: SpanType) {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        if (start == end) {
            // Toggle pending style for cursor
            val existing = pendingActiveStyles.firstOrNull { FormattingEngine.isMatchingSpanType(it, spanType) }
            pendingActiveStyles = if (existing != null) {
                pendingActiveStyles - existing
            } else {
                pendingActiveStyles + spanType
            }
        } else {
            undoRedoManager.pushState(document, selection, forceSnapshot = true)
            val updated = FormattingEngine.toggleSpan(document, spanType, selection)
            commitMutation(updated, selection)
        }
    }

    fun toggleParagraph(paragraphType: ParagraphType) {
        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val updated = FormattingEngine.toggleParagraph(document, paragraphType, selection)
        commitMutation(updated, selection)
    }

    fun setAlignment(alignment: TextAlignment?) {
        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val updated = FormattingEngine.setAlignment(document, alignment, selection)
        commitMutation(updated, selection)
    }

    fun cycleAlignment() {
        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val updated = FormattingEngine.cycleAlignment(document, selection)
        commitMutation(updated, selection)
    }

    fun undo() {
        val snapshot = undoRedoManager.undo(document, selection) ?: return
        composition = null
        pendingActiveStyles = emptySet()
        commitMutation(snapshot.document, snapshot.selection)
    }

    fun redo() {
        val snapshot = undoRedoManager.redo(document, selection) ?: return
        composition = null
        pendingActiveStyles = emptySet()
        commitMutation(snapshot.document, snapshot.selection)
    }

    private fun updateActiveStylesForCursor(sel: TextRange) {
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        if (start == end) {
            val activeAtCursor = mutableSetOf<SpanType>()
            if (start > 0) {
                for (span in document.spans) {
                    if (span.contains(start - 1)) {
                        activeAtCursor.add(span.type)
                    }
                }
            } else if (start == 0 && document.spans.isNotEmpty()) {
                for (span in document.spans) {
                    if (span.start == 0 && span.end > 0) {
                        activeAtCursor.add(span.type)
                    }
                }
            }
            pendingActiveStyles = activeAtCursor
        } else if (start != end) {
            pendingActiveStyles = emptySet()
        }
    }
}

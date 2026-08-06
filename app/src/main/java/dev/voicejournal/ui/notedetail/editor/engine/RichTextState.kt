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
            if (pendingActiveStyles.contains(spanType)) {
                true
            } else {
                FormattingEngine.isSpanActive(document, spanType, start, end)
            }
        } else {
            FormattingEngine.isSpanActive(document, spanType, start, end)
        }
    }

    fun isParagraphActive(paragraphType: ParagraphType): Boolean {
        val (pStart, pEnd) = FormattingEngine.getParagraphBounds(document.text, selection)
        return document.paragraphs.any {
            if (it.type is ParagraphType.Heading && paragraphType is ParagraphType.Heading) {
                (it.type as ParagraphType.Heading).level == (paragraphType as ParagraphType.Heading).level && it.start <= pStart && it.end >= pEnd
            } else {
                it.type == paragraphType && it.start <= pStart && it.end >= pEnd
            }
        }
    }

    fun isLinkActive(): Boolean {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        return document.spans.any { it.type is SpanType.Link && it.intersects(start, end) }
    }

    fun toggleLink(url: String) {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        if (start == end) return

        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        val existingLink = document.spans.firstOrNull { it.type is SpanType.Link && it.intersects(start, end) }
        if (existingLink != null) {
            document = FormattingEngine.removeSpan(document, existingLink.type, start, end)
        } else {
            document = FormattingEngine.applySpan(document, SpanType.Link(url), start, end)
        }
    }

    fun setDocument(newDocument: RichTextDocument, newSelection: TextRange = TextRange(newDocument.length)) {
        document = newDocument
        selection = newSelection
        composition = null
        pendingActiveStyles = emptySet()
        undoRedoManager.clear()
    }

    fun onTextFieldValueChange(newValue: TextFieldValue) {
        val oldText = document.text
        val newText = newValue.text
        val newSelection = newValue.selection

        if (oldText == newText) {
            // Selection, cursor position, or composition change
            selection = newSelection
            composition = newValue.composition
            updateActiveStylesForCursor(newSelection)
            return
        }

        // Save current snapshot for Undo before modifying (force snapshot on space, newline, or deletion)
        val isBoundary = newText.endsWith(" ") || newText.endsWith("\n") || newText.length < oldText.length
        undoRedoManager.pushState(document, selection, forceSnapshot = isBoundary)

        // Text content changed (typing or deletion)
        val deltaLength = newText.length - oldText.length

        if (deltaLength > 0) {
            // Insertion
            val changePos = newValue.selection.start - deltaLength
            if (changePos >= 0 && changePos <= oldText.length) {
                document = FormattingEngine.adjustSpansOnTextChange(
                    oldDocument = document,
                    newText = newText,
                    changePos = changePos,
                    charsDeleted = 0,
                    charsInserted = deltaLength,
                    activeStyles = pendingActiveStyles
                )
            } else {
                document = document.copy(text = newText)
            }
        } else {
            // Deletion
            val deleteCount = -deltaLength
            val changePos = newValue.selection.start
            if (changePos >= 0 && changePos + deleteCount <= oldText.length) {
                document = FormattingEngine.adjustSpansOnTextChange(
                    oldDocument = document,
                    newText = newText,
                    changePos = changePos,
                    charsDeleted = deleteCount,
                    charsInserted = 0,
                    activeStyles = emptySet()
                )
            } else {
                document = document.copy(text = newText)
            }
        }

        selection = newSelection
        composition = newValue.composition
        updateActiveStylesForCursor(newSelection)
    }

    fun toggleSpan(spanType: SpanType) {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        if (start == end) {
            // Toggle pending style for cursor
            pendingActiveStyles = if (pendingActiveStyles.contains(spanType)) {
                pendingActiveStyles - spanType
            } else {
                pendingActiveStyles + spanType
            }
        } else {
            undoRedoManager.pushState(document, selection, forceSnapshot = true)
            document = FormattingEngine.toggleSpan(document, spanType, selection)
        }
    }

    fun toggleParagraph(paragraphType: ParagraphType) {
        undoRedoManager.pushState(document, selection, forceSnapshot = true)
        document = FormattingEngine.toggleParagraph(document, paragraphType, selection)
    }

    fun undo() {
        val snapshot = undoRedoManager.undo(document, selection) ?: return
        document = snapshot.document
        selection = snapshot.selection
        composition = null
        pendingActiveStyles = emptySet()
    }

    fun redo() {
        val snapshot = undoRedoManager.redo(document, selection) ?: return
        document = snapshot.document
        selection = snapshot.selection
        composition = null
        pendingActiveStyles = emptySet()
    }

    private fun updateActiveStylesForCursor(sel: TextRange) {
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        if (start == end && start > 0) {
            // Inherit active styles from character immediately preceding cursor
            val activeAtCursor = mutableSetOf<SpanType>()
            for (span in document.spans) {
                if (span.contains(start - 1)) {
                    activeAtCursor.add(span.type)
                }
            }
            pendingActiveStyles = activeAtCursor
        } else if (start != end) {
            pendingActiveStyles = emptySet()
        }
    }
}

package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument

data class EditorSnapshot(
    val document: RichTextDocument,
    val selection: TextRange
)

class UndoRedoManager(private val maxHistorySize: Int = 50) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var lastPushTimestamp: Long = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushState(document: RichTextDocument, selection: TextRange, forceSnapshot: Boolean = false) {
        val now = System.currentTimeMillis()
        if (undoStack.isNotEmpty()) {
            val last = undoStack.last()
            // Avoid duplicate consecutive snapshots
            if (last.document == document && last.selection == selection) return

            // Batch typing snapshots if typing within 1 second without a forced boundary (e.g. format change, space, or enter)
            if (!forceSnapshot && (now - lastPushTimestamp < 1000L)) {
                return
            }
        }
        undoStack.addLast(EditorSnapshot(document, selection))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastPushTimestamp = now
    }

    fun undo(currentDocument: RichTextDocument, currentSelection: TextRange): EditorSnapshot? {
        if (!canUndo) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(EditorSnapshot(currentDocument, currentSelection))
        return previous
    }

    fun redo(currentDocument: RichTextDocument, currentSelection: TextRange): EditorSnapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(EditorSnapshot(currentDocument, currentSelection))
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastPushTimestamp = 0L
    }
}

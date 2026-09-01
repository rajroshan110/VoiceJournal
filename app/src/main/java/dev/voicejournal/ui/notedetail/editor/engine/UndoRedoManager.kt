package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument

data class EditorSnapshot(
    val document: RichTextDocument,
    val selection: TextRange
)

enum class MutationKind {
    TYPING,
    DELETION,
    STRUCTURAL
}

class UndoRedoManager(private val maxHistorySize: Int = 50) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var lastPushTimestamp: Long = 0L
    private var currentSessionKind: MutationKind? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushState(
        document: RichTextDocument,
        selection: TextRange,
        kind: MutationKind = MutationKind.STRUCTURAL
    ) {
        val now = System.currentTimeMillis()
        if (undoStack.isNotEmpty()) {
            val last = undoStack.last()
            // Avoid duplicate consecutive snapshots
            if (last.document == document && last.selection == selection) return

            // Batch continuous typing or deletion sessions within 1500ms
            if (kind != MutationKind.STRUCTURAL && kind == currentSessionKind && (now - lastPushTimestamp < 1500L)) {
                lastPushTimestamp = now
                return
            }
        }

        undoStack.addLast(EditorSnapshot(document, selection))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastPushTimestamp = now
        currentSessionKind = kind
    }

    fun pushState(document: RichTextDocument, selection: TextRange, forceSnapshot: Boolean) {
        pushState(document, selection, if (forceSnapshot) MutationKind.STRUCTURAL else MutationKind.TYPING)
    }

    fun undo(currentDocument: RichTextDocument, currentSelection: TextRange): EditorSnapshot? {
        if (!canUndo) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(EditorSnapshot(currentDocument, currentSelection))
        currentSessionKind = null
        lastPushTimestamp = 0L
        return previous
    }

    fun redo(currentDocument: RichTextDocument, currentSelection: TextRange): EditorSnapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(EditorSnapshot(currentDocument, currentSelection))
        currentSessionKind = null
        lastPushTimestamp = 0L
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastPushTimestamp = 0L
        currentSessionKind = null
    }
}

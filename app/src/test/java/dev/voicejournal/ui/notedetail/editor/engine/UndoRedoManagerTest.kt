package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoManagerTest {

    @Test
    fun `undo and redo restores snapshots in correct stack order`() {
        val manager = UndoRedoManager(maxHistorySize = 10)

        val doc1 = RichTextDocument(text = "Hello")
        val doc2 = RichTextDocument(text = "Hello World", spans = listOf(SpanRange(SpanType.Bold, 6, 11)))
        val doc3 = RichTextDocument(text = "Hello World!", spans = listOf(SpanRange(SpanType.Bold, 6, 11)))

        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        // Push state 1
        manager.pushState(doc1, TextRange(5), forceSnapshot = true)
        assertTrue(manager.canUndo)

        // Push state 2
        manager.pushState(doc2, TextRange(11), forceSnapshot = true)

        // Undo from state 3 -> should get state 2
        val undone1 = manager.undo(doc3, TextRange(12))
        assertNotNull(undone1)
        assertEquals("Hello World", undone1!!.document.text)
        assertTrue(manager.canRedo)

        // Undo again -> should get state 1
        val undone2 = manager.undo(undone1.document, undone1.selection)
        assertNotNull(undone2)
        assertEquals("Hello", undone2!!.document.text)

        // Redo -> should get state 2
        val redone1 = manager.redo(undone2.document, undone2.selection)
        assertNotNull(redone1)
        assertEquals("Hello World", redone1!!.document.text)

        // Redo -> should get state 3
        val redone2 = manager.redo(redone1.document, redone1.selection)
        assertNotNull(redone2)
        assertEquals("Hello World!", redone2!!.document.text)

        // No more redo
        assertFalse(manager.canRedo)
    }

    @Test
    fun `clear resets undo and redo stacks`() {
        val manager = UndoRedoManager()
        manager.pushState(RichTextDocument(text = "A"), TextRange(1), forceSnapshot = true)
        assertTrue(manager.canUndo)

        manager.clear()
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)
    }
}

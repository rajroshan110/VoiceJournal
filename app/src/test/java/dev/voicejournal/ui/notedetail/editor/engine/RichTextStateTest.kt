package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextStateTest {

    @Test
    fun `insertLink embeds link into selected text`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(text = "Visit my site for more info"),
            initialSelection = TextRange(9, 13) // "site"
        )

        state.insertLink(displayText = "portfolio site", url = "example.com")

        assertEquals("Visit my portfolio site for more info", state.document.text)
        assertEquals(1, state.document.spans.size)
        val linkSpan = state.document.spans[0]
        assertEquals(9, linkSpan.start)
        assertEquals(23, linkSpan.end)
        assertEquals(SpanType.Link("https://example.com"), linkSpan.type)
    }

    @Test
    fun `typed markdown symbols remain literal text without auto conversion`() {
        val state = RichTextState()

        // User types "**bold text**"
        state.onTextFieldValueChange(TextFieldValue(text = "**bold text**", selection = TextRange(13)))

        assertEquals("**bold text**", state.document.text)
        assertTrue(state.document.spans.isEmpty())
    }

    @Test
    fun `typed hash space remains literal text without converting to Heading`() {
        val state = RichTextState()

        // User types "# Title"
        state.onTextFieldValueChange(TextFieldValue(text = "# Title", selection = TextRange(7)))

        assertEquals("# Title", state.document.text)
        assertTrue(state.document.paragraphs.isEmpty())
    }

    @Test
    fun `typed number dot space remains literal text without converting to NumberedList`() {
        val state = RichTextState()

        // User types "2. Item"
        state.onTextFieldValueChange(TextFieldValue(text = "2. Item", selection = TextRange(7)))

        assertEquals("2. Item", state.document.text)
        assertTrue(state.document.paragraphs.isEmpty())
    }

    @Test
    fun `toggling span off at cursor immediately de-highlights active state`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "Hello world",
                spans = listOf(SpanRange(SpanType.Bold, 0, 5))
            ),
            initialSelection = TextRange(5) // Right at boundary of "Hello"
        )

        // Move cursor to pos 5 -> updates active styles to {Bold}
        state.onTextFieldValueChange(TextFieldValue(text = "Hello world", selection = TextRange(5)))
        assertTrue(state.isSpanActive(SpanType.Bold))

        // User toggles bold off before typing next character
        state.toggleSpan(SpanType.Bold)

        // Must IMMEDIATELY de-highlight bold
        org.junit.Assert.assertFalse(state.isSpanActive(SpanType.Bold))
    }

    @Test
    fun `applying multiple headings makes only the latest heading active`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(text = "My Title"),
            initialSelection = TextRange(0, 8)
        )

        // Apply H1
        state.toggleParagraph(ParagraphType.Heading(1))
        assertTrue(state.isParagraphActive(ParagraphType.Heading(1)))
        org.junit.Assert.assertFalse(state.isParagraphActive(ParagraphType.Heading(2)))

        // Apply H2 on the same line -> replaces H1
        state.toggleParagraph(ParagraphType.Heading(2))
        org.junit.Assert.assertFalse(state.isParagraphActive(ParagraphType.Heading(1)))
        assertTrue(state.isParagraphActive(ParagraphType.Heading(2)))
        assertEquals(1, state.document.paragraphs.size)
        assertEquals(ParagraphType.Heading(2), state.document.paragraphs[0].type)
    }

    @Test
    fun `getLinkAtCursor and removeLinkAtCursor manage links properly`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "Visit my site now",
                spans = listOf(SpanRange(SpanType.Link("https://example.com"), 9, 13))
            ),
            initialSelection = TextRange(11) // inside "site"
        )

        val link = state.getLinkAtCursor()
        org.junit.Assert.assertNotNull(link)
        assertEquals("https://example.com", (link?.type as? SpanType.Link)?.url)

        // Remove link
        state.removeLinkAtCursor()
        org.junit.Assert.assertNull(state.getLinkAtCursor())
        assertTrue(state.document.spans.none { it.type is SpanType.Link })
        assertEquals("Visit my site now", state.document.text)
    }

    @Test
    fun `typed URL remains literal text and is formatted via ribbon insertLink`() {
        val state = RichTextState()

        // User types "Visit https://google.com "
        state.onTextFieldValueChange(TextFieldValue(text = "Visit https://google.com ", selection = TextRange(25)))

        assertEquals("Visit https://google.com ", state.document.text)
        assertTrue(state.document.spans.isEmpty())

        // Formatted via ribbon link dialog
        state.onTextFieldValueChange(TextFieldValue(text = "Visit Google ", selection = TextRange(6, 12)))
        state.insertLink("Google", "https://google.com")
        assertEquals(1, state.document.spans.size)
        assertEquals(SpanType.Link("https://google.com"), state.document.spans[0].type)
    }

    @Test
    fun `typing at trailing boundary of a link continues as unlinked text`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "Visit https://google.com",
                spans = listOf(SpanRange(SpanType.Link("https://google.com"), 6, 24))
            ),
            initialSelection = TextRange(24) // Right at end of link
        )

        // User types " today" at pos 24
        state.onTextFieldValueChange(TextFieldValue(text = "Visit https://google.com today", selection = TextRange(30)))

        assertEquals("Visit https://google.com today", state.document.text)
        assertEquals(1, state.document.spans.size)
        val linkSpan = state.document.spans[0]
        assertEquals(6, linkSpan.start)
        assertEquals(24, linkSpan.end) // Unexpanded! " today" is normal unlinked text!
    }

    @Test
    fun `switching from BulletList to NumberedList converts directly without toggling off`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "My Item",
                paragraphs = listOf(ParagraphRange(ParagraphType.BulletList, 0, 7))
            ),
            initialSelection = TextRange(0, 7)
        )

        assertTrue(state.isParagraphActive(ParagraphType.BulletList))

        // Direct switch to NumberedList
        state.toggleParagraph(ParagraphType.NumberedList())

        org.junit.Assert.assertFalse(state.isParagraphActive(ParagraphType.BulletList))
        assertTrue(state.isParagraphActive(ParagraphType.NumberedList()))
        assertEquals(1, state.document.paragraphs.size)
        assertEquals(ParagraphType.NumberedList(), state.document.paragraphs[0].type)
    }

    @Test
    fun `toggleSpan on mixed selection applies span to entire selection`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "Hello beautiful world",
                spans = listOf(SpanRange(SpanType.Bold, 0, 5)) // Only "Hello" is bold
            ),
            initialSelection = TextRange(0, 21) // Entire line selected
        )

        // Mixed: not all bold -> toggleSpan should apply Bold to the ENTIRE selection [0, 21]
        state.toggleSpan(SpanType.Bold)

        assertTrue(state.isSpanActive(SpanType.Bold))
        assertEquals(1, state.document.spans.size)
        assertEquals(0, state.document.spans[0].start)
        assertEquals(21, state.document.spans[0].end)
    }

    @Test
    fun `undo coalesces continuous typing into a single step`() {
        val state = RichTextState()

        // User types "Hello" character by character or word by word
        state.onTextFieldValueChange(TextFieldValue(text = "H", selection = TextRange(1)))
        state.onTextFieldValueChange(TextFieldValue(text = "He", selection = TextRange(2)))
        state.onTextFieldValueChange(TextFieldValue(text = "Hello", selection = TextRange(5)))

        assertEquals("Hello", state.document.text)
        assertTrue(state.canUndo)

        // Single Undo reverts the whole continuous typing run
        state.undo()
        assertEquals("", state.document.text)
    }

    @Test
    fun `editing existing link replaces original text and updates url without duplicating`() {
        val state = RichTextState(
            initialDocument = RichTextDocument(
                text = "Check this link for details",
                spans = listOf(SpanRange(SpanType.Link("https://old.com"), 6, 15)) // "this link"
            ),
            initialSelection = TextRange(10) // cursor inside "this link"
        )

        // User edits the link via dialog to "my new website" with https://new.com
        state.insertLink(displayText = "my new website", url = "https://new.com")

        assertEquals("Check my new website for details", state.document.text)
        val link = state.getLinkAtCursor()
        org.junit.Assert.assertNotNull(link)
        assertEquals("https://new.com", (link?.type as? SpanType.Link)?.url)
        assertEquals(6, link?.start)
        assertEquals(20, link?.end) // 6 + "my new website".length (14) = 20
    }
}

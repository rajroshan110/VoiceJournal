package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.AlignmentRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.notedetail.editor.model.TextAlignment
import dev.voicejournal.ui.notedetail.editor.serializer.RichTextHtmlSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingEngineTest {

    @Test
    fun `isSpanActive detects active span at cursor position 0`() {
        val doc = RichTextDocument(
            text = "Bold text here",
            spans = listOf(SpanRange(SpanType.Bold, 0, 4))
        )

        // At index 0 (start of document)
        assertTrue(FormattingEngine.isSpanActive(doc, SpanType.Bold, 0, 0))
        // At index 2 (inside "Bold")
        assertTrue(FormattingEngine.isSpanActive(doc, SpanType.Bold, 2, 2))
        // At index 5 (outside "Bold")
        assertFalse(FormattingEngine.isSpanActive(doc, SpanType.Bold, 5, 5))
    }

    @Test
    fun `isSpanActive matches Highlight and Link regardless of instance arguments`() {
        val doc = RichTextDocument(
            text = "Link and Highlight text",
            spans = listOf(
                SpanRange(SpanType.Link("https://custom.url"), 0, 4),
                SpanRange(SpanType.Highlight(0xFF112233L), 9, 18)
            )
        )

        // Link with empty URL query should match custom URL link
        assertTrue(FormattingEngine.isSpanActive(doc, SpanType.Link(""), 0, 4))
        // Highlight with default color should match custom color highlight
        assertTrue(FormattingEngine.isSpanActive(doc, SpanType.Highlight(), 9, 18))
    }

    @Test
    fun `adjustSpansOnTextChange splits Heading on Enter into normal paragraph on next line`() {
        val oldDoc = RichTextDocument(
            text = "My Heading",
            paragraphs = listOf(ParagraphRange(ParagraphType.Heading(1), 0, 10))
        )

        // User presses Enter at the end of "My Heading" (pos 10)
        val newText = "My Heading\n"
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = newText,
            changePos = 10,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals("My Heading\n", adjusted.text)
        assertEquals(1, adjusted.paragraphs.size)
        val p = adjusted.paragraphs[0]
        assertEquals(0, p.start)
        assertEquals(10, p.end)
        assertEquals(ParagraphType.Heading(1), p.type)
    }

    @Test
    fun `adjustSpansOnTextChange splits bullet list item on Enter into new list item`() {
        val oldDoc = RichTextDocument(
            text = "Item 1",
            paragraphs = listOf(ParagraphRange(ParagraphType.BulletList, 0, 6))
        )

        // User presses Enter at pos 6
        val newText = "Item 1\n"
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = newText,
            changePos = 6,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals("Item 1\n", adjusted.text)
        assertEquals(2, adjusted.paragraphs.size)
        // Item 1
        assertEquals(0, adjusted.paragraphs[0].start)
        assertEquals(6, adjusted.paragraphs[0].end)
        assertEquals(ParagraphType.BulletList, adjusted.paragraphs[0].type)
        // New item ready on line 2 (start = 7, end = 7)
        assertEquals(7, adjusted.paragraphs[1].start)
        assertEquals(7, adjusted.paragraphs[1].end)
        assertEquals(ParagraphType.BulletList, adjusted.paragraphs[1].type)
    }

    @Test
    fun `adjustSpansOnTextChange exits bullet list on empty item Enter`() {
        val oldDoc = RichTextDocument(
            text = "Item 1\n",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 6),
                ParagraphRange(ParagraphType.BulletList, 7, 7) // empty bullet item
            )
        )

        // User presses Enter on the empty bullet item at pos 7
        val newText = "Item 1\n\n"
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = newText,
            changePos = 7,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals(1, adjusted.paragraphs.size)
        assertEquals(0, adjusted.paragraphs[0].start)
        assertEquals(6, adjusted.paragraphs[0].end)
        assertEquals(ParagraphType.BulletList, adjusted.paragraphs[0].type)
    }

    @Test
    fun `applySpan merges adjacent and overlapping spans of same type`() {
        val doc = RichTextDocument(
            text = "Hello world today",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 5) // "Hello"
            )
        )

        // Apply bold to " world" (starts at 5, touches end of "Hello")
        val updated = FormattingEngine.applySpan(doc, SpanType.Bold, 5, 11)
        assertEquals(1, updated.spans.size)
        assertEquals(0, updated.spans[0].start)
        assertEquals(11, updated.spans[0].end)
    }

    @Test
    fun `removeSpan correctly splits span in the middle`() {
        val doc = RichTextDocument(
            text = "Supercalifragilistic",
            spans = listOf(SpanRange(SpanType.Bold, 0, 20))
        )

        // Remove bold in the middle [5, 10]
        val updated = FormattingEngine.removeSpan(doc, SpanType.Bold, 5, 10)
        assertEquals(2, updated.spans.size)
        assertEquals(0, updated.spans[0].start)
        assertEquals(5, updated.spans[0].end)
        assertEquals(10, updated.spans[1].start)
        assertEquals(20, updated.spans[1].end)
    }

    @Test
    fun `typing at end of bold span does not expand bold when bold is toggled off`() {
        val oldDoc = RichTextDocument(
            text = "Hello",
            spans = listOf(SpanRange(SpanType.Bold, 0, 5))
        )

        // User typed " world" at pos 5 with activeStyles = emptySet() (Bold toggled off)
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = "Hello world",
            changePos = 5,
            charsDeleted = 0,
            charsInserted = 6,
            activeStyles = emptySet()
        )

        assertEquals(1, adjusted.spans.size)
        assertEquals(0, adjusted.spans[0].start)
        assertEquals(5, adjusted.spans[0].end) // Bold stays strictly on "Hello"
    }

    @Test
    fun `typing at end of bold span expands bold when bold is active`() {
        val oldDoc = RichTextDocument(
            text = "Hello",
            spans = listOf(SpanRange(SpanType.Bold, 0, 5))
        )

        // User typed " world" at pos 5 with activeStyles = {Bold}
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = "Hello world",
            changePos = 5,
            charsDeleted = 0,
            charsInserted = 6,
            activeStyles = setOf(SpanType.Bold)
        )

        assertEquals(1, adjusted.spans.size)
        assertEquals(0, adjusted.spans[0].start)
        assertEquals(11, adjusted.spans[0].end) // Bold expands to entire "Hello world"
    }

    @Test
    fun `toggleParagraph on multiline selection creates separate list item per line`() {
        val doc = RichTextDocument(text = "Apple\nBanana\nCherry")
        // Select from "Apple" to "Cherry" (0..19)
        val result = FormattingEngine.toggleParagraph(doc, ParagraphType.BulletList, TextRange(0, 19))

        assertEquals(3, result.paragraphs.size)
        assertEquals(0, result.paragraphs[0].start)
        assertEquals(5, result.paragraphs[0].end)
        assertEquals(ParagraphType.BulletList, result.paragraphs[0].type)

        assertEquals(6, result.paragraphs[1].start)
        assertEquals(12, result.paragraphs[1].end)
        assertEquals(ParagraphType.BulletList, result.paragraphs[1].type)

        assertEquals(13, result.paragraphs[2].start)
        assertEquals(19, result.paragraphs[2].end)
        assertEquals(ParagraphType.BulletList, result.paragraphs[2].type)
    }

    @Test
    fun `applySpan preserves existing non-overlapping spans of same type`() {
        val doc = RichTextDocument(
            text = "First bold and Second bold",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 5) // "First"
            )
        )

        // Apply bold to "Second" [15, 21]
        val updated = FormattingEngine.applySpan(doc, SpanType.Bold, 15, 21)

        // Both bold spans MUST coexist
        assertEquals(2, updated.spans.size)
        assertEquals(0, updated.spans[0].start)
        assertEquals(5, updated.spans[0].end)
        assertEquals(15, updated.spans[1].start)
        assertEquals(21, updated.spans[1].end)
    }

    @Test
    fun `typing in newly created empty bullet list item expands paragraph bounds`() {
        val oldDoc = RichTextDocument(
            text = "Item 1\n",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 6),
                ParagraphRange(ParagraphType.BulletList, 7, 7) // empty line
            )
        )

        // User types "A" at pos 7
        val adjusted = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = oldDoc,
            newText = "Item 1\nA",
            changePos = 7,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals(2, adjusted.paragraphs.size)
        assertEquals(0, adjusted.paragraphs[0].start)
        assertEquals(6, adjusted.paragraphs[0].end)
        assertEquals(7, adjusted.paragraphs[1].start)
        assertEquals(8, adjusted.paragraphs[1].end) // Expanded to cover "A"
    }

    @Test
    fun `toggleParagraph on sub-range heading selection applies heading to whole paragraph without splitting text`() {
        val doc = RichTextDocument(text = "Before title. Important Title. After title.")
        // Select "Important Title." (index 14..30)
        val result = FormattingEngine.toggleParagraph(doc, ParagraphType.Heading(1), TextRange(14, 30))

        // Text remains unchanged (no artificial newlines injected)
        assertEquals("Before title. Important Title. After title.", result.text)
        assertEquals(1, result.paragraphs.size)
        assertEquals(0, result.paragraphs[0].start)
        assertEquals(43, result.paragraphs[0].end)
        assertEquals(ParagraphType.Heading(1), result.paragraphs[0].type)
    }

    @Test
    fun `adjustSpansOnTextChange handles Enter at start, middle, and end of Heading`() {
        val doc = RichTextDocument(
            text = "My Heading Line",
            paragraphs = listOf(ParagraphRange(ParagraphType.Heading(1), 0, 15))
        )

        // Enter at end (pos 15) -> heading stays, next line normal
        val atEnd = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "My Heading Line\n",
            changePos = 15,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        assertEquals(1, atEnd.paragraphs.size)
        assertEquals(0, atEnd.paragraphs[0].start)
        assertEquals(15, atEnd.paragraphs[0].end)

        // Enter at start (pos 0) -> creates normal line above, heading moves down
        val atStart = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "\nMy Heading Line",
            changePos = 0,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        assertEquals(1, atStart.paragraphs.size)
        assertEquals(1, atStart.paragraphs[0].start)
        assertEquals(16, atStart.paragraphs[0].end)

        // Enter in middle (pos 3) -> splits into two heading lines
        val inMiddle = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "My \nHeading Line",
            changePos = 3,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        assertEquals(2, inMiddle.paragraphs.size)
        assertEquals(0, inMiddle.paragraphs[0].start)
        assertEquals(3, inMiddle.paragraphs[0].end)
        assertEquals(4, inMiddle.paragraphs[1].start)
        assertEquals(16, inMiddle.paragraphs[1].end)
    }

    @Test
    fun `adjustSpansOnTextChange preserves bullet sequence when Enter is pressed in the middle and start of an item`() {
        val doc = RichTextDocument(
            text = "Hel\nlo",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 3),
                ParagraphRange(ParagraphType.BulletList, 4, 6)
            )
        )

        // Enter at start of 2nd bullet (pos 4)
        val atStart = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "Hel\n\nlo",
            changePos = 4,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        
        // Should have 3 bullets: "Hel" (0-3), "" (4-4), "lo" (5-7)
        assertEquals(3, atStart.paragraphs.size)
        assertEquals(0, atStart.paragraphs[0].start)
        assertEquals(3, atStart.paragraphs[0].end)
        assertEquals(ParagraphType.BulletList, atStart.paragraphs[0].type)
        
        assertEquals(4, atStart.paragraphs[1].start)
        assertEquals(4, atStart.paragraphs[1].end)
        assertEquals(ParagraphType.BulletList, atStart.paragraphs[1].type)
        
        assertEquals(5, atStart.paragraphs[2].start)
        assertEquals(7, atStart.paragraphs[2].end)
        assertEquals(ParagraphType.BulletList, atStart.paragraphs[2].type)
    }

    @Test
    fun `adjustSpansOnTextChange exits bullet list on empty item`() {
        val doc = RichTextDocument(
            text = "Item 1\n",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 6),
                ParagraphRange(ParagraphType.BulletList, 7, 7)
            )
        )

        // Enter on empty bullet line (pos 7) -> exits list
        val result = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "Item 1\n\n",
            changePos = 7,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        assertEquals(1, result.paragraphs.size)
        assertEquals(0, result.paragraphs[0].start)
        assertEquals(6, result.paragraphs[0].end)
    }

    @Test
    fun `setAlignment applies Center alignment to single line`() {
        val doc = RichTextDocument(text = "Hello World")
        val result = FormattingEngine.setAlignment(doc, TextAlignment.Center, TextRange(3, 3))

        assertEquals(1, result.alignments.size)
        assertEquals(TextAlignment.Center, result.alignments[0].alignment)
        assertEquals(0, result.alignments[0].start)
        assertEquals(11, result.alignments[0].end)
    }

    @Test
    fun `setAlignment with null resets alignment`() {
        val doc = RichTextDocument(
            text = "Centered Line",
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 13))
        )
        val result = FormattingEngine.setAlignment(doc, null, TextRange(5, 5))

        assertTrue(result.alignments.isEmpty())
    }

    @Test
    fun `cycleAlignment cycles Left to Center to Right to Reset`() {
        var doc = RichTextDocument(text = "Line")
        // 1. Initial unaligned -> cycle -> Start (Left)
        doc = FormattingEngine.cycleAlignment(doc, TextRange(0, 0))
        assertEquals(1, doc.alignments.size)
        assertEquals(TextAlignment.Start, doc.alignments[0].alignment)

        // 2. Start -> cycle -> Center
        doc = FormattingEngine.cycleAlignment(doc, TextRange(0, 0))
        assertEquals(1, doc.alignments.size)
        assertEquals(TextAlignment.Center, doc.alignments[0].alignment)

        // 3. Center -> cycle -> End (Right)
        doc = FormattingEngine.cycleAlignment(doc, TextRange(0, 0))
        assertEquals(1, doc.alignments.size)
        assertEquals(TextAlignment.End, doc.alignments[0].alignment)

        // 4. End -> cycle -> Reset (unaligned)
        doc = FormattingEngine.cycleAlignment(doc, TextRange(0, 0))
        assertTrue(doc.alignments.isEmpty())
    }

    @Test
    fun `setAlignment preserves existing Headings and Spans orthogonally`() {
        val doc = RichTextDocument(
            text = "Important Heading",
            spans = listOf(SpanRange(SpanType.Bold, 0, 9)),
            paragraphs = listOf(ParagraphRange(ParagraphType.Heading(1), 0, 17))
        )
        val result = FormattingEngine.setAlignment(doc, TextAlignment.Center, TextRange(0, 0))

        assertEquals(1, result.spans.size)
        assertEquals(1, result.paragraphs.size)
        assertEquals(1, result.alignments.size)
        assertEquals(TextAlignment.Center, result.alignments[0].alignment)
        assertEquals(ParagraphType.Heading(1), result.paragraphs[0].type)
    }

    @Test
    fun `adjustSpansOnTextChange propagates alignment to new line on Enter`() {
        val doc = RichTextDocument(
            text = "Centered",
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 8))
        )
        val result = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "Centered\nNew",
            changePos = 8,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals(2, result.alignments.size)
        assertEquals(TextAlignment.Center, result.alignments[0].alignment)
        assertEquals(0, result.alignments[0].start)
        assertEquals(8, result.alignments[0].end)
        assertEquals(TextAlignment.Center, result.alignments[1].alignment)
        assertEquals(9, result.alignments[1].start)
    }

    @Test
    fun `RichTextHtmlSerializer roundtrip preserves text alignment`() {
        val original = RichTextDocument(
            text = "Centered Title\nRight Signed",
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 0, 14),
                AlignmentRange(TextAlignment.End, 15, 27)
            )
        )
        val html = RichTextHtmlSerializer.toHtml(original)
        val roundtrip = RichTextHtmlSerializer.fromHtml(html)

        assertEquals(2, roundtrip.alignments.size)
        assertEquals(TextAlignment.Center, roundtrip.alignments[0].alignment)
        assertEquals(TextAlignment.End, roundtrip.alignments[1].alignment)
    }

    @Test
    fun `typing with active Code style expands span to support multi-word code snippets with spaces`() {
        val initialDoc = RichTextDocument(
            text = "val x",
            spans = listOf(SpanRange(SpanType.Code, 0, 5))
        )

        // Typing space while Code style is active (e.g. typing "val x = 10")
        val withActive = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = initialDoc,
            newText = "val x ",
            changePos = 5,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = setOf(SpanType.Code)
        )

        assertEquals("val x ", withActive.text)
        assertEquals(1, withActive.spans.size)
        // Code span expands so the space and next words stay in code format
        assertEquals(0, withActive.spans[0].start)
        assertEquals(6, withActive.spans[0].end)
        assertEquals(SpanType.Code, withActive.spans[0].type)

        // If toggled off via ribbon (activeStyles is empty), boundary typing does NOT expand
        val withInactive = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = initialDoc,
            newText = "val x ",
            changePos = 5,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        assertEquals("val x ", withInactive.text)
        assertEquals(1, withInactive.spans.size)
        assertEquals(0, withInactive.spans[0].start)
        assertEquals(5, withInactive.spans[0].end)
    }
}

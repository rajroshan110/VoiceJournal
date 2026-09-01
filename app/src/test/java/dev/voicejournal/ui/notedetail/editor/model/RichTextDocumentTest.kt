package dev.voicejournal.ui.notedetail.editor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextDocumentTest {

    @Test
    fun `normalize preserves valid spans and paragraphs`() {
        val doc = RichTextDocument(
            text = "Hello world",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 5),
                SpanRange(SpanType.Italic, 6, 11)
            ),
            paragraphs = listOf(
                ParagraphRange(ParagraphType.Heading(1), 0, 11)
            )
        )

        val normalized = doc.normalize()
        assertEquals(doc.text, normalized.text)
        assertEquals(2, normalized.spans.size)
        assertEquals(1, normalized.paragraphs.size)
    }

    @Test
    fun `normalize removes zero-length and inverted spans`() {
        val doc = RichTextDocument(
            text = "Hello world",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 5),
                SpanRange(SpanType.Italic, 3, 3) // zero-length
            )
        )

        val normalized = doc.normalize()
        assertEquals(1, normalized.spans.size)
        assertEquals(SpanType.Bold, normalized.spans[0].type)
        assertEquals(0, normalized.spans[0].start)
        assertEquals(5, normalized.spans[0].end)
    }

    @Test
    fun `normalize clamps out-of-bounds spans to document bounds`() {
        val doc = RichTextDocument(
            text = "Hello",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 100) // out-of-bounds end
            )
        )

        val normalized = doc.normalize()
        assertEquals(1, normalized.spans.size)
        assertEquals(0, normalized.spans[0].start)
        assertEquals(5, normalized.spans[0].end) // Clamped to 5
    }

    @Test
    fun `normalize merges adjacent and overlapping same-type spans`() {
        val doc = RichTextDocument(
            text = "Hello beautiful world",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 5),   // "Hello"
                SpanRange(SpanType.Bold, 5, 15)   // " beautiful"
            )
        )

        val normalized = doc.normalize()
        assertEquals(1, normalized.spans.size)
        assertEquals(SpanType.Bold, normalized.spans[0].type)
        assertEquals(0, normalized.spans[0].start)
        assertEquals(15, normalized.spans[0].end)
    }

    @Test
    fun `normalize strictly preserves distinct overlapping formatting types`() {
        val doc = RichTextDocument(
            text = "Hello beautiful world",
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 15),    // "Hello beautiful" (Bold)
                SpanRange(SpanType.Italic, 6, 21)   // "beautiful world" (Italic)
            )
        )

        val normalized = doc.normalize()
        assertEquals(2, normalized.spans.size)
        val bold = normalized.spans.first { it.type is SpanType.Bold }
        val italic = normalized.spans.first { it.type is SpanType.Italic }

        assertEquals(0, bold.start)
        assertEquals(15, bold.end)
        assertEquals(6, italic.start)
        assertEquals(21, italic.end)
    }

    @Test
    fun `normalize on empty text clears spans and clamps paragraph to zero`() {
        val doc = RichTextDocument(
            text = "",
            spans = listOf(SpanRange(SpanType.Bold, 0, 5)),
            paragraphs = listOf(ParagraphRange(ParagraphType.Heading(1), 0, 5))
        )

        val normalized = doc.normalize()
        assertTrue(normalized.isEmpty)
        assertTrue(normalized.spans.isEmpty())
        assertEquals(1, normalized.paragraphs.size)
        assertEquals(0, normalized.paragraphs[0].start)
        assertEquals(0, normalized.paragraphs[0].end)
    }

    @Test
    fun `normalize strictly snaps partial or sub-line paragraph ranges to whole lines`() {
        val doc = RichTextDocument(
            text = "Line One\nLine Two\nLine Three",
            paragraphs = listOf(
                // Sub-range inside Line Two (index 12..15 inside "Line Two" which is 9..17)
                ParagraphRange(ParagraphType.BulletList, 12, 15)
            )
        )

        val normalized = doc.normalize()
        assertEquals(1, normalized.paragraphs.size)
        assertEquals(ParagraphType.BulletList, normalized.paragraphs[0].type)
        // Snapped to start of Line Two (9) and end of Line Two (17)
        assertEquals(9, normalized.paragraphs[0].start)
        assertEquals(17, normalized.paragraphs[0].end)
    }
}

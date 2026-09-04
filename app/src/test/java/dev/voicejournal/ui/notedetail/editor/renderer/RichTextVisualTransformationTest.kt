package dev.voicejournal.ui.notedetail.editor.renderer

import androidx.compose.ui.text.AnnotatedString
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import dev.voicejournal.ui.notedetail.editor.model.AlignmentRange
import dev.voicejournal.ui.notedetail.editor.engine.FormattingEngine
import dev.voicejournal.ui.notedetail.editor.model.TextAlignment
import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextVisualTransformationTest {

    @Test
    fun `filter renders visual bullet prefixes for bullet list and maps offsets accurately`() {
        val doc = RichTextDocument(
            text = "Milk\nBread",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 4),
                ParagraphRange(ParagraphType.BulletList, 5, 10)
            )
        )

        val transformation = RichTextVisualTransformation { doc }
        val result = transformation.filter(AnnotatedString(doc.text))

        // Transformed text should contain bullet prefixes
        assertEquals("• Milk\n• Bread", result.text.text)

        val offsetMapping = result.offsetMapping

        // Raw 0 ("M") -> Transformed 2 (after "• ")
        assertEquals(2, offsetMapping.originalToTransformed(0))
        // Raw 4 ('\n') -> Transformed 6
        assertEquals(6, offsetMapping.originalToTransformed(4))
        // Raw 5 ("B") -> Transformed 9 (after second "• ")
        assertEquals(9, offsetMapping.originalToTransformed(5))
        // Raw 10 (end of string) -> Transformed 14
        assertEquals(14, offsetMapping.originalToTransformed(10))

        // Transformed 0 (on bullet) -> Raw 0
        assertEquals(0, offsetMapping.transformedToOriginal(0))
        // Transformed 2 ("M") -> Raw 0
        assertEquals(0, offsetMapping.transformedToOriginal(2))
        // Transformed 9 ("B") -> Raw 5
        assertEquals(5, offsetMapping.transformedToOriginal(9))
    }

    @Test
    fun `filter renders sequential numbers for numbered list`() {
        val doc = RichTextDocument(
            text = "First\nSecond\nThird",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.NumberedList(), 0, 5),
                ParagraphRange(ParagraphType.NumberedList(), 6, 12),
                ParagraphRange(ParagraphType.NumberedList(), 13, 18)
            )
        )

        val transformation = RichTextVisualTransformation { doc }
        val result = transformation.filter(AnnotatedString(doc.text))

        assertEquals("1. First\n2. Second\n3. Third", result.text.text)
    }

    @Test
    fun `filter renders quote visual bar prefix`() {
        val doc = RichTextDocument(
            text = "Wise quote here",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.Quote, 0, 15)
            )
        )

        val transformation = RichTextVisualTransformation { doc }
        val result = transformation.filter(AnnotatedString(doc.text))

        assertEquals("▎ Wise quote here", result.text.text)
    }

    @Test
    fun `terminal offset mapping allows cursor placement after last character in plain text`() {
        val doc = RichTextDocument(text = "Hello")
        val transformation = RichTextVisualTransformation { doc }
        val result = transformation.filter(AnnotatedString(doc.text))

        val offsetMapping = result.offsetMapping
        // Transformed 5 (after 'o') MUST map to Raw 5
        assertEquals(5, offsetMapping.transformedToOriginal(5))
        assertEquals(5, offsetMapping.originalToTransformed(5))
    }

    @Test
    fun `terminal offset mapping allows cursor placement after last character in bullet text`() {
        val doc = RichTextDocument(
            text = "Apple",
            paragraphs = listOf(ParagraphRange(ParagraphType.BulletList, 0, 5))
        )
        val transformation = RichTextVisualTransformation { doc }
        val result = transformation.filter(AnnotatedString(doc.text))

        // Transformed is "• Apple" (length 7)
        assertEquals("• Apple", result.text.text)
        val offsetMapping = result.offsetMapping

        // Transformed 7 (after 'e') MUST map to Raw 5 (after 'e')
        assertEquals(5, offsetMapping.transformedToOriginal(7))
        assertEquals(7, offsetMapping.originalToTransformed(5))
    }

    @Test
    fun `single line aligned text paragraph style covers text without ghost line`() {
        val doc = RichTextDocument(
            text = "Hello",
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 5))
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))
        assertEquals(1, result.text.paragraphStyles.size)
        val style = result.text.paragraphStyles[0]
        assertEquals(0, style.start)
        assertEquals(5, style.end)
        assertEquals(TextAlign.Center, style.item.textAlign)
    }

    @Test
    fun `multiline aligned text paragraph styles correctly bounded without newline inclusion`() {
        val doc = RichTextDocument(
            text = "Hello\nWorld",
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 0, 5),
                AlignmentRange(TextAlignment.End, 6, 11)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))
        assertEquals(2, result.text.paragraphStyles.size)

        // Line 1 covers 0..6 (including newline for seamless contiguous paragraph style without gap)
        val p1 = result.text.paragraphStyles[0]
        assertEquals(0, p1.start)
        assertEquals(6, p1.end)
        assertEquals(TextAlign.Center, p1.item.textAlign)

        // Line 2 covers start=6 to end=11
        val p2 = result.text.paragraphStyles[1]
        assertEquals(6, p2.start)
        assertEquals(11, p2.end)
        assertEquals(TextAlign.End, p2.item.textAlign)
    }

    @Test
    fun `trailing newline text does not produce zero-length trailing paragraph style`() {
        val doc = RichTextDocument(
            text = "Hello\n",
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 5))
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        // Must produce exactly 1 style for Line 1 (0..6), no ghost style at 6..6
        assertEquals(1, result.text.paragraphStyles.size)
        val p1 = result.text.paragraphStyles[0]
        assertEquals(0, p1.start)
        assertEquals(6, p1.end)
        assertEquals(TextAlign.Center, p1.item.textAlign)
    }

    @Test
    fun `bullet list above and below aligned paragraph leaves unaligned lists unstyled and centers only target line`() {
        val doc = RichTextDocument(
            text = "First bullet\nCentered title\nSecond bullet",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 12),
                ParagraphRange(ParagraphType.BulletList, 28, 41)
            ),
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 13, 27)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("• First bullet\nCentered title\n• Second bullet", result.text.text)

        // Only the centered line receives a ParagraphStyle; bullet lists above and below remain unfragmented
        assertEquals(1, result.text.paragraphStyles.size)

        val p1 = result.text.paragraphStyles[0]
        assertEquals(15, p1.start)
        assertEquals(30, p1.end)
        assertEquals(TextAlign.Center, p1.item.textAlign)

        val mapping = result.offsetMapping
        // Cursor at end of Line 1 ("First bullet", raw 12) -> transformed 14 (on line 1, before \n at 14)
        assertEquals(14, mapping.originalToTransformed(12))
        // Cursor at start of Line 2 (raw 13) -> transformed 15 (start of line 2)
        assertEquals(15, mapping.originalToTransformed(13))
        // Cursor at end of Line 2 (raw 27) -> transformed 29 (before \n at 29, inside centered paragraph 15..30)
        assertEquals(29, mapping.originalToTransformed(27))
        // Cursor at start of Line 3 text (raw 28) -> transformed 32 (after bullet prefix)
        assertEquals(32, mapping.originalToTransformed(28))
    }

    @Test
    fun `numbered list and quote around aligned text maintain correct boundaries and styles`() {
        val doc = RichTextDocument(
            text = "Item one\nCentered quote\nFinal quote",
            paragraphs = listOf(
                ParagraphRange(ParagraphType.NumberedList(), 0, 8),
                ParagraphRange(ParagraphType.Quote, 24, 35)
            ),
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 9, 23)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("1. Item one\nCentered quote\n▎ Final quote", result.text.text)
        // Only the centered quote gets a ParagraphStyle; unaligned list and quote are not fragmented
        assertEquals(1, result.text.paragraphStyles.size)

        val p = result.text.paragraphStyles[0]
        assertEquals(12, p.start)
        assertEquals(27, p.end)
        assertEquals(TextAlign.Center, p.item.textAlign)
    }

    @Test
    fun `consecutive newlines with alignment maintains contiguous paragraph styles without double-spacing blanks`() {
        val doc = RichTextDocument(
            text = "Line 1\n\nLine 3",
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 0, 6)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("Line 1\n\nLine 3", result.text.text)
        // Only Line 1 gets ParagraphStyle(Center); blank line and Line 3 remain unfragmented
        assertEquals(1, result.text.paragraphStyles.size)

        // Line 1: 0..7 ("Line 1\n") -> Center
        assertEquals(0, result.text.paragraphStyles[0].start)
        assertEquals(7, result.text.paragraphStyles[0].end)
        assertEquals(TextAlign.Center, result.text.paragraphStyles[0].item.textAlign)
    }

    @Test
    fun `consecutive lines with same alignment are merged into single paragraph style`() {
        val doc = RichTextDocument(
            text = "Title 1\nTitle 2\nBody text",
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 0, 7),
                AlignmentRange(TextAlignment.Center, 8, 15)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("Title 1\nTitle 2\nBody text", result.text.text)
        // Title 1 and Title 2 merge into one contiguous ParagraphStyle (0..16), preserving single spacing between them
        assertEquals(1, result.text.paragraphStyles.size)

        val p = result.text.paragraphStyles[0]
        assertEquals(0, p.start)
        assertEquals(16, p.end)
        assertEquals(TextAlign.Center, p.item.textAlign)
    }

    @Test
    fun `consecutive lines with different non-default alignments produce independent paragraph styles`() {
        val doc = RichTextDocument(
            text = "Center line\nEnd line\nDefault line",
            alignments = listOf(
                AlignmentRange(TextAlignment.Center, 0, 11),
                AlignmentRange(TextAlignment.End, 12, 20)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("Center line\nEnd line\nDefault line", result.text.text)
        assertEquals(2, result.text.paragraphStyles.size)

        val p1 = result.text.paragraphStyles[0]
        assertEquals(0, p1.start)
        assertEquals(12, p1.end)
        assertEquals(TextAlign.Center, p1.item.textAlign)

        val p2 = result.text.paragraphStyles[1]
        assertEquals(12, p2.start)
        assertEquals(21, p2.end)
        assertEquals(TextAlign.End, p2.item.textAlign)
    }

    @Test
    fun `document with explicit Start alignments produces styled paragraph with balanced line break and hyphens`() {
        val doc = RichTextDocument(
            text = "Line 1\nLine 2",
            alignments = listOf(
                AlignmentRange(TextAlignment.Start, 0, 6)
            )
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("Line 1\nLine 2", result.text.text)
        assertEquals(1, result.text.paragraphStyles.size)
        val p = result.text.paragraphStyles[0]
        assertEquals(0, p.start)
        assertEquals(7, p.end)
        assertEquals(TextAlign.Start, p.item.textAlign)
        assertEquals(LineBreak.Heading, p.item.lineBreak)
        assertEquals(Hyphens.Auto, p.item.hyphens)
    }

    @Test
    fun `no alignments in document produces zero paragraph styles`() {
        val doc = RichTextDocument(
            text = "Line 1\nLine 2",
            paragraphs = listOf(ParagraphRange(ParagraphType.BulletList, 0, 6))
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals("• Line 1\nLine 2", result.text.text)
        assertEquals(0, result.text.paragraphStyles.size)
    }

    @Test
    fun `character spans inside aligned line are preserved alongside alignment paragraph style`() {
        val doc = RichTextDocument(
            text = "Centered bold text",
            spans = listOf(SpanRange(SpanType.Bold, 9, 13)),
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 18))
        )
        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))

        assertEquals(1, result.text.paragraphStyles.size)
        assertEquals(TextAlign.Center, result.text.paragraphStyles[0].item.textAlign)

        assertEquals(1, result.text.spanStyles.size)
        val boldSpan = result.text.spanStyles[0]
        assertEquals(9, boldSpan.start)
        assertEquals(13, boldSpan.end)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, boldSpan.item.fontWeight)
    }

    @Test
    fun `pressing multiple enters on aligned paragraph maintains alignment across empty lines`() {
        var doc = RichTextDocument(
            text = "Paragraph",
            alignments = listOf(AlignmentRange(TextAlignment.Center, 0, 9))
        )

        // First Enter after "Paragraph" (at index 9)
        doc = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "Paragraph\n",
            changePos = 9,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )
        // Second Enter on the empty line (at index 10)
        doc = FormattingEngine.adjustSpansOnTextChange(
            oldDocument = doc,
            newText = "Paragraph\n\n",
            changePos = 10,
            charsDeleted = 0,
            charsInserted = 1,
            activeStyles = emptySet()
        )

        val result = RichTextVisualTransformation { doc }.filter(AnnotatedString(doc.text))
        // The entire block from 0..11 should merge into a single Centered paragraph style
        assertEquals(1, result.text.paragraphStyles.size)
        val p = result.text.paragraphStyles[0]
        assertEquals(0, p.start)
        assertEquals(11, p.end)
        assertEquals(TextAlign.Center, p.item.textAlign)
    }
}

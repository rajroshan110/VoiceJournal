package dev.voicejournal.ui.notedetail.editor.renderer

import androidx.compose.ui.text.AnnotatedString
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
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
}

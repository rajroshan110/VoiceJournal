package dev.voicejournal.ui.notedetail.editor.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanType

class RichTextVisualTransformation(
    private val getDocument: () -> RichTextDocument
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val document = getDocument()
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(raw)

        // Apply Character Spans
        for (span in document.spans) {
            val start = span.start.coerceIn(0, raw.length)
            val end = span.end.coerceIn(0, raw.length)
            if (start >= end) continue

            val style = when (val t = span.type) {
                is SpanType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                is SpanType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                is SpanType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
                is SpanType.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                is SpanType.Code -> SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0x28888888)
                )
                is SpanType.Highlight -> SpanStyle(
                    background = Color(t.colorArgb)
                )
                is SpanType.Link -> SpanStyle(
                    color = Color(0xFF2196F3),
                    textDecoration = TextDecoration.Underline
                )
            }
            builder.addStyle(style, start, end)
        }

        // Apply Paragraph Spans
        for (para in document.paragraphs) {
            val start = para.start.coerceIn(0, raw.length)
            val end = para.end.coerceIn(0, raw.length)
            if (start >= end) continue

            val style = when (val t = para.type) {
                is ParagraphType.Heading -> {
                    val size = when (t.level) {
                        1 -> 26.sp
                        2 -> 22.sp
                        3 -> 19.sp
                        4 -> 17.sp
                        5 -> 15.sp
                        else -> 13.sp
                    }
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)
                }
                is ParagraphType.Quote -> SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF757575)
                )
                is ParagraphType.BulletList -> SpanStyle(fontWeight = FontWeight.Medium)
                is ParagraphType.NumberedList -> SpanStyle(fontWeight = FontWeight.Medium)
            }
            builder.addStyle(style, start, end)
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

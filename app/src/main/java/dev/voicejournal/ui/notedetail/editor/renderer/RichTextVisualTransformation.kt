package dev.voicejournal.ui.notedetail.editor.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanType

class RichTextVisualTransformation(
    private val linkColor: Color = Color(0xFF2196F3),
    private val quoteColor: Color = Color(0xFF757575),
    private val codeBackground: Color = Color(0x28888888),
    private val codeTextColor: Color = Color.Unspecified,
    private val getDocument: () -> RichTextDocument
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val document = getDocument()
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // Determine line prefixes for ParagraphTypes
        val prefixesByStart = mutableMapOf<Int, String>()
        var currentNumber = 0
        val sortedParas = document.paragraphs.sortedBy { it.start }

        for (i in sortedParas.indices) {
            val para = sortedParas[i]
            val prev = if (i > 0) sortedParas[i - 1] else null
            when (para.type) {
                is ParagraphType.BulletList -> {
                    prefixesByStart[para.start] = "• "
                }
                is ParagraphType.NumberedList -> {
                    val isAdjacent = prev != null && prev.type is ParagraphType.NumberedList &&
                            (prev.end == para.start || prev.end + 1 >= para.start)
                    currentNumber = if (isAdjacent) currentNumber + 1 else 1
                    prefixesByStart[para.start] = "$currentNumber. "
                }
                is ParagraphType.Quote -> {
                    prefixesByStart[para.start] = "▎ "
                }
                is ParagraphType.Heading -> { /* No prefix */ }
            }
        }

        // Build transformed text and bi-directional offset mapping
        val originalToTransformed = IntArray(raw.length + 1)
        val transformedToOriginal = ArrayList<Int>(raw.length + 64)
        val builder = AnnotatedString.Builder()
        val prefixRanges = mutableListOf<Triple<Int, Int, ParagraphType>>()

        var rawIdx = 0
        while (rawIdx <= raw.length) {
            val prefix = prefixesByStart[rawIdx]
            if (prefix != null) {
                val pStart = builder.length
                for (char in prefix) {
                    transformedToOriginal.add(rawIdx)
                    builder.append(char)
                }
                val pEnd = builder.length
                val pType = sortedParas.first { it.start == rawIdx }.type
                prefixRanges.add(Triple(pStart, pEnd, pType))
            }

            originalToTransformed[rawIdx] = builder.length

            if (rawIdx < raw.length) {
                transformedToOriginal.add(rawIdx)
                builder.append(raw[rawIdx])
            }
            rawIdx++
        }
        // Terminal mapping for end-of-text cursor position
        transformedToOriginal.add(raw.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return originalToTransformed[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (transformedToOriginal.isEmpty()) return 0
                val clamped = offset.coerceIn(0, transformedToOriginal.size - 1)
                return transformedToOriginal[clamped]
            }
        }

        // 1. Style prefixes (quote bar styled with quoteColor, bullets and numbers in regular font)
        for ((pStart, pEnd, type) in prefixRanges) {
            when (type) {
                is ParagraphType.Quote -> {
                    builder.addStyle(SpanStyle(color = quoteColor, fontWeight = FontWeight.Black), pStart, pEnd)
                }
                else -> { /* Normal text appearance for bullet & number prefixes */ }
            }
        }

        // 2. Apply Paragraph Spans FIRST (so character spans like Bold, Italic layer on top)
        for (para in document.paragraphs) {
            val rawStart = para.start.coerceIn(0, raw.length)
            val rawEnd = para.end.coerceIn(0, raw.length)
            if (rawStart >= rawEnd) continue

            val tStart = originalToTransformed[rawStart]
            val tEnd = originalToTransformed[rawEnd]
            if (tStart >= tEnd) continue

            when (val t = para.type) {
                is ParagraphType.Heading -> {
                    val size = when (t.level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        4 -> 16.sp
                        5 -> 15.sp
                        else -> 14.sp
                    }
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), tStart, tEnd)
                }
                is ParagraphType.Quote -> {
                    builder.addStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = quoteColor
                        ),
                        tStart,
                        tEnd
                    )
                }
                is ParagraphType.BulletList, is ParagraphType.NumberedList -> {
                    // Do NOT override font weight on list text so manual Bold, Italic works cleanly
                }
            }
        }

        // 3. Pre-process TextDecorations to combine overlapping Underline and Strikethrough
        val underlineSpans = document.spans.filter { it.type is SpanType.Underline }
        val strikeSpans = document.spans.filter { it.type is SpanType.Strikethrough }

        val decorationPoints = (underlineSpans.flatMap { listOf(it.start.coerceIn(0, raw.length), it.end.coerceIn(0, raw.length)) } +
                strikeSpans.flatMap { listOf(it.start.coerceIn(0, raw.length), it.end.coerceIn(0, raw.length)) })
            .distinct()
            .sorted()

        for (k in 0 until decorationPoints.size - 1) {
            val rawSegStart = decorationPoints[k]
            val rawSegEnd = decorationPoints[k + 1]
            if (rawSegStart >= rawSegEnd) continue

            val tSegStart = originalToTransformed[rawSegStart]
            val tSegEnd = originalToTransformed[rawSegEnd]
            if (tSegStart >= tSegEnd) continue

            val hasU = underlineSpans.any { it.start <= rawSegStart && it.end >= rawSegEnd }
            val hasS = strikeSpans.any { it.start <= rawSegStart && it.end >= rawSegEnd }
            if (hasU && hasS) {
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))), tSegStart, tSegEnd)
            } else if (hasU) {
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), tSegStart, tSegEnd)
            } else if (hasS) {
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), tSegStart, tSegEnd)
            }
        }

        // 4. Apply Character Spans
        for (span in document.spans) {
            val rawStart = span.start.coerceIn(0, raw.length)
            val rawEnd = span.end.coerceIn(0, raw.length)
            if (rawStart >= rawEnd) continue

            val tStart = originalToTransformed[rawStart]
            val tEnd = originalToTransformed[rawEnd]
            if (tStart >= tEnd) continue

            when (val t = span.type) {
                is SpanType.Bold -> builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), tStart, tEnd)
                is SpanType.Italic -> builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), tStart, tEnd)
                is SpanType.Underline, is SpanType.Strikethrough -> { /* Handled above */ }
                is SpanType.Code -> builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        color = codeTextColor
                    ),
                    tStart,
                    tEnd
                )
                is SpanType.Highlight -> builder.addStyle(
                    SpanStyle(
                        background = Color(t.colorArgb),
                        color = Color(0xFF121212)
                    ),
                    tStart,
                    tEnd
                )
                is SpanType.Link -> builder.addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    tStart,
                    tEnd
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}

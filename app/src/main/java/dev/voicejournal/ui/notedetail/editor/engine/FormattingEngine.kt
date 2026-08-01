package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType

object FormattingEngine {

    fun applySpan(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): RichTextDocument {
        if (start >= end) return document

        val otherSpans = document.spans.filter { it.type != spanType }
        val matchingSpans = document.spans.filter { it.type == spanType }

        // Find min start and max end among overlapping/adjacent matching spans
        var newStart = start
        var newEnd = end

        for (span in matchingSpans) {
            // Check overlap or exact touch
            if (span.start <= newEnd && span.end >= newStart) {
                newStart = minOf(newStart, span.start)
                newEnd = maxOf(newEnd, span.end)
            }
        }

        val updatedSpans = (otherSpans + SpanRange(spanType, newStart, newEnd)).sortedBy { it.start }
        return document.copy(spans = updatedSpans)
    }

    fun removeSpan(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): RichTextDocument {
        if (start >= end) return document

        val resultSpans = mutableListOf<SpanRange>()

        for (span in document.spans) {
            if (span.type != spanType) {
                resultSpans.add(span)
                continue
            }

            // Check intersection
            if (span.end <= start || span.start >= end) {
                // No overlap
                resultSpans.add(span)
            } else {
                // Split left side if necessary
                if (span.start < start) {
                    resultSpans.add(SpanRange(spanType, span.start, start))
                }
                // Split right side if necessary
                if (span.end > end) {
                    resultSpans.add(SpanRange(spanType, end, span.end))
                }
            }
        }

        return document.copy(spans = resultSpans.sortedBy { it.start })
    }

    fun isSpanActive(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): Boolean {
        if (start >= end) {
            // Point check (cursor position)
            if (start <= 0) return false
            return document.spans.any { it.type == spanType && it.contains(start - 1) && it.contains(start) }
        }

        // Check range coverage
        val matching = document.spans.filter { it.type == spanType && it.intersects(start, end) }
            .sortedBy { it.start }

        if (matching.isEmpty()) return false

        var coveredUntil = start
        for (span in matching) {
            if (span.start > coveredUntil) return false
            coveredUntil = maxOf(coveredUntil, span.end)
            if (coveredUntil >= end) return true
        }

        return coveredUntil >= end
    }

    fun toggleSpan(document: RichTextDocument, spanType: SpanType, selection: TextRange): RichTextDocument {
        val start = minOf(selection.start, selection.end).coerceIn(0, document.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, document.length)

        if (start == end) return document // Handled by pendingActiveStyles in RichTextState

        return if (isSpanActive(document, spanType, start, end)) {
            removeSpan(document, spanType, start, end)
        } else {
            applySpan(document, spanType, start, end)
        }
    }

    fun getParagraphBounds(text: String, selection: TextRange): Pair<Int, Int> {
        if (text.isEmpty()) return Pair(0, 0)
        val selStart = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val selEnd = maxOf(selection.start, selection.end).coerceIn(0, text.length)

        val pStart = if (selStart == 0) 0 else {
            val idx = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
            if (idx == -1) 0 else idx + 1
        }

        val pEnd = if (selEnd >= text.length) text.length else {
            val idx = text.indexOf('\n', selEnd)
            if (idx == -1) text.length else idx
        }

        return Pair(pStart, pEnd)
    }

    fun toggleParagraph(document: RichTextDocument, paragraphType: ParagraphType, selection: TextRange): RichTextDocument {
        val (pStart, pEnd) = getParagraphBounds(document.text, selection)

        val existingSameType = document.paragraphs.firstOrNull {
            it.type == paragraphType && it.start <= pStart && it.end >= pEnd
        }

        val otherParagraphs = document.paragraphs.filterNot {
            it.start < pEnd && it.end > pStart
        }

        val newParagraphs = if (existingSameType != null) {
            otherParagraphs
        } else {
            otherParagraphs + ParagraphRange(paragraphType, pStart, pEnd)
        }

        return document.copy(paragraphs = newParagraphs.sortedBy { it.start })
    }

    fun adjustSpansOnTextChange(
        oldDocument: RichTextDocument,
        newText: String,
        changePos: Int,
        charsDeleted: Int,
        charsInserted: Int,
        activeStyles: Set<SpanType>
    ): RichTextDocument {
        val adjustedSpans = mutableListOf<SpanRange>()

        for (span in oldDocument.spans) {
            if (charsDeleted > 0) {
                val delStart = changePos
                val delEnd = changePos + charsDeleted

                if (span.end <= delStart) {
                    adjustedSpans.add(span)
                } else if (span.start >= delEnd) {
                    adjustedSpans.add(span.copy(start = span.start - charsDeleted + charsInserted, end = span.end - charsDeleted + charsInserted))
                } else {
                    // Span overlaps deleted region
                    val newStart = if (span.start >= delStart) changePos else span.start
                    val newEnd = if (span.end <= delEnd) changePos else span.end - charsDeleted
                    if (newStart < newEnd) {
                        adjustedSpans.add(span.copy(start = newStart, end = newEnd + charsInserted))
                    }
                }
            } else {
                // Pure insertion
                if (span.end < changePos) {
                    adjustedSpans.add(span)
                } else if (span.start > changePos) {
                    adjustedSpans.add(span.copy(start = span.start + charsInserted, end = span.end + charsInserted))
                } else if (span.start < changePos && span.end >= changePos) {
                    // Insertion inside span -> expand span
                    adjustedSpans.add(span.copy(end = span.end + charsInserted))
                } else if (span.start == changePos) {
                    // Insertion at span start boundary -> expand if style active
                    if (activeStyles.contains(span.type)) {
                        adjustedSpans.add(span.copy(end = span.end + charsInserted))
                    } else {
                        adjustedSpans.add(span.copy(start = span.start + charsInserted, end = span.end + charsInserted))
                    }
                }
            }
        }

        // Apply any pending active styles to newly inserted characters
        var doc = oldDocument.copy(text = newText, spans = adjustedSpans.sortedBy { it.start })
        if (charsInserted > 0 && activeStyles.isNotEmpty()) {
            for (style in activeStyles) {
                doc = applySpan(doc, style, changePos, changePos + charsInserted)
            }
        }

        // Adjust paragraphs
        val adjustedParagraphs = mutableListOf<ParagraphRange>()
        val (pStart, pEnd) = Pair(0, newText.length)
        for (para in oldDocument.paragraphs) {
            val delta = charsInserted - charsDeleted
            if (para.end <= changePos) {
                adjustedParagraphs.add(para)
            } else if (para.start >= changePos) {
                val ns = (para.start + delta).coerceIn(0, newText.length)
                val ne = (para.end + delta).coerceIn(0, newText.length)
                if (ns < ne) adjustedParagraphs.add(para.copy(start = ns, end = ne))
            } else {
                val ne = (para.end + delta).coerceIn(0, newText.length)
                if (para.start < ne) adjustedParagraphs.add(para.copy(end = ne))
            }
        }

        return doc.copy(paragraphs = adjustedParagraphs.sortedBy { it.start })
    }
}

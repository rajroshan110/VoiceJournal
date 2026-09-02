package dev.voicejournal.ui.notedetail.editor.engine

import androidx.compose.ui.text.TextRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType

object FormattingEngine {

    fun isMatchingSpanType(a: SpanType, b: SpanType): Boolean {
        return when {
            a is SpanType.Highlight && b is SpanType.Highlight -> true
            a is SpanType.Link && b is SpanType.Link -> true
            else -> a == b
        }
    }

    fun applySpan(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): RichTextDocument {
        if (start >= end) return document

        var newStart = start
        var newEnd = end
        val nonOverlappingSpans = mutableListOf<SpanRange>()

        for (span in document.spans) {
            if (isMatchingSpanType(span.type, spanType)) {
                // If it overlaps or touches the new span range, merge into newStart/newEnd
                if (span.start <= newEnd && span.end >= newStart) {
                    newStart = minOf(newStart, span.start)
                    newEnd = maxOf(newEnd, span.end)
                } else {
                    // Separate span of the same type elsewhere in the document: PRESERVE IT!
                    nonOverlappingSpans.add(span)
                }
            } else {
                // Different span type: PRESERVE IT!
                nonOverlappingSpans.add(span)
            }
        }

        val updatedSpans = (nonOverlappingSpans + SpanRange(spanType, newStart, newEnd)).sortedBy { it.start }
        return document.copy(spans = updatedSpans)
    }

    fun removeSpan(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): RichTextDocument {
        if (start >= end) return document

        val resultSpans = mutableListOf<SpanRange>()

        for (span in document.spans) {
            if (!isMatchingSpanType(span.type, spanType)) {
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
                    resultSpans.add(SpanRange(span.type, span.start, start))
                }
                // Split right side if necessary
                if (span.end > end) {
                    resultSpans.add(SpanRange(span.type, end, span.end))
                }
            }
        }

        return document.copy(spans = resultSpans.sortedBy { it.start })
    }

    fun isSpanActive(document: RichTextDocument, spanType: SpanType, start: Int, end: Int): Boolean {
        if (start >= end) {
            // Point check (cursor position)
            if (start == 0) {
                return document.spans.any { isMatchingSpanType(it.type, spanType) && it.start == 0 && it.end > 0 }
            }
            return document.spans.any { isMatchingSpanType(it.type, spanType) && (it.contains(start - 1) || (it.start == start && it.end > start)) }
        }

        // Check range coverage
        val matching = document.spans.filter { isMatchingSpanType(it.type, spanType) && it.intersects(start, end) }
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

    fun getLineRanges(text: String, selection: TextRange): List<Pair<Int, Int>> {
        if (text.isEmpty()) return listOf(Pair(0, 0))
        val (pStart, pEnd) = getParagraphBounds(text, selection)
        if (pStart >= pEnd) return listOf(Pair(pStart, pEnd))

        val lines = mutableListOf<Pair<Int, Int>>()
        var lineStart = pStart
        while (lineStart <= pEnd) {
            val nextNewline = text.indexOf('\n', lineStart)
            val lineEnd = if (nextNewline == -1 || nextNewline > pEnd) pEnd else nextNewline
            lines.add(Pair(lineStart, lineEnd))
            if (nextNewline == -1 || nextNewline >= pEnd) break
            lineStart = nextNewline + 1
        }
        return lines
    }

    fun toggleParagraph(document: RichTextDocument, paragraphType: ParagraphType, selection: TextRange): RichTextDocument {
        val lineRanges = getLineRanges(document.text, selection)
        if (lineRanges.isEmpty()) return document

        // Check if ALL lines in the selection already have this exact paragraph type
        val allLinesHaveType = lineRanges.all { (lStart, lEnd) ->
            document.paragraphs.any {
                val matchesType = if (it.type is ParagraphType.Heading && paragraphType is ParagraphType.Heading) {
                    it.type.level == paragraphType.level
                } else if (it.type is ParagraphType.NumberedList && paragraphType is ParagraphType.NumberedList) {
                    true
                } else {
                    it.type == paragraphType
                }
                matchesType && it.start <= lStart && it.end >= lEnd
            }
        }

        val minStart = lineRanges.minOf { it.first }
        val maxEnd = lineRanges.maxOf { it.second }

        val otherParagraphs = document.paragraphs.filterNot {
            it.start <= maxEnd && it.end >= minStart
        }

        val newParagraphs = if (allLinesHaveType) {
            otherParagraphs
        } else {
            otherParagraphs + lineRanges.map { (lStart, lEnd) ->
                ParagraphRange(paragraphType, lStart, lEnd)
            }
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
        val isNewline = charsInserted == 1 && charsDeleted == 0 && changePos < newText.length && newText[changePos] == '\n'

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
                        adjustedSpans.add(span.copy(start = newStart, end = newEnd))
                    }
                }
            } else {
                // Insertion
                if (span.end < changePos) {
                    adjustedSpans.add(span)
                } else if (span.start > changePos) {
                    adjustedSpans.add(span.copy(start = span.start + charsInserted, end = span.end + charsInserted))
                } else if (span.start < changePos && changePos < span.end) {
                    // Insertion strictly inside span -> expand span
                    adjustedSpans.add(span.copy(end = span.end + charsInserted))
                } else if (span.end == changePos) {
                    // Insertion at span end boundary -> expand only if style active and not a pure newline
                    if (!isNewline && activeStyles.any { isMatchingSpanType(it, span.type) }) {
                        adjustedSpans.add(span.copy(end = span.end + charsInserted))
                    } else {
                        adjustedSpans.add(span)
                    }
                } else if (span.start == changePos) {
                    // Insertion at span start boundary -> expand if style active
                    if (activeStyles.any { isMatchingSpanType(it, span.type) }) {
                        adjustedSpans.add(span.copy(end = span.end + charsInserted))
                    } else {
                        adjustedSpans.add(span.copy(start = span.start + charsInserted, end = span.end + charsInserted))
                    }
                }
            }
        }

        // Apply any pending active styles to newly inserted characters (except for pure newline)
        var doc = oldDocument.copy(text = newText, spans = adjustedSpans.sortedBy { it.start })
        if (charsInserted > 0 && activeStyles.isNotEmpty() && !isNewline) {
            for (style in activeStyles) {
                doc = applySpan(doc, style, changePos, changePos + charsInserted)
            }
        }

        // Adjust paragraphs with intelligent Enter, continuation, exit, and line-join handling
        val adjustedParagraphs = mutableListOf<ParagraphRange>()
        val delta = charsInserted - charsDeleted

        for (para in oldDocument.paragraphs) {
            if (isNewline && para.start <= changePos && changePos <= para.end) {
                // User pressed Enter inside or at the boundary of a paragraph
                val lineText = oldDocument.text.substring(para.start.coerceIn(0, oldDocument.length), para.end.coerceIn(0, oldDocument.length))
                val isLineBlank = lineText.isBlank()

                when (para.type) {
                    is ParagraphType.Heading -> {
                        if (changePos == para.start) {
                            // Enter at start of heading: creates blank normal line above, heading moves down
                            adjustedParagraphs.add(ParagraphRange(para.type, changePos + 1, para.end + 1))
                        } else if (changePos == para.end) {
                            // Enter at end of heading: heading stays, subsequent line is normal paragraph
                            adjustedParagraphs.add(ParagraphRange(para.type, para.start, changePos))
                        } else {
                            // Enter in middle of heading: split heading into two heading lines
                            adjustedParagraphs.add(ParagraphRange(para.type, para.start, changePos))
                            adjustedParagraphs.add(ParagraphRange(para.type, changePos + 1, para.end + 1))
                        }
                    }
                    is ParagraphType.BulletList -> {
                        if (isLineBlank || (para.start == changePos && para.end == changePos)) {
                            // Exit list on empty bullet item -> line becomes normal paragraph
                        } else {
                            // Continue bullet list
                            if (para.start <= changePos) {
                                adjustedParagraphs.add(ParagraphRange(ParagraphType.BulletList, para.start, changePos))
                            }
                            adjustedParagraphs.add(ParagraphRange(ParagraphType.BulletList, changePos + 1, para.end + delta))
                        }
                    }
                    is ParagraphType.NumberedList -> {
                        if (isLineBlank || (para.start == changePos && para.end == changePos)) {
                            // Exit list on empty numbered item -> line becomes normal paragraph
                        } else {
                            // Continue numbered list
                            if (para.start <= changePos) {
                                adjustedParagraphs.add(ParagraphRange(ParagraphType.NumberedList(), para.start, changePos))
                            }
                            adjustedParagraphs.add(ParagraphRange(ParagraphType.NumberedList(), changePos + 1, para.end + delta))
                        }
                    }
                    is ParagraphType.Quote -> {
                        if (isLineBlank || (para.start == changePos && para.end == changePos)) {
                            // Exit quote on empty line -> line becomes normal paragraph
                        } else {
                            // Continue quote
                            if (para.start <= changePos) {
                                adjustedParagraphs.add(ParagraphRange(ParagraphType.Quote, para.start, changePos))
                            }
                            adjustedParagraphs.add(ParagraphRange(ParagraphType.Quote, changePos + 1, para.end + delta))
                        }
                    }
                }
            } else if (isNewline) {
                // Paragraphs outside the newline change point
                if (para.end < changePos) {
                    adjustedParagraphs.add(para)
                } else if (para.start >= changePos) {
                    adjustedParagraphs.add(para.copy(start = para.start + 1, end = para.end + 1))
                }
            } else {
                // Normal typing insertion or deletion
                if (charsDeleted > 0) {
                    val delStart = changePos
                    val delEnd = changePos + charsDeleted
                    if (para.end <= delStart) {
                        adjustedParagraphs.add(para)
                    } else if (para.start >= delEnd) {
                        val ns = (para.start + delta).coerceIn(0, newText.length)
                        val ne = (para.end + delta).coerceIn(0, newText.length)
                        if (ns <= ne) adjustedParagraphs.add(para.copy(start = ns, end = ne))
                    } else {
                        // Deletion overlaps paragraph
                        val ns = minOf(para.start, delStart).coerceIn(0, newText.length)
                        val ne = maxOf(delStart, para.end + delta).coerceIn(0, newText.length)
                        if (ns <= ne) adjustedParagraphs.add(para.copy(start = ns, end = ne))
                    }
                } else {
                    // Character insertion
                    if (para.end < changePos) {
                        adjustedParagraphs.add(para)
                    } else if (para.start > changePos) {
                        val ns = (para.start + delta).coerceIn(0, newText.length)
                        val ne = (para.end + delta).coerceIn(0, newText.length)
                        if (ns <= ne) adjustedParagraphs.add(para.copy(start = ns, end = ne))
                    } else {
                        // Insertion inside or at boundary of paragraph
                        val ne = (para.end + delta).coerceIn(0, newText.length)
                        adjustedParagraphs.add(para.copy(start = para.start, end = ne))
                    }
                }
            }
        }

        return doc.copy(paragraphs = adjustedParagraphs.sortedBy { it.start })
    }
}

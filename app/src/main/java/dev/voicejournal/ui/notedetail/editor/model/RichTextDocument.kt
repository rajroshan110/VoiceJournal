package dev.voicejournal.ui.notedetail.editor.model

data class RichTextDocument(
    val text: String = "",
    val spans: List<SpanRange> = emptyList(),
    val paragraphs: List<ParagraphRange> = emptyList()
) {
    val length: Int get() = text.length
    val isEmpty: Boolean get() = text.isEmpty()

    fun normalize(): RichTextDocument {
        val maxLen = text.length

        // 1. Sanitize & clamp spans: 0 <= start < end <= maxLen
        val validSpans = if (maxLen == 0) emptyList() else spans.mapNotNull { span ->
            val s = span.start.coerceIn(0, maxLen)
            val e = span.end.coerceIn(0, maxLen)
            if (s < e) span.copy(start = s, end = e) else null
        }

        // Merge adjacent or overlapping compatible spans of the exact same formatting type
        val mergedSpans = mutableListOf<SpanRange>()
        val spansByType = validSpans.groupBy { span ->
            when (val t = span.type) {
                is SpanType.Bold -> "Bold"
                is SpanType.Italic -> "Italic"
                is SpanType.Underline -> "Underline"
                is SpanType.Strikethrough -> "Strikethrough"
                is SpanType.Code -> "Code"
                is SpanType.Highlight -> "Highlight_${t.colorArgb}"
                is SpanType.Link -> "Link_${t.url}"
            }
        }

        for ((_, list) in spansByType) {
            val sorted = list.sortedBy { it.start }
            var current: SpanRange? = null
            for (span in sorted) {
                if (current == null) {
                    current = span
                } else if (span.start <= current.end) {
                    // Overlapping or adjacent same-type span: merge boundaries
                    current = current.copy(end = maxOf(current.end, span.end))
                } else {
                    mergedSpans.add(current)
                    current = span
                }
            }
            if (current != null) {
                mergedSpans.add(current)
            }
        }

        // 2. Sanitize paragraphs: strictly line-aligned, 1-to-1 per line, non-overlapping
        val validParas = mutableListOf<ParagraphRange>()
        if (maxLen == 0) {
            val firstPara = paragraphs.firstOrNull()
            if (firstPara != null) {
                validParas.add(firstPara.copy(start = 0, end = 0))
            }
        } else {
            // Compute all line bounds in text
            val lineRanges = mutableListOf<Pair<Int, Int>>()
            var cur = 0
            while (cur <= maxLen) {
                val nextNl = text.indexOf('\n', cur)
                val lEnd = if (nextNl == -1) maxLen else nextNl
                lineRanges.add(Pair(cur, lEnd))
                if (nextNl == -1) break
                cur = nextNl + 1
            }

            // Map each line to its assigned ParagraphType (latest assigned takes precedence)
            val lineTypeMap = mutableMapOf<Pair<Int, Int>, ParagraphType>()
            for (para in paragraphs) {
                val s = para.start.coerceIn(0, maxLen)
                val e = para.end.coerceIn(0, maxLen)
                for (line in lineRanges) {
                    val (lStart, lEnd) = line
                    // A paragraph covers this line if it intersects the line
                    val intersects = if (s == e) {
                        s in lStart..lEnd
                    } else {
                        s < lEnd && e > lStart || (s == lStart && e == lEnd) || (s <= lStart && e >= lEnd)
                    }
                    if (intersects) {
                        lineTypeMap[line] = para.type
                    }
                }
            }

            for (line in lineRanges) {
                val type = lineTypeMap[line]
                if (type != null) {
                    validParas.add(ParagraphRange(type, line.first, line.second))
                }
            }
        }

        return copy(
            spans = mergedSpans.sortedBy { it.start },
            paragraphs = validParas.sortedBy { it.start }
        )
    }

    companion object {
        val EMPTY = RichTextDocument()
    }
}

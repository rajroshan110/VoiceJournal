package dev.voicejournal.ui.notedetail.editor.serializer

import dev.voicejournal.ui.notedetail.editor.model.AlignmentRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import dev.voicejournal.ui.notedetail.editor.model.TextAlignment

object RichTextHtmlSerializer {

    fun escapeHtml(text: String): String {
        if (!text.contains('&') && !text.contains('<') && !text.contains('>') && !text.contains('"')) {
            return text
        }
        return buildString(text.length + 16) {
            for (ch in text) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    else -> append(ch)
                }
            }
        }
    }

    fun toPlainText(document: RichTextDocument): String {
        return document.text
    }

    fun toHtml(document: RichTextDocument): String {
        if (document.text.isEmpty()) return ""
        if (document.spans.isEmpty() && document.paragraphs.isEmpty() && document.alignments.isEmpty()) {
            return escapeHtml(document.text)
        }

        val raw = document.text
        val len = raw.length

        // Event points for tag opening and closing
        class TagEvent(val pos: Int, val isClose: Boolean, val tag: String, val priority: Int)

        val events = mutableListOf<TagEvent>()

        for (span in document.spans) {
            val start = span.start.coerceIn(0, len)
            val end = span.end.coerceIn(0, len)
            if (start >= end) continue

            val tag = when (val t = span.type) {
                is SpanType.Bold -> "b"
                is SpanType.Italic -> "i"
                is SpanType.Underline -> "u"
                is SpanType.Strikethrough -> "s"
                is SpanType.Code -> "code"
                is SpanType.Highlight -> "mark"
                is SpanType.Link -> "a href=\"${escapeHtml(t.url)}\""
            }
            val closeTag = if (span.type is SpanType.Link) "a" else tag
            events.add(TagEvent(start, false, "<$tag>", 1))
            events.add(TagEvent(end, true, "</$closeTag>", 0))
        }

        // Group consecutive list items into proper <ul>/<ol> wrappers (checking type and text adjacency)
        val sortedParas = document.paragraphs.sortedBy { it.start }
        for (i in sortedParas.indices) {
            val para = sortedParas[i]
            val start = para.start.coerceIn(0, len)
            val end = para.end.coerceIn(0, len)
            if (start >= end) continue

            val prev = if (i > 0) sortedParas[i - 1] else null
            val next = if (i + 1 < sortedParas.size) sortedParas[i + 1] else null

            when (val t = para.type) {
                is ParagraphType.Heading -> {
                    events.add(TagEvent(start, false, "<h${t.level}>", 2))
                    events.add(TagEvent(end, true, "</h${t.level}>", -1))
                }
                is ParagraphType.Quote -> {
                    events.add(TagEvent(start, false, "<blockquote>", 2))
                    events.add(TagEvent(end, true, "</blockquote>", -1))
                }
                is ParagraphType.BulletList -> {
                    val isAdjacentToPrev = prev != null && prev.type is ParagraphType.BulletList && (prev.end == start || prev.end + 1 >= start)
                    val isAdjacentToNext = next != null && next.type is ParagraphType.BulletList && (end == next.start || end + 1 >= next.start)
                    val isFirstInGroup = !isAdjacentToPrev
                    val isLastInGroup = !isAdjacentToNext
                    val openTag = if (isFirstInGroup) "<ul><li>" else "<li>"
                    val closeTag = if (isLastInGroup) "</li></ul>" else "</li>"
                    events.add(TagEvent(start, false, openTag, 2))
                    events.add(TagEvent(end, true, closeTag, -1))
                }
                is ParagraphType.NumberedList -> {
                    val isAdjacentToPrev = prev != null && prev.type is ParagraphType.NumberedList && (prev.end == start || prev.end + 1 >= start)
                    val isAdjacentToNext = next != null && next.type is ParagraphType.NumberedList && (end == next.start || end + 1 >= next.start)
                    val isFirstInGroup = !isAdjacentToPrev
                    val isLastInGroup = !isAdjacentToNext
                    val openTag = if (isFirstInGroup) "<ol><li>" else "<li>"
                    val closeTag = if (isLastInGroup) "</li></ol>" else "</li>"
                    events.add(TagEvent(start, false, openTag, 2))
                    events.add(TagEvent(end, true, closeTag, -1))
                }
            }
        }

        for (align in document.alignments) {
            val start = align.start.coerceIn(0, len)
            val end = align.end.coerceIn(0, len)
            if (start >= end) continue

            when (align.alignment) {
                TextAlignment.Start -> {
                    events.add(TagEvent(start, false, "<p style=\"text-align:left\">", 3))
                    events.add(TagEvent(end, true, "</p>", -2))
                }
                TextAlignment.Center -> {
                    events.add(TagEvent(start, false, "<p style=\"text-align:center\">", 3))
                    events.add(TagEvent(end, true, "</p>", -2))
                }
                TextAlignment.End -> {
                    events.add(TagEvent(start, false, "<p style=\"text-align:right\">", 3))
                    events.add(TagEvent(end, true, "</p>", -2))
                }
            }
        }

        events.sortWith(Comparator { a, b ->
            if (a.pos != b.pos) {
                a.pos.compareTo(b.pos)
            } else if (a.isClose != b.isClose) {
                if (a.isClose) -1 else 1
            } else {
                b.priority.compareTo(a.priority)
            }
        })

        val sb = StringBuilder()
        var lastPos = 0

        for (event in events) {
            if (event.pos > lastPos) {
                sb.append(escapeHtml(raw.substring(lastPos, event.pos)))
                lastPos = event.pos
            }
            sb.append(event.tag)
        }

        if (lastPos < len) {
            sb.append(escapeHtml(raw.substring(lastPos)))
        }

        return sb.toString()
    }

    private val HTML_TAG_REGEX = Regex("""<(?:b|strong|i|em|u|s|strike|del|code|pre|mark|a|h[1-6]|blockquote|ul|ol|li|p|div|center)\b""", RegexOption.IGNORE_CASE)

    fun fromHtml(html: String?): RichTextDocument {
        if (html.isNullOrEmpty()) return RichTextDocument.EMPTY

        if (HTML_TAG_REGEX.containsMatchIn(html)) {
            return parseHtmlTags(html)
        }

        if (html.contains("&")) {
            return RichTextDocument(text = decodeHtmlEntities(html)).normalize()
        }

        return RichTextDocument(text = html).normalize()
    }

    private data class OpenTagInfo(
        val name: String,
        val startPos: Int,
        val attributes: Map<String, String> = emptyMap()
    )

    private fun parseAttributes(tagContent: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val attrRegex = Regex("""([a-zA-Z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")
        for (m in attrRegex.findAll(tagContent)) {
            val name = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            attrs[name] = value
        }
        return attrs
    }

    fun decodeHtmlEntities(input: String): String {
        if (!input.contains('&')) return input
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            if (input[i] == '&') {
                val semiIdx = input.indexOf(';', i)
                if (semiIdx != -1 && semiIdx - i in 2..8) {
                    val entity = input.substring(i, semiIdx + 1)
                    val decodedChar = when (entity.lowercase()) {
                        "&amp;" -> '&'
                        "&lt;" -> '<'
                        "&gt;" -> '>'
                        "&quot;" -> '"'
                        "&#39;", "&apos;" -> '\''
                        "&nbsp;" -> ' '
                        else -> null
                    }
                    if (decodedChar != null) {
                        sb.append(decodedChar)
                        i = semiIdx + 1
                        continue
                    }
                }
            }
            sb.append(input[i])
            i++
        }
        return sb.toString()
    }

    private fun parseHtmlTags(html: String): RichTextDocument {
        val spans = mutableListOf<SpanRange>()
        val paragraphs = mutableListOf<ParagraphRange>()
        val alignments = mutableListOf<AlignmentRange>()
        val cleanText = StringBuilder()

        val tagStack = mutableListOf<OpenTagInfo>()

        var i = 0
        while (i < html.length) {
            if (html[i] == '<') {
                val closeIdx = html.indexOf('>', i)
                if (closeIdx != -1) {
                    val fullTag = html.substring(i, closeIdx + 1)
                    val isClosing = fullTag.startsWith("</")
                    val tagBody = fullTag.trim('<', '>', '/').trim()
                    val tagName = tagBody.split(Regex("\\s+"))[0].lowercase()

                    if (!isClosing) {
                        val attributes = parseAttributes(tagBody)
                        tagStack.add(OpenTagInfo(tagName, cleanText.length, attributes))
                    } else {
                        val stackIdx = tagStack.indexOfLast { it.name == tagName }
                        if (stackIdx != -1) {
                            val openTag = tagStack.removeAt(stackIdx)
                            val startPos = openTag.startPos
                            val endPos = cleanText.length
                            if (endPos > startPos) {
                                when (openTag.name) {
                                    "b", "strong" -> spans.add(SpanRange(SpanType.Bold, startPos, endPos))
                                    "i", "em" -> spans.add(SpanRange(SpanType.Italic, startPos, endPos))
                                    "u" -> spans.add(SpanRange(SpanType.Underline, startPos, endPos))
                                    "s", "strike", "del" -> spans.add(SpanRange(SpanType.Strikethrough, startPos, endPos))
                                    "code", "pre" -> spans.add(SpanRange(SpanType.Code, startPos, endPos))
                                    "mark" -> spans.add(SpanRange(SpanType.Highlight(), startPos, endPos))
                                    "a" -> {
                                        val url = openTag.attributes["href"] ?: ""
                                        spans.add(SpanRange(SpanType.Link(url), startPos, endPos))
                                    }
                                    "h1" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(1), startPos, endPos))
                                    "h2" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(2), startPos, endPos))
                                    "h3" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(3), startPos, endPos))
                                    "h4" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(4), startPos, endPos))
                                    "h5" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(5), startPos, endPos))
                                    "h6" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(6), startPos, endPos))
                                    "blockquote" -> paragraphs.add(ParagraphRange(ParagraphType.Quote, startPos, endPos))
                                    "li" -> {
                                        val isOrdered = tagStack.any { it.name == "ol" }
                                        val pType = if (isOrdered) ParagraphType.NumberedList() else ParagraphType.BulletList
                                        paragraphs.add(ParagraphRange(pType, startPos, endPos))
                                    }
                                    "ul" -> {
                                        // If ul contained text without <li>, add paragraph for it
                                        val hasChildLi = paragraphs.any { it.type is ParagraphType.BulletList && it.start >= startPos && it.end <= endPos }
                                        if (!hasChildLi) {
                                            paragraphs.add(ParagraphRange(ParagraphType.BulletList, startPos, endPos))
                                        }
                                    }
                                    "ol" -> {
                                        val hasChildLi = paragraphs.any { it.type is ParagraphType.NumberedList && it.start >= startPos && it.end <= endPos }
                                        if (!hasChildLi) {
                                            paragraphs.add(ParagraphRange(ParagraphType.NumberedList(), startPos, endPos))
                                        }
                                    }
                                }

                                val style = openTag.attributes["style"] ?: ""
                                val alignAttr = openTag.attributes["align"] ?: ""
                                val alignment = when {
                                    openTag.name == "center" -> TextAlignment.Center
                                    style.contains("text-align:center", ignoreCase = true) || style.contains("text-align: center", ignoreCase = true) || alignAttr.equals("center", ignoreCase = true) -> TextAlignment.Center
                                    style.contains("text-align:right", ignoreCase = true) || style.contains("text-align: right", ignoreCase = true) || alignAttr.equals("right", ignoreCase = true) -> TextAlignment.End
                                    style.contains("text-align:left", ignoreCase = true) || style.contains("text-align: left", ignoreCase = true) || alignAttr.equals("left", ignoreCase = true) -> TextAlignment.Start
                                    else -> null
                                }
                                if (alignment != null) {
                                    alignments.add(AlignmentRange(alignment, startPos, endPos))
                                }
                            }
                        }
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // HTML entity decoding (&amp;, &lt;, &gt;, &quot;, &#39;, &nbsp;)
            if (html[i] == '&') {
                val semiIdx = html.indexOf(';', i)
                if (semiIdx != -1 && semiIdx - i in 2..8) {
                    val entity = html.substring(i, semiIdx + 1)
                    val decodedChar = when (entity.lowercase()) {
                        "&amp;" -> '&'
                        "&lt;" -> '<'
                        "&gt;" -> '>'
                        "&quot;" -> '"'
                        "&#39;", "&apos;" -> '\''
                        "&nbsp;" -> ' '
                        else -> null
                    }
                    if (decodedChar != null) {
                        cleanText.append(decodedChar)
                        i = semiIdx + 1
                        continue
                    }
                }
            }

            cleanText.append(html[i])
            i++
        }

        val textStr = cleanText.toString()
        val normalizedParagraphs = mutableListOf<ParagraphRange>()
        for (para in paragraphs) {
            val pStart = para.start.coerceIn(0, textStr.length)
            val pEnd = para.end.coerceIn(0, textStr.length)
            if (pStart >= pEnd) continue
            val paraSubstring = textStr.substring(pStart, pEnd)
            if (paraSubstring.contains('\n')) {
                var lineStart = pStart
                while (lineStart < pEnd) {
                    val nextNewline = textStr.indexOf('\n', lineStart)
                    val lineEnd = if (nextNewline == -1 || nextNewline > pEnd) pEnd else nextNewline
                    if (lineStart < lineEnd) {
                        normalizedParagraphs.add(ParagraphRange(para.type, lineStart, lineEnd))
                    }
                    if (nextNewline == -1 || nextNewline >= pEnd) break
                    lineStart = nextNewline + 1
                }
            } else {
                normalizedParagraphs.add(para)
            }
        }

        val normalizedAlignments = mutableListOf<AlignmentRange>()
        for (align in alignments) {
            val aStart = align.start.coerceIn(0, textStr.length)
            val aEnd = align.end.coerceIn(0, textStr.length)
            if (aStart >= aEnd) continue
            val alignSubstring = textStr.substring(aStart, aEnd)
            if (alignSubstring.contains('\n')) {
                var lineStart = aStart
                while (lineStart < aEnd) {
                    val nextNewline = textStr.indexOf('\n', lineStart)
                    val lineEnd = if (nextNewline == -1 || nextNewline > aEnd) aEnd else nextNewline
                    if (lineStart < lineEnd) {
                        normalizedAlignments.add(AlignmentRange(align.alignment, lineStart, lineEnd))
                    }
                    if (nextNewline == -1 || nextNewline >= aEnd) break
                    lineStart = nextNewline + 1
                }
            } else {
                normalizedAlignments.add(align)
            }
        }

        return RichTextDocument(
            text = textStr,
            spans = spans.sortedBy { it.start },
            paragraphs = normalizedParagraphs.sortedBy { it.start },
            alignments = normalizedAlignments.sortedBy { it.start }
        ).normalize()
    }
}

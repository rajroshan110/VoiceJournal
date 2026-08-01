package dev.voicejournal.ui.notedetail.editor.serializer

import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType

object RichTextHtmlSerializer {

    fun toPlainText(document: RichTextDocument): String {
        return document.text
    }

    fun toHtml(document: RichTextDocument): String {
        if (document.text.isEmpty()) return ""
        if (document.spans.isEmpty() && document.paragraphs.isEmpty()) {
            return document.text
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
                is SpanType.Link -> "a href=\"${t.url}\""
            }
            val closeTag = if (span.type is SpanType.Link) "a" else tag
            events.add(TagEvent(start, false, "<$tag>", 1))
            events.add(TagEvent(end, true, "</$closeTag>", 0))
        }

        for (para in document.paragraphs) {
            val start = para.start.coerceIn(0, len)
            val end = para.end.coerceIn(0, len)
            if (start >= end) continue

            val (openTag, closeTag) = when (val t = para.type) {
                is ParagraphType.Heading -> Pair("<h${t.level}>", "</h${t.level}>")
                is ParagraphType.Quote -> Pair("<blockquote>", "</blockquote>")
                is ParagraphType.BulletList -> Pair("<ul><li>", "</li></ul>")
                is ParagraphType.NumberedList -> Pair("<ol><li>", "</li></ol>")
            }
            events.add(TagEvent(start, false, openTag, 2))
            events.add(TagEvent(end, true, closeTag, -1))
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
                sb.append(raw.substring(lastPos, event.pos))
                lastPos = event.pos
            }
            sb.append(event.tag)
        }

        if (lastPos < len) {
            sb.append(raw.substring(lastPos))
        }

        return sb.toString()
    }

    fun fromHtml(html: String?): RichTextDocument {
        if (html.isNullOrEmpty()) return RichTextDocument.EMPTY

        // Check if string contains HTML tags
        val hasHtmlTags = html.contains("<b") || html.contains("<i") || html.contains("<u") ||
                html.contains("<s") || html.contains("<code") || html.contains("<mark") ||
                html.contains("<h") || html.contains("<blockquote")

        if (hasHtmlTags) {
            return parseHtmlTags(html)
        }

        // Check if string contains legacy Markdown tags (**bold**, *italic*, <u>underline</u>, ~~strike~~, # heading)
        if (html.contains("**") || html.contains("~~") || html.contains("<u>") || html.startsWith("#") || html.startsWith("- ")) {
            return parseLegacyMarkdown(html)
        }

        return RichTextDocument(text = html)
    }

    private fun parseHtmlTags(html: String): RichTextDocument {
        val spans = mutableListOf<SpanRange>()
        val paragraphs = mutableListOf<ParagraphRange>()
        val cleanText = StringBuilder()

        val tagStack = mutableListOf<Pair<String, Int>>()

        var i = 0
        while (i < html.length) {
            if (html[i] == '<') {
                val closeIdx = html.indexOf('>', i)
                if (closeIdx != -1) {
                    val fullTag = html.substring(i, closeIdx + 1)
                    val tagName = fullTag.trim('<', '>', '/').lowercase().split(" ")[0]
                    val isClosing = fullTag.startsWith("</")

                    if (!isClosing) {
                        tagStack.add(Pair(tagName, cleanText.length))
                    } else {
                        val stackIdx = tagStack.indexOfLast { it.first == tagName }
                        if (stackIdx != -1) {
                            val (tag, startPos) = tagStack.removeAt(stackIdx)
                            val endPos = cleanText.length
                            if (endPos > startPos) {
                                when (tag) {
                                    "b", "strong" -> spans.add(SpanRange(SpanType.Bold, startPos, endPos))
                                    "i", "em" -> spans.add(SpanRange(SpanType.Italic, startPos, endPos))
                                    "u" -> spans.add(SpanRange(SpanType.Underline, startPos, endPos))
                                    "s", "strike", "del" -> spans.add(SpanRange(SpanType.Strikethrough, startPos, endPos))
                                    "code" -> spans.add(SpanRange(SpanType.Code, startPos, endPos))
                                    "mark" -> spans.add(SpanRange(SpanType.Highlight(), startPos, endPos))
                                    "a" -> spans.add(SpanRange(SpanType.Link(""), startPos, endPos))
                                    "h1" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(1), startPos, endPos))
                                    "h2" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(2), startPos, endPos))
                                    "h3" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(3), startPos, endPos))
                                    "h4" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(4), startPos, endPos))
                                    "h5" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(5), startPos, endPos))
                                    "h6" -> paragraphs.add(ParagraphRange(ParagraphType.Heading(6), startPos, endPos))
                                    "blockquote" -> paragraphs.add(ParagraphRange(ParagraphType.Quote, startPos, endPos))
                                    "ul" -> paragraphs.add(ParagraphRange(ParagraphType.BulletList, startPos, endPos))
                                    "ol" -> paragraphs.add(ParagraphRange(ParagraphType.NumberedList(), startPos, endPos))
                                }
                            }
                        }
                    }
                    i = closeIdx + 1
                    continue
                }
            }
            cleanText.append(html[i])
            i++
        }

        return RichTextDocument(
            text = cleanText.toString(),
            spans = spans.sortedBy { it.start },
            paragraphs = paragraphs.sortedBy { it.start }
        )
    }

    private fun parseLegacyMarkdown(raw: String): RichTextDocument {
        var text = raw
        val spans = mutableListOf<SpanRange>()

        // Convert legacy **bold**
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { m ->
            val content = m.groupValues[1]
            val start = m.range.first
            text = text.replaceRange(m.range, content)
            spans.add(SpanRange(SpanType.Bold, start, start + content.length))
        }

        // Convert legacy ~~strikethrough~~
        val strikeRegex = Regex("~~(.*?)~~")
        strikeRegex.findAll(text).forEach { m ->
            val content = m.groupValues[1]
            val start = m.range.first
            text = text.replaceRange(m.range, content)
            spans.add(SpanRange(SpanType.Strikethrough, start, start + content.length))
        }

        // Convert legacy <u>underline</u>
        val uRegex = Regex("<u>(.*?)</u>")
        uRegex.findAll(text).forEach { m ->
            val content = m.groupValues[1]
            val start = m.range.first
            text = text.replaceRange(m.range, content)
            spans.add(SpanRange(SpanType.Underline, start, start + content.length))
        }

        return RichTextDocument(text = text, spans = spans.sortedBy { it.start })
    }
}

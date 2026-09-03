package dev.voicejournal.ui.notedetail.editor.serializer

import dev.voicejournal.ui.notedetail.editor.model.ParagraphRange
import dev.voicejournal.ui.notedetail.editor.model.ParagraphType
import dev.voicejournal.ui.notedetail.editor.model.RichTextDocument
import dev.voicejournal.ui.notedetail.editor.model.SpanRange
import dev.voicejournal.ui.notedetail.editor.model.SpanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextHtmlSerializerTest {

    @Test
    fun `toHtml and fromHtml preserves link URLs accurately`() {
        val originalDoc = RichTextDocument(
            text = "Visit VoiceJournal or Google today.",
            spans = listOf(
                SpanRange(SpanType.Link("https://voicejournal.dev"), 6, 18),
                SpanRange(SpanType.Link("https://google.com/search?q=test#anchor"), 22, 28)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(originalDoc)
        assertEquals(
            "Visit <a href=\"https://voicejournal.dev\">VoiceJournal</a> or <a href=\"https://google.com/search?q=test#anchor\">Google</a> today.",
            html
        )

        val restoredDoc = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(originalDoc.text, restoredDoc.text)
        assertEquals(2, restoredDoc.spans.size)

        val link1 = restoredDoc.spans[0]
        assertEquals(6, link1.start)
        assertEquals(18, link1.end)
        assertTrue(link1.type is SpanType.Link)
        assertEquals("https://voicejournal.dev", (link1.type as SpanType.Link).url)

        val link2 = restoredDoc.spans[1]
        assertEquals(22, link2.start)
        assertEquals(28, link2.end)
        assertTrue(link2.type is SpanType.Link)
        assertEquals("https://google.com/search?q=test#anchor", (link2.type as SpanType.Link).url)
    }

    @Test
    fun `fromHtml parses note containing ONLY a link without other tags`() {
        val html = "<a href=\"https://example.org\">Click Here</a>"
        val doc = RichTextHtmlSerializer.fromHtml(html)
        assertEquals("Click Here", doc.text)
        assertEquals(1, doc.spans.size)
        assertEquals(SpanType.Link("https://example.org"), doc.spans[0].type)
        assertEquals(0, doc.spans[0].start)
        assertEquals(10, doc.spans[0].end)
    }

    @Test
    fun `fromHtml parses note containing ONLY a list without other tags`() {
        val html = "<ul><li>Single Item</li></ul>"
        val doc = RichTextHtmlSerializer.fromHtml(html)
        assertEquals("Single Item", doc.text)
        assertEquals(1, doc.paragraphs.size)
        assertEquals(ParagraphType.BulletList, doc.paragraphs[0].type)
        assertEquals(0, doc.paragraphs[0].start)
        assertEquals(11, doc.paragraphs[0].end)
    }

    @Test
    fun `toHtml groups consecutive bullet list paragraphs under single ul`() {
        val text = "Apple\nBanana\nCherry"
        val doc = RichTextDocument(
            text = text,
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 5),
                ParagraphRange(ParagraphType.BulletList, 6, 12),
                ParagraphRange(ParagraphType.BulletList, 13, 19)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("<ul><li>Apple</li>\n<li>Banana</li>\n<li>Cherry</li></ul>", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(3, restored.paragraphs.size)
        assertTrue(restored.paragraphs.all { it.type is ParagraphType.BulletList })
    }

    @Test
    fun `toHtml groups consecutive numbered list paragraphs under single ol`() {
        val text = "Step 1\nStep 2"
        val doc = RichTextDocument(
            text = text,
            paragraphs = listOf(
                ParagraphRange(ParagraphType.NumberedList(), 0, 6),
                ParagraphRange(ParagraphType.NumberedList(), 7, 13)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("<ol><li>Step 1</li>\n<li>Step 2</li></ol>", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(2, restored.paragraphs.size)
        assertTrue(restored.paragraphs.all { it.type is ParagraphType.NumberedList })
    }

    @Test
    fun `toHtml handles adjacent different list types properly`() {
        val text = "Bullet item\nNumbered item"
        val doc = RichTextDocument(
            text = text,
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 11),
                ParagraphRange(ParagraphType.NumberedList(), 12, 25)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("<ul><li>Bullet item</li></ul>\n<ol><li>Numbered item</li></ol>", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(2, restored.paragraphs.size)
        assertEquals(ParagraphType.BulletList, restored.paragraphs[0].type)
        assertEquals(ParagraphType.NumberedList(), restored.paragraphs[1].type)
    }

    @Test
    fun `toHtml handles separated list groups with intervening plain paragraph correctly`() {
        val text = "Item 1\nPlain text in middle\nItem 2"
        val doc = RichTextDocument(
            text = text,
            paragraphs = listOf(
                ParagraphRange(ParagraphType.BulletList, 0, 6),
                ParagraphRange(ParagraphType.BulletList, 28, 34)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("<ul><li>Item 1</li></ul>\nPlain text in middle\n<ul><li>Item 2</li></ul>", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(2, restored.paragraphs.size)
        assertEquals(0, restored.paragraphs[0].start)
        assertEquals(6, restored.paragraphs[0].end)
        assertEquals(28, restored.paragraphs[1].start)
        assertEquals(34, restored.paragraphs[1].end)
    }

    @Test
    fun `fromHtml safely handles HTML entities`() {
        val html = "Tom &amp; Jerry &lt;3 cheese &quot;always&quot;"
        val doc = RichTextHtmlSerializer.fromHtml(html)
        assertEquals("Tom & Jerry <3 cheese \"always\"", doc.text)
    }

    @Test
    fun `fromHtml loads plain text notes literally without converting markdown symbols into spans`() {
        val plainTextWithSymbols = "This is **bold 1** and ~~strike 1~~ followed by **bold 2** and `code`."
        val doc = RichTextHtmlSerializer.fromHtml(plainTextWithSymbols)

        assertEquals(plainTextWithSymbols, doc.text)
        assertTrue(doc.spans.isEmpty())
    }

    @Test
    fun `fromHtml safely handles asterisks, underscores, and hashes without corrupting text or spans`() {
        val raw = "# Header and *item* and _note_ and file_name_final.txt"
        val doc = RichTextHtmlSerializer.fromHtml(raw)

        assertEquals(raw, doc.text)
        assertTrue(doc.spans.isEmpty())
        assertTrue(doc.paragraphs.isEmpty())
    }

    @Test
    fun `toHtml and fromHtml roundtrips combined formatting`() {
        val text = "Bold Title\nQuoted insight here\nHighlighted point"
        val doc = RichTextDocument(
            text = text,
            spans = listOf(
                SpanRange(SpanType.Bold, 0, 10),
                SpanRange(SpanType.Highlight(), 31, 48)
            ),
            paragraphs = listOf(
                ParagraphRange(ParagraphType.Heading(1), 0, 10),
                ParagraphRange(ParagraphType.Quote, 11, 30)
            )
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        val restored = RichTextHtmlSerializer.fromHtml(html)

        assertEquals(doc.text, restored.text)
        assertEquals(2, restored.spans.size)
        assertEquals(2, restored.paragraphs.size)
    }

    @Test
    fun `fromHtml normalizes multiline ol and ul blocks into per line paragraphs`() {
        val html = "<ol>Line 1\nLine 2\nLine 3</ol>"
        val doc = RichTextHtmlSerializer.fromHtml(html)

        assertEquals("Line 1\nLine 2\nLine 3", doc.text)
        assertEquals(3, doc.paragraphs.size)
        assertEquals(0, doc.paragraphs[0].start)
        assertEquals(6, doc.paragraphs[0].end)
        assertEquals(7, doc.paragraphs[1].start)
        assertEquals(13, doc.paragraphs[1].end)
        assertEquals(14, doc.paragraphs[2].start)
        assertEquals(20, doc.paragraphs[2].end)
    }

    @Test
    fun `toHtml and fromHtml losslessly roundtrips raw angle brackets and ampersands`() {
        val raw = "x < y && y > z and 5 > 3 & AT&T <tag>"
        val doc = RichTextDocument(text = raw)

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("x &lt; y &amp;&amp; y &gt; z and 5 &gt; 3 &amp; AT&amp;T &lt;tag&gt;", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(raw, restored.text)
        assertTrue(restored.spans.isEmpty())
        assertTrue(restored.paragraphs.isEmpty())
    }

    @Test
    fun `special characters inside formatted spans roundtrip identically`() {
        val text = "Before a < b & c > d After"
        val doc = RichTextDocument(
            text = text,
            spans = listOf(SpanRange(SpanType.Bold, 7, 20)) // "a < b & c > d" in Bold
        )

        val html = RichTextHtmlSerializer.toHtml(doc)
        assertEquals("Before <b>a &lt; b &amp; c &gt; d</b> After", html)

        val restored = RichTextHtmlSerializer.fromHtml(html)
        assertEquals(text, restored.text)
        assertEquals(1, restored.spans.size)
        val boldSpan = restored.spans[0]
        assertEquals(7, boldSpan.start)
        assertEquals(20, boldSpan.end)
        assertEquals(SpanType.Bold, boldSpan.type)
    }
}

package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.util.ArrayDeque
import java.util.Deque
import arrow.atomic.AtomicInt
import nl.knaw.huygens.tei.Context
import nl.knaw.huygens.tei.DelegatingVisitor
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.ElementHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.Traversal.NEXT
import nl.knaw.huygens.tei.XmlContext
import nl.knaw.huygens.tei.handlers.DefaultTextHandler

internal class ParagraphVisitor(divType: String, lang: String) : DelegatingVisitor<XmlContext>(XmlContext()) {

    val paraCounter = AtomicInt(2)

    init {
        paraCounter.set(2)
        setTextHandler(MyTextHandler(divType, lang, paraCounter))
        setCommentHandler(IgnoreCommentHandler())
        setDefaultElementHandler(MyElementHandler(divType, lang))
    }

    class MyTextHandler<T : Context>(
        val divType: String,
        val lang: String,
        val paraCounter: AtomicInt
    ) :
        DefaultTextHandler<T>() {

        override fun filterText(text: String): String {
            val n = text.length
            val builder = StringBuilder((n * 1.1).toInt())
            for (i in 0..<n) {
                val c = text.get(i)
                when (c) {
                    '<' -> builder.append("&lt;")
                    '>' -> builder.append("&gt;")
                    '&' -> builder.append("&amp;")
                    else -> builder.append(c)
                }
            }
            val filteredText = builder.toString().replace("\n\n", "\n")
            if (filteredText.isNotBlank() && filteredText.contains("\n")) {
                val parts = filteredText.split("\n")
                if (parts.size > 2) {
                    throw RuntimeException(">1 newlines in text: '$text'")
                }
                val n = paraCounter.andIncrement
                val indent = if (parts[1].startsWith(" ")) {
                    " rend=\"indent\""
                } else {
                    ""
                }
                val opener = "<p xml:id=\"p.$divType.$lang.$n\" n=\"$n\"$indent>"

                val closingTags = openElements.descendingIterator().asSequence().map { "</${it.name}>" }.joinToString()
                val openingTags = openElements.iterator()
                    .asSequence()
                    .map {
                        val builder = StringBuilder()
                        it.appendOpenTagTo(builder)
                        builder.toString()
                    }
                    .joinToString()

                return parts[0].trim() + closingTags + "</p>\n" + opener + openingTags + parts[1].trim()
            } else {
                return filteredText
            }
        }

    }

    internal class MyElementHandler(val divType: String, val lang: String) : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            when (element.name) {
                "xml" -> {
                    val p = Element("p").withAttribute("xml:id", "p.$divType.$lang.1").withAttribute("n", "1")
                    context.addOpenTag(p)
                }

                "space", "pb", "ptr" -> context.addEmptyElementTag(element)

                else -> {
                    context.addOpenTag(element)
                    openElements.push(element)
                }
            }
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            when (element.name) {
                "xml" -> {
                    context.addCloseTag("p")
                }

                "space", "pb", "ptr" -> {}

                else -> {
                    context.addCloseTag(openElements.pop())
                }
            }
            return NEXT
        }
    }

    companion object {
        private val openElements: Deque<Element> = ArrayDeque()
    }
}
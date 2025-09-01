package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.util.ArrayDeque
import java.util.Deque
import nl.knaw.huygens.tei.DelegatingVisitor
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.ElementHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.Traversal.NEXT
import nl.knaw.huygens.tei.XmlContext
import nl.knaw.huygens.tei.handlers.XmlTextHandler

internal class WordPressExportItemContentVisitor() : DelegatingVisitor<XmlContext>(XmlContext()) {

    init {
        setTextHandler(XmlTextHandler())
        setCommentHandler(IgnoreCommentHandler())
        setDefaultElementHandler(DefaultElementHandler())
        addElementHandler(RefHandler(), "a")
        addElementHandler(BrHandler(), "br")
        addElementHandler(AsCommentHandler(), "button", "iframe")
//        addElementHandler(RemoveAttributesHandler(), "p")
//        addElementHandler(ImgHandler(), "img")
    }

    internal class BrHandler() : ElementHandler<XmlContext> {

        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.addEmptyElementTag("lb")
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }
    }

    internal class ImgHandler() : ElementHandler<XmlContext> {

        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val newElement = Element("figure")
            context.addOpenTag(newElement)
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.addCloseTag("figure")
            return NEXT
        }
    }

    internal class RefHandler() : ElementHandler<XmlContext> {
        var closeElement = true
        var closingElement = ""
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            if (element.hasAttribute("href")) {
                val newElement = if (element.getAttribute("href").contains("#_ftn")) {
                    Element("ptr").withAttribute("target", element.getAttribute("href"))
                } else {
                    Element("ref").withAttribute("target", element.getAttribute("href"))
                }
                context.addOpenTag(newElement)
                closeElement = true
                closingElement = newElement.name

            } else {
                val newElement = Element("anchor").withAttribute("xml:id", element.getAttribute("name"))
                context.addEmptyElementTag(newElement)
                closeElement = false

            }
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            if (closeElement) {
                context.addCloseTag(closingElement)
            }
            return NEXT
        }
    }

    internal class AsCommentHandler() : ElementHandler<XmlContext> {

        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.apply {
                addLiteral("<!-- ")
                addOpenTag(element)
                addLiteral(" -->")
            }
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.apply {
                addLiteral("<!-- ")
                addCloseTag(element)
                addLiteral(" -->")
            }
            return NEXT
        }
    }

    internal class RemoveAttributesHandler() : ElementHandler<XmlContext> {

        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val newElement = Element(element.name)
            context.addOpenTag(newElement)
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.addCloseTag(element)
            return NEXT
        }
    }

    internal class DefaultElementHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val name: String = element.name
            if (TEIBuilder.HI_TAGS.containsKey(name)) {
                val hi: Element = Element("hi").withAttribute("rend", TEIBuilder.HI_TAGS[name])
                context.addOpenTag(hi)
                openElements.push(hi)
            } else {
                context.addOpenTag(element)
                openElements.push(element)
            }
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.addCloseTag(openElements.pop())
            return NEXT
        }
    }

    companion object {
        private val openElements: Deque<Element> = ArrayDeque()
    }
}


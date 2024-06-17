package nl.knaw.huc.di.elaborate.elabctl

import java.util.TreeSet
import nl.knaw.huygens.tei.Comment
import nl.knaw.huygens.tei.CommentHandler
import nl.knaw.huygens.tei.DelegatingVisitor
import nl.knaw.huygens.tei.Document
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.ElementHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.Traversal.STOP
import nl.knaw.huygens.tei.XmlContext
import nl.knaw.huygens.tei.handlers.XmlTextHandler

class AnnotationBodyConverter {

    class DefaultElementHandler : ElementHandler<XmlContext> {
        override fun enterElement(e: Element, c: XmlContext): Traversal {
            unhandledTags.add(e.name)
            c.addOpenTag(e)
            return Traversal.NEXT
        }

        override fun leaveElement(e: Element, c: XmlContext): Traversal {
            c.addCloseTag(e)
            return Traversal.NEXT
        }
    }

    class BrHandler : ElementHandler<XmlContext> {
        override fun enterElement(e: Element, c: XmlContext): Traversal {
            return Traversal.NEXT
        }

        override fun leaveElement(e: Element, c: XmlContext): Traversal {
            c.addEmptyElementTag("lb")
            return Traversal.NEXT
        }
    }

    class DelHandler : ElementHandler<XmlContext> {
        override fun enterElement(e: Element, c: XmlContext): Traversal {
            c.addOpenTag(DEL)
            return Traversal.NEXT
        }

        override fun leaveElement(e: Element, c: XmlContext): Traversal {
            c.addCloseTag(DEL)
            return Traversal.NEXT
        }

        companion object {
            private const val DEL = "del"
        }
    }

    class HiHandler : ElementHandler<XmlContext> {
        override fun enterElement(e: Element, c: XmlContext): Traversal {
            val hi = hiElement(e)
            c.addOpenTag(hi)
            return Traversal.NEXT
        }

        override fun leaveElement(e: Element, c: XmlContext): Traversal {
            val hi = hiElement(e)
            c.addCloseTag(hi)
            return Traversal.NEXT
        }

        private fun hiElement(e: Element): Element {
            return Element("hi").withAttribute("rend", TEIBuilder.HI_TAGS[e.name])
        }
    }

    class IgnoreElementHandler : ElementHandler<XmlContext?> {
        override fun enterElement(e: Element, c: XmlContext?): Traversal {
            return Traversal.NEXT
        }

        override fun leaveElement(e: Element, c: XmlContext?): Traversal {
            return Traversal.NEXT
        }
    }

    class IgnoreCommentHandler : CommentHandler<XmlContext> {
        override fun visitComment(p0: Comment?, p1: XmlContext?): Traversal {
            return STOP
        }
    }

    companion object {
        val unhandledTags: TreeSet<String> = TreeSet()
        fun convert(xml: String): String {
//            logger.info { "val xml=\"\"\"$xml\"\"\"\n" }
            val fixedXml: String = fixXhtml(wrapInXml(xml))
            try {
                val document = Document.createFromXml(fixedXml, false)

                val visitor = DelegatingVisitor(XmlContext())
                visitor.setTextHandler(XmlTextHandler())
                visitor.setCommentHandler(IgnoreCommentHandler())
                visitor.setDefaultElementHandler(DefaultElementHandler())
                visitor.addElementHandler(IgnoreElementHandler(), "xml", "span")
                visitor.addElementHandler(HiHandler(), *TEIBuilder.HI_TAGS.keys.toTypedArray())
                visitor.addElementHandler(DelHandler(), "strike")
                visitor.addElementHandler(BrHandler(), "br")

                document.accept(visitor)
                if (!unhandledTags.isEmpty()) {
                    logger.warn { "unhandled tags: $unhandledTags for annotation body $fixedXml" }
                    unhandledTags.clear()
                }
                return visitor.context.result
            } catch (e: Exception) {
                e.printStackTrace()
                return " :: error in parsing annotation body ::" + e.message
            }
        }
    }

}

package nl.knaw.huc.di.elaborate.elabctl.archiver

import com.google.common.collect.TreeMultimap
import nl.knaw.huygens.tei.Comment
import nl.knaw.huygens.tei.CommentHandler
import nl.knaw.huygens.tei.DelegatingVisitor
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.ElementHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.Traversal.NEXT
import nl.knaw.huygens.tei.Traversal.STOP
import nl.knaw.huygens.tei.XmlContext
import nl.knaw.huygens.tei.handlers.XmlTextHandler

class ElementInventoryVisitor() : DelegatingVisitor<XmlContext>(XmlContext()) {

    init {
        setTextHandler(XmlTextHandler())
        setCommentHandler(IgnoreCommentHandler())
        setDefaultElementHandler(DefaultElementHandler())
    }

    fun elementInventory(): Map<String, Collection<String>> = elementInventory.asMap()
    fun elementNames(): List<String> = elementSet.sorted()

    internal class DefaultElementHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val name: String = element.name
            val attributes = element.attributes.keys
            elementSet.add(name)
            elementInventory.putAll(name, attributes)
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }
    }

    class IgnoreCommentHandler : CommentHandler<XmlContext> {
        override fun visitComment(p0: Comment?, p1: XmlContext?): Traversal {
            return STOP
        }
    }

    companion object {
        val elementSet = mutableSetOf<String>()
        val elementInventory: TreeMultimap<String, String> = TreeMultimap.create<String, String>()
    }
}


package nl.knaw.huc.di.elaborate.elabctl.archiver

import com.google.common.collect.TreeMultimap
import nl.knaw.huygens.tei.DelegatingVisitor
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.ElementHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.Traversal.NEXT
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
    fun elementParents(): Map<String, Collection<String>> = elementParents.asMap()

    internal class DefaultElementHandler : ElementHandler<XmlContext> {
        val wrapElement = "xml"
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val parent = parentElements.firstOrNull()
            val name: String = element.name
            if (parent != null && parent.name != wrapElement) {
                elementParents.put(name, parent.name)
            }
            val attributes = element.attributes.keys
            if (name != wrapElement) {
                elementSet.add(name)
                elementInventory.putAll(name, attributes)
            }
            parentElements.addFirst(element)
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            parentElements.removeFirst()
            return NEXT
        }
    }

    companion object {
        val elementSet = mutableSetOf<String>()
        val elementInventory: TreeMultimap<String, String> = TreeMultimap.create<String, String>()
        val parentElements = ArrayDeque<Element>()
        val elementParents: TreeMultimap<String, String> = TreeMultimap.create<String, String>()
    }
}


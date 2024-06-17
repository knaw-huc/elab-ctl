package nl.knaw.huc.di.elaborate.elabctl

import java.util.ArrayDeque
import java.util.Deque
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

internal class TranscriptionVisitor(
    annotationMap: Map<Long, AnnotationData>
) : DelegatingVisitor<XmlContext>(XmlContext()) {

    init {
        setTextHandler(XmlTextHandler())
        setCommentHandler(IgnoreCommentHandler())
        setDefaultElementHandler(DefaultElementHandler())
        addElementHandler(SupHandler(annotationMap), "sup")
        addElementHandler(BrHandler(), "br")
        addElementHandler(LbHandler(), TAG_LB)
        addElementHandler(IgnoreHandler(NEXT), "content")
        addElementHandler(DivHandler(), "div")
        addElementHandler(XmlHandler(), "xml")
        addElementHandler(SpanHandler(), "span")
        linenum = 1
//        addElementHandler(
//            AnnotationHandler(config, entityManager),
//            Transcription.BodyTags.ANNOTATION_BEGIN,
//            Transcription.BodyTags.ANNOTATION_END
//        )
    }

    internal class BrHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.addEmptyElementTag(Element(TAG_LB, "n", linenum.toString()))
            linenum++
            return NEXT
        }
    }

    internal class LbHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            if (skipNextNewline) {
                skipNextNewline = false
            } else {
                context.addEmptyElementTag(TAG_LB)
            }
            return NEXT
        }
    }

    internal class IgnoreHandler(private val onEnter: Traversal) : ElementHandler<XmlContext> {

        override fun enterElement(element: Element, context: XmlContext): Traversal {
            return onEnter
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }
    }

    internal class DivHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }
    }

    internal class XmlHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.addOpenTag(element)
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            context.addCloseTag(element)
            return NEXT
        }
    }

    internal class SpanHandler() : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
//            val type: String = element.getAttribute("data-type")
            return NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            return NEXT
        }

    }

//    internal class AnnotationHandler(config: TeiConversionConfig, entityManager: EntityManager?) :
//        ElementHandler<XmlContext> {
//        private val config: TeiConversionConfig = config
//        private val entityManager: EntityManager? = entityManager
//
//        override fun enterElement(element: Element, context: XmlContext): Traversal {
//            val id: String = element.getAttribute("id")
//            val annotation = getAnnotation(id)
//            if (annotation != null) {
//                val annotationType: AnnotationType = annotation.getAnnotationType()
//                val name: String = element.getName()
//                if (name == Transcription.BodyTags.ANNOTATION_BEGIN) {
//                    if (isMappable(annotationType)) {
//                        val taginfo: TagInfo = tagInfo(annotation, annotationType)
//                        context.addOpenTag(Element(taginfo.getName(), taginfo.getAttributes()))
//                    } else {
//                        // addPtr(context, id, "annotation_begin");
//                    }
//                } else if (name == Transcription.BodyTags.ANNOTATION_END) {
//                    if (isMappable(annotationType)) {
//                        val taginfo: TagInfo = tagInfo(annotation, annotationType)
//                        context.addCloseTag(Element(taginfo.getName()))
//                        skipNextNewline = taginfo.skipNewlineAfter()
//                    } else {
//                        // addPtr(context, id, "annotation_end");
//                        addNote(context, annotation)
//                    }
//                }
//            }
//
//            return STOP
//        }
//
//        override fun leaveElement(element: Element, context: XmlContext): Traversal {
//            return NEXT
//        }
//
//        private fun isMappable(annotationType: AnnotationType): Boolean {
//            return config.getAnnotationTypeMapper().containsKey(annotationType)
//        }
//
//        private fun tagInfo(annotation: Annotation, annotationType: AnnotationType): TagInfo {
//            return config.getAnnotationTypeMapper().get(annotationType).apply(annotation)
//        }
//
//        private fun addNote(context: XmlContext, annotation: Annotation) {
//            val note: Element = Element("note")
//            note.setAttribute("xml:id", "note" + annotation.getAnnotationNo())
//            note.setAttribute("type", annotation.getAnnotationType().getName())
//            val annotationBody: String = annotation.getBody()
//            context.addOpenTag(note)
//            val annotationMetadataItems: Set<AnnotationMetadataItem> = annotation.getAnnotationMetadataItems()
//            if (!annotationMetadataItems.isEmpty()) {
//                context.addOpenTag(TEIBuilder.INTERP_GRP)
//                for (annotationMetadataItem in annotationMetadataItems) {
//                    val type: String = annotationMetadataItem.getAnnotationTypeMetadataItem().getName()
//                    val value: String = annotationMetadataItem.getData()
//                    context.addEmptyElementTag(interp(type, value))
//                }
//                context.addCloseTag(TeiMaker.INTERP_GRP)
//            }
//            context.addLiteral(AnnotationBodyConverter.convert(annotationBody))
//            context.addCloseTag(note)
//        }
//
//        private fun interp(key: String, value: String): Element {
//            val attrs: MutableMap<String, String> = Maps.newHashMap()
//            attrs["type"] = key
//            attrs["value"] = StringEscapeUtils.escapeHtml(value)
//            return Element("interp", attrs)
//        }
//
//        private fun getAnnotation(annotationId: String): Annotation {
//            return AnnotationService.instance().getAnnotationByAnnotationNo(annotationId.toInt(), entityManager)
//        }
//    }

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

    private class SupHandler(private val annotationMap: Map<Long, AnnotationData>) : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            if (element.hasAttribute("data-id")) {
                val marker: String = element.getAttribute("data-marker")
                val id: String = element.getAttribute("data-id")
                if (marker == "end") {
                    val key = id.toLong()
                    val annotationData = annotationMap[key]!!
                    val note = Element("note", mapOf("xml:id" to "note_$id", "type" to annotationData.type.name))
                    context.addOpenTag(note)
                    val annotationMetadataMap: Map<String, String> = annotationData.type.metadata
                    if (annotationMetadataMap.isNotEmpty()) {
                        context.addOpenTag(INTERP_GRP)
                        annotationMetadataMap.forEach { (type, value) ->
                            context.addEmptyElementTag(interpElement(type, value))
                        }
                        context.addCloseTag(INTERP_GRP)
                    }
                    val noteContent = annotationData.text.ifEmpty { annotationData.annotatedText }
                    val fixedContent = AnnotationBodyConverter.convert(noteContent)
                    context.addLiteral(fixedContent)
                    context.addCloseTag(note)
                }
                return STOP
            } else {
                val hi: Element = Element("hi").withAttribute("rend", TEIBuilder.HI_TAGS["sup"])
                context.addOpenTag(hi)
                openElements.push(hi)
                return NEXT
            }
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            if (!element.hasAttribute("data-id")) {
                context.addCloseTag(openElements.pop())
            }
            return NEXT
        }

        companion object {
            private fun interpElement(key: String, value: String): Element =
                Element(
                    "interp",
                    mapOf(
                        "type" to key,
                        "value" to value
                    )
                )
        }

    }

    class IgnoreCommentHandler : CommentHandler<XmlContext> {
        override fun visitComment(p0: Comment?, p1: XmlContext?): Traversal {
            return STOP
        }
    }

    companion object {
        private val openElements: Deque<Element> = ArrayDeque()

        private var linenum = 1
        private var skipNextNewline = false

        private const val TAG_LB = "lb"
        val INTERP_GRP: String = "interpGrp"
    }
}


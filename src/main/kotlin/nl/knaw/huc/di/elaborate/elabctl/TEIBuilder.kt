package nl.knaw.huc.di.elaborate.elabctl

import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

object TEIBuilder {

    val HI_TAGS: Map<String, String> = mapOf(
        "strong" to "bold",
        "b" to "bold",
        "u" to "underline",
        "em" to "italic",
        "i" to "italic",
        "sub" to "subscript",
        "sup" to "superscript"
    )

    fun Entry.toTEI(teiName: String): String {
        val printOptions = PrintOptions(
            singleLineTextElements = true,
            indent = "  ",
            useSelfClosingTags = true
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDateTime.now().format(formatter)
        return xml("TEI") {
            xmlns = "http:www.tei-c.org/ns/1.0"
            "teiHeader" {
                "fileDesc" {
                    "titelStmt" {
                        "title" {
                            -name
                        }
                    }
                    "publicationStmt" {
                        "publisher" {
                            -"Huygens Instituut"
                        }
                        "date" {
                            -currentDate
                        }
                    }
                }
            }
            if (facsimiles.isNotEmpty()) {
                "facsimile" {
                    facsimiles.forEachIndexed { i, facs ->
                        "graphic" {
                            attribute("url", "$teiName-${(i + 1).toString().padStart(2, '0')}.jp2")
                        }
                    }
                }
            }
            "text" {
                "interpGrp" {
                    metadata
                        .filter { it.value.isNotEmpty() }
                        .forEach {
                            "interp" {
                                attribute("type", it.field)
                                attribute("value", it.value)
                            }
                        }
                }
                "body" {
                    parallelTexts
                        .filter { it.value.text.isNotEmpty() }
//                        .onEach { logger.info { "\ntext=\"\"\"${it.value.text}\"\"\"\"" } }
                        .forEach { (layerName, textLayer) ->
                            val annotationMap = textLayer.annotationData.associateBy { it.n }
                            "div" {
                                attribute("type", layerName)
                                unsafeText(textLayer.text.transform(layerName, annotationMap))
                            }
                        }
                }
            }
        }.toString(printOptions = printOptions)
    }

    private fun String.transform(transcriptionType: String, annotationMap: Map<Long, AnnotationData>): String {
        val visitor = TranscriptionVisitor(annotationMap = annotationMap)
        val prepared = replace("<br>", "<br/>\n").replace("\u00A0", " ")
        val wrapped = wrapInXml(prepared)
        val doc = nl.knaw.huygens.tei.Document.createFromXml(wrapped, false)
        doc.accept(visitor)
        val result = visitor.context.result
        if (!isWellFormed(result)) {
            logger.error { "Bad XML in result:\n$result\n" }
            throw RuntimeException("Bad XML")
        }
        val transformed = unwrapFromXml(result)
        return transformed
    }

    private fun String.cleanup(): String {
        val xmlFromHtml = this.replace("<br>", "\n")
            .replace("&", "&amp;")
            .replace("<br>", "<br/>\n")
            .replace("<body>", "<div>")
            .replace("</body>", "</div>\n")
            .replace("\n", "<lb/>\n")
            .replace("<i></em></i>", "</em>")
            .replace("<em style=\"font-style: italic;\">", "<em>")
            .replace("<em></i></em>", "</i>")
            .replace("<em></em>", "")
            .replace("<i></i>", "")
            .replace(" style=\"font-size:.*?>", ">")
            .replace("\n", " ")
            .replace("<lb/>", "<lb/>\n")
//        logger.info { xmlFromHtml }
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val wrappedXml = "<xml>$xmlFromHtml</xml>"
        val doc = builder.parse(InputSource(StringReader(wrappedXml)))
        HI_TAGS.forEach { (oldTag, rendValue) ->
            doc.replaceElements(oldTag) { doc, _ ->
                doc.createElement("hi")
                    .apply {
                        mapOf("rend" to rendValue).forEach { (name, value) -> setAttribute(name, value) }
                    }
            }
        }
//        doc.replaceElements("span") { doc, element ->
//            val type = element.getAttribute("type")
//            val style = element.getAttribute("style")
//            val id = element.getAttribute("id")
//            if (id.isEmpty()) {
//                var e: Element? = null
//                if ("text-decoration: underline;" == style) {
//                    e = Element("hi", "rend", "underline")
//                } else if ("text-decoration: line-through;" == style) {
//                    e = Element("del", "rend", "strikethrough")
//                } else if ("font-style: italic;" == style) {
//                    e = Element("ex")
//                }
//            } else {
//                if (elaborate.editor.export.tei.TranscriptionVisitor.SpanHandler.isStartMilestone(id)) {
//                    if ("ex" == type) {
//                        val e: Element = Element("ex")
//                        context.addOpenTag(e)
//                        elaborate.editor.export.tei.TranscriptionVisitor.openElements.push(element)
//                    }
//                } else {
//                    context.addCloseTag(elaborate.editor.export.tei.TranscriptionVisitor.openElements.pop())
//                }
//            }
//
//            element
//        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc.firstChild), StreamResult(writer))
        val modifiedXmlString = writer.toString()
            .replace("""<?xml version="1.0" encoding="UTF-8"?><xml>""", "")
            .replace("</xml>", "")
        logger.info { modifiedXmlString }
        return modifiedXmlString
    }

    private fun Document.replaceElements(oldTag: String, elementReplacer: (Document, Element) -> Element) {
        val nodes = getElementsByTagName(oldTag)
        val toReplace = mutableListOf<Element>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element) {
                toReplace.add(node)
            }
        }

        for (node in toReplace) {
            val parent = node.parentNode
            val newElement = elementReplacer(this, node)

            while (node.hasChildNodes()) {
                newElement.appendChild(node.firstChild)
            }

            // Replace the old element with the new element
            parent.replaceChild(newElement, node)
        }
    }

    private fun Document.replaceElementsOld(oldTag: String, newTag: String, newAttributes: Map<String, String>) {
        val nodes = getElementsByTagName(oldTag)
        val toReplace1 = mutableListOf<Element>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element) {
                toReplace1.add(node)
            }
        }
        val toReplace = toReplace1

        for (node in toReplace) {
            val parent = node.parentNode
            val newElement = this.createElement(newTag)
                .apply {
                    newAttributes.forEach { (name, value) -> setAttribute(name, value) }
                }

            while (node.hasChildNodes()) {
                newElement.appendChild(node.firstChild)
            }

            // Replace the old element with the new element
            parent.replaceChild(newElement, node)
        }
    }

}
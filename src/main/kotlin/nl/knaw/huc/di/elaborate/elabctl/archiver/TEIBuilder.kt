package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import nl.knaw.huc.di.elaborate.elabctl.logger

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
                    facsimiles.forEachIndexed { i, _ ->
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
                                unsafeText(textLayer.text.transform(annotationMap))
                            }
                        }
                }
            }
        }.toString(printOptions = printOptions)
    }

    private fun String.transform(annotationMap: Map<Long, AnnotationData>): String {
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

}
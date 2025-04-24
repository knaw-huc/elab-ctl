package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.XmlVersion
import org.redundent.kotlin.xml.xml
import nl.knaw.huc.di.elaborate.elabctl.logger

object TEIBuilder {

    val HI_TAGS: Map<String, String> = mapOf(
        "strong" to "bold",
        "b" to "bold",
        "u" to "underline",
        "em" to "italics",
        "i" to "italics",
        "sub" to "subscript",
        "sup" to "super"
    )

    fun Entry.toTEI(teiName: String, projectName: String): String {
        val printOptions = PrintOptions(
            singleLineTextElements = false,
            indent = "  ",
            useSelfClosingTags = true
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDateTime.now().format(formatter)
        return xml("TEI") {
            processingInstruction("editem", Pair("template", "letter"))
            processingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-letter.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
            )
            processingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-letter.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
            )
            version = XmlVersion.V10
            encoding = "UTF-8"
            xmlns = "http://www.tei-c.org/ns/1.0"
            namespace("ed", "http://xmlschema.huygens.knaw.nl/ns/editem")
            "teiHeader" {
                "fileDesc" {
                    "titleStmt" {
                        "title" {
                            -name
                        }
                    }
                    "publicationStmt" {
                        "publisher" {
                            "name" {
                                attribute("ref", "https://huygens.knaw.nl")
                                -"Huygens Institute for the History and Cultures of the Netherlands (KNAW)"
                            }
                        }
                        "date" {
                            attribute("when", currentDate)
                            -currentDate
                        }
                        "ptr" {
                            attribute("target", "https://$projectName.huygens.knaw.nl/edition/entry/$id")
                        }
                    }
                    "sourceDesc" {
                        "msDesc" {
                            "msIdentifier" {
                                "country"
                                "settlement"
                                "institution"
                                "idno"
                            }
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
            metadata
                .filter { it.value.isNotEmpty() }
                .forEach {
                    comment("${it.field.asType()} = ${it.value}")
                }
            "text" {
                "body" {
                    parallelTexts
                        .filter { it.value.text.isNotEmpty() }
//                        .onEach { logger.info { "\ntext=\"\"\"${it.value.text}\"\"\"\"" } }
                        .forEach { (layerName, textLayer) ->
                            val annotationMap = textLayer.annotationData.associateBy { it.n }
                            val text = textLayer.text.transform(annotationMap)
                            "div" {
                                attribute("type", layerName)
                                if (text.contains("</p>")) {
                                    unsafeText(text)
                                } else {
                                    "p" { unsafeText(text) }
                                }
                            }
                        }
                }
            }
            "standOff" {
                "listAnnotation" {
                    attribute("type", "notes")


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
        return unwrapFromXml(result)
            .replace("\u00A0", " ")
            .replace(" </hi>", "</hi> ")
            .replace("</p>", "</p>\n")
    }

}
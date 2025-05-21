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

    fun Entry.toTEI(teiName: String, projectConfig: ProjectConfig): String {
        val printOptions = PrintOptions(
            singleLineTextElements = true,
            indent = "  ",
            useSelfClosingTags = true
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDateTime.now().format(formatter)
        val metadataMap = metadata.associate { it.field to it.value }
        val projectName = projectConfig.projectName

        return xml("TEI") {
            globalProcessingInstruction("editem", Pair("template", "letter"))
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-letter.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
            )
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-letter.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
            )
            version = XmlVersion.V10
            encoding = "UTF-8"
            xmlns = "http://www.tei-c.org/ns/1.0"
//            namespace("ed", "http://xmlschema.huygens.knaw.nl/ns/editem") // TODO: make conditional
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
                                "country" {}
                                "settlement" {}
                                "institution" {}
                                "idno" {}
                            }
                        }
                        "physDesc" {
                            "objectDesc" {
                                attribute("form", "letter")
                            }
                        }
                    }
                }
                "profileDesc" {
                    "correspDesc" {
                        val forwardDelimiter = "-->"
                        "correspAction" {
                            attribute("type", "sent")
                            (metadataMap["Afzender"] ?: "").split("/")
                                .forEach { sender ->
                                    val senderId = projectConfig.personIds[sender] ?: ""
                                    "rs" {
                                        if (senderId.isNotEmpty()) {
                                            attribute("ref", "bio.xml#$senderId")
                                        }
                                        attribute("type", "person")
                                        -sender
//                                metadataMap[metadataMapping["sender"]]
                                    }
                                }
                            "date" {
                                attribute("when", metadataMap["Datum"] ?: "")
                                -(metadataMap["Datum"] ?: "")
                            }
                            "placeName" {
                                -(metadataMap["Plaats"] ?: "")
                            }
                        }
                        val receiveString = metadataMap["Ontvanger"] ?: ""
                        val (firstReceivers, forwardReceivers) = receiveString.biSplit(forwardDelimiter)
                        "correspAction" {
                            attribute("type", "received")
                            val (personReceivers, orgReceivers) = firstReceivers.biSplit("#")
                            personReceivers.split("/")
                                .forEach { receiver ->
                                    val receiverId = projectConfig.personIds[receiver] ?: ""
                                    "rs" {
                                        if (receiverId.isNotEmpty()) {
                                            attribute("ref", "bio.xml#$receiverId")
                                        }
                                        attribute("type", "person")
                                        -receiver
                                    }
                                }
                            orgReceivers?.let {
                                it.split("/")
                                    .forEach { receiver ->
                                        "rs" {
                                            val orgId = ""
                                            if (orgId.isNotEmpty()) {
                                                attribute("ref", "orgs.xml#$orgId")
                                            }
                                            attribute("type", "org")
                                            -receiver
                                        }
                                    }
                            }
                        }
                        forwardReceivers?.let {
                            "correspAction" {
                                attribute("type", "received-after-forward")
                                val (personReceivers, orgReceivers) = forwardReceivers.biSplit("#")
                                personReceivers.split("/")
                                    .forEach { receiver ->
                                        val receiverId = projectConfig.personIds[receiver] ?: ""
                                        "rs" {
                                            if (receiverId.isNotEmpty()) {
                                                attribute("ref", "bio.xml#$receiverId")
                                            }
                                            attribute("type", "person")
                                            -receiver
                                        }
                                    }
                                orgReceivers?.let {
                                    it.split("/")
                                        .forEach { receiver ->
                                            "rs" {
                                                val orgId = ""
                                                if (orgId.isNotEmpty()) {
                                                    attribute("ref", "orgs.xml#$orgId")
                                                }
                                                attribute("type", "org")
                                                -receiver
                                            }
                                        }
                                }
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

    private fun String.biSplit(delimiter: String): Pair<String, String?> {
        val parts = split(delimiter)
        return if (parts.size == 1) {
            Pair(this, null)
        } else {
            Pair(parts[0], parts[1])
        }
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
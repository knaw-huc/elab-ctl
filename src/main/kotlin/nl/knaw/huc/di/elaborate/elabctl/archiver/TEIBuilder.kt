package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import arrow.atomic.AtomicInt
import org.redundent.kotlin.xml.Node
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
                                "institution" { }
                                "idno" { -(metadataMap["Signatuur"] ?: "") }
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
                        sentCorrespActionNode(projectConfig, metadataMap)

                        val receiveString = metadataMap["Ontvanger"] ?: ""
                        val (firstReceivers, forwardReceivers) = receiveString.biSplit("-->")
                        correspActionNode(projectConfig, "received", firstReceivers)
                        forwardReceivers?.let {
                            correspActionNode(projectConfig, "received-after-forward", forwardReceivers)
                        }
                    }
                }
            }
            if (facsimiles.isNotEmpty()) {
                "facsimile" {
                    facsimiles.forEachIndexed { i, facs ->
                        "surface" {
                            attribute("n", "${i + 1}")
                            attribute("xml:id", "s${i + 1}")
                            comment(facs.title)
                            "graphic" {
                                attribute("url", "$teiName-${(i + 1).toString().padStart(2, '0')}")
                            }
                        }
                    }
                }
            }
            metadata
                .filter { it.value.isNotEmpty() }
                .forEach {
                    comment("${it.field.asType()} = ${it.value}")
                }
            val annotationMap: MutableMap<Long, AnnotationData> = mutableMapOf()
            "text" {
                "body" {
                    parallelTexts
                        .filter { it.value.text.isNotEmpty() }
//                        .onEach { logger.info { "\ntext=\"\"\"${it.value.text}\"\"\"\"" } }
                        .forEach { (layerName, textLayer) ->
                            val lang = metadataMap["Taal"]?.asIsoLang() ?: "nl"
                            val divType = projectConfig.divTypeForLayerName[layerName] ?: "original"
                            val layerAnnotationMap = textLayer.annotationData.associateBy { it.n }
                            annotationMap.putAll(layerAnnotationMap)
                            val text = textLayer.text
                                .transform(layerAnnotationMap)
                                .removeLineBreaks()
                                .convertVerticalSpace()
                                .setParagraphs(divType, lang)
                                .setPageBreaks(divType, lang)
                                .wrapLines(80)
                            "div" {
                                attribute("type", divType)
                                attribute("xml:lang", lang)
                                -"\n"
                                if (text.contains("</p>")) {
                                    unsafeText(text)
                                } else {
                                    "p" {
                                        attribute("xml:id", "p.$divType.$lang.1")
                                        unsafeText(text)
                                    }
                                }
                            }
                        }
                }
            }
            if (annotationMap.isNotEmpty()) {
                val noteCounter = AtomicInt(1)
                "standOff" {
                    "listAnnotation" {
                        attribute("type", "notes")
                        annotationMap.forEach { id, data ->
                            "note" {
                                attribute("xml:id", "note_$id")
                                attribute("n", noteCounter.andIncrement)
                                comment("${data.type.name} / ${data.type.description}")
                                "p" { -data.text }
                            }
                        }

                    }
                }
            }
        }.toString(printOptions = printOptions)
    }

    private fun Node.correspActionNode(
        projectConfig: ProjectConfig,
        type: String,
        correspondentString: String
    ) {
        val (personReceivers, orgReceivers) = correspondentString.biSplit("#")
        "correspAction" {
            attribute("type", type)
            personReceivers.split("/")
                .forEach { personRsNode(projectConfig, it) }
            orgReceivers?.let {
                it.split("/").forEach { orgRsNode(it) }
            }
        }
    }

    private fun Node.sentCorrespActionNode(
        projectConfig: ProjectConfig,
        metadataMap: Map<String, String>
    ) {
        val senders = (metadataMap["Afzender"] ?: "").split("/")
        val date = metadataMap["Datum"] ?: ""
        val place = metadataMap["Plaats"] ?: ""
        "correspAction" {
            attribute("type", "sent")
            senders
                .forEach { sender ->
                    val (person, org) = sender.biSplit("#")
                    personRsNode(projectConfig, person)
                    org?.let { orgRsNode(org) }
                }
            "date" {
                attribute("when", date)
                -date
            }
            "placeName" {
                -place
            }
        }
    }

    private fun Node.personRsNode(
        projectConfig: ProjectConfig,
        personName: String
    ) {
        val personId = projectConfig.personIds[personName] ?: ""
        "rs" {
            attribute("type", "person")
            if (personId.isNotEmpty()) {
                attribute("ref", "bio.xml#$personId")
            }
            -personName
        }
    }

    private fun Node.orgRsNode(org: String) {
        val orgId = ""
        "rs" {
            attribute("type", "org")
            if (orgId.isNotEmpty()) {
                attribute("ref", "orgs.xml#$orgId")
            }
            -org
        }
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
        val wrapped = this
            .replace("\u00A0", " ")
            .replaceWhileFound(" <br>", "<br>")

            .replaceWhileFound("<b><br>", "<br><b>")
            .replaceWhileFound("<br></b>", "</b><br>")
            .replaceWhileFound("<b> ", " <b>")
            .replaceWhileFound(" </b>", "</b> ")
            .replaceWhileFound("<b></b>", "")

            .replaceWhileFound("<u><br>", "<br><u>")
            .replaceWhileFound("<br></u>", "</u><br>")
            .replaceWhileFound("<u> ", " <u>")
            .replaceWhileFound(" </u>", "</u> ")
            .replaceWhileFound("<u></u>", "")

            .replaceWhileFound("<i><br>", "<br><i>")
            .replaceWhileFound("<br></i>", "</i><br>")
            .replaceWhileFound("<i> ", " <i>")
            .replaceWhileFound(" </i>", "</i> ")
            .replaceWhileFound("<i></i>", "")

            .replaceWhileFound("<sup><br>", "<br><sup>")
            .replaceWhileFound("<br></sup>", "</sup><br>")
            .replaceWhileFound("<sup> ", " <sup>")
            .replaceWhileFound(" </sup>", "</sup> ")
            .replaceWhileFound("<sup></sup>", "")

            .replaceWhileFound("<b><b>¶</b><br>", "<b>¶</b><br><b>")
            .replaceWhileFound("<b><b>¶</b></b>", "<b>¶</b>")

            .replace("<br>", "<br/>\n")
            .trim()
            .wrapInXml()
        val doc = nl.knaw.huygens.tei.Document.createFromXml(wrapped, false)
        doc.accept(visitor)
        val result = visitor.context.result
        if (!result.isWellFormed()) {
            logger.error { "Bad XML in result:\n$result\n" }
            throw RuntimeException("Bad XML")
        }
        return result.unwrapFromXml()
            .replace("\u00A0", " ")
            .replace(" </hi>", "</hi> ")
            .replace("</p>", "</p>\n")
    }

    private fun String.replaceWhileFound(oldValue: String, newValue: String): String {
        var string = this
        while (string.contains(oldValue)) {
            string = string.replace(oldValue, newValue)
        }
        return string
    }

    private fun String.asIsoLang() =
        when {
            contains("Nederlands") -> "nl"
            contains("Duits") -> "ge"
            contains("Engels") -> "en"
            contains("Frans") -> "fr"
            else -> "nl"
        }

    private fun String.removeLineBreaks(): String =
        this.replace(Regex("<lb n=\"\\d+\"/>\n"), "")

    private fun String.convertVerticalSpace(): String =
        this.replace(Regex("\n\\s*\n"), "\n<space dim=\"vertical\" unit=\"lines\" quantity=\"1\"/>\n")

    private fun String.setParagraphs(divType: String, lang: String): String {
        val paraCounter = AtomicInt(1)
        return this.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") {
                if (it.contains("<space ") || it == """<hi rend="bold">¶</hi>""") {
                    it
                } else {
                    val n = paraCounter.andIncrement
                    val indent = if (it.startsWith(" ")) {
                        " rend=\"indent\""
                    } else {
                        ""
                    }
                    "<p xml:id=\"p.$divType.$lang.$n\" n=\"$n\"$indent>${it.trim()}</p>"
                }
            }
    }

    private fun String.setPageBreaks(divType: String, lang: String): String =
        this.replace("""<hi rend="bold"><hi rend="bold">¶</hi></hi>""", """<hi rend="bold">¶</hi>""")
            .split("""<hi rend="bold">¶</hi>""")
            .mapIndexed { i, t ->
                if (i == 0) {
                    t
                } else {
                    "\n<pb xml:id=\"pb.$divType.$lang.$i\" f=\"$i\" facs=\"#s$i\" n=\"$i\"/>\n$t"
                }
            }
            .joinToString("")

    private fun String.wrapLines(width: Int): String {
        val result = StringBuilder()
        this.trim()
            .split("\n")
            .forEach { line ->
                var currentLineLength = 0
                line.split(" ")
                    .forEach { word ->
                        if (currentLineLength + word.length > width) {
                            result.append("\n")
                            currentLineLength = 0
                        } else if (currentLineLength > 0) {
                            result.append(" ")
                            currentLineLength++
                        }
                        result.append(word)
                        currentLineLength += word.length
                    }
                result.append("\n")
            }
        return result.toString().trim()
    }

}
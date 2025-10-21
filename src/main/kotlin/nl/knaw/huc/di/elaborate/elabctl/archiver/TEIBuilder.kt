package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import arrow.atomic.AtomicInt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.XmlVersion
import org.redundent.kotlin.xml.xml
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.config.ElabCtlConfig
import nl.knaw.huc.di.elaborate.elabctl.config.LetterMetadataConfig
import nl.knaw.huc.di.elaborate.elabctl.config.PageBreakEncoding
import nl.knaw.huc.di.elaborate.elabctl.logger
import nl.knaw.huygens.tei.Document

@OptIn(ExperimentalSerializationApi::class)
class TEIBuilder(val projectConfig: ProjectConfig, val conversionConfig: ElabCtlConfig) {
    val annoNumToRefTarget: Map<String, String> by lazy { loadAnnoNumToRefTarget(conversionConfig.annoNumToRefTarget) }

    val dateAttributeFactory = conversionConfig.letterDates?.let { DateAttributeFactory(it) }

    val printOptions = PrintOptions(
        singleLineTextElements = true,
        indent = "  ",
        useSelfClosingTags = true
    )

    fun entryToTEI(
        entry: Entry,
        teiName: String
    ): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDateTime.now().format(formatter)
        val metadataMap = entry.metadata.associate { it.field to it.value }
        val projectName = projectConfig.projectName
        val title = entry.name
        val editorName = conversionConfig.editor.name
        val editorId = conversionConfig.editor.id
        val editorUrl = conversionConfig.editor.url

        val letterMetadata = conversionConfig.letterMetadata!!
        return xml("TEI") {
            prologNodes("letter")
            xmlns = "http://www.tei-c.org/ns/1.0"
//            namespace("ed", "http://xmlschema.huygens.knaw.nl/ns/editem") // TODO: make conditional
            teiHeaderNode(
                entry,
                title,
                editorId,
                editorName,
                editorUrl,
                currentDate,
                projectName,
                metadataMap,
                letterMetadata
            )
            facsimileNode(listOf(entry), teiName)
            metadataCommentNodes(entry)
            val annotationMap: MutableMap<Long, AnnotationData> = textNode(entry, metadataMap, letterMetadata)
            standOffNode(annotationMap)
        }.toString(printOptions = printOptions)
    }

    fun manuscriptToTEI(entries: List<Entry>, projectName: String): String {
        val entriesPerChapter = entries.groupBy { it.metadata.asMap()["Hoofdstuknummer"]!! }
        return xml("TEI") {
            prologNodes("medieval-manuscript")
            xmlns = "http://www.tei-c.org/ns/1.0"
            "teiHeader" {
//                "fileDesc" {
//                    "titleStmt" {
//                        "title" {
//                            comment(entry.name)
//                            -title
//                        }
//                        "editor" {
//                            attribute("xml:id", editorId)
//                            -editorName
//                            comment(editorUrl)
//                        }
//                    }
//                    "publicationStmt" {
//                        "publisher" {
//                            "name" {
//                                attribute("ref", "https://huygens.knaw.nl")
//                                -"Huygens Institute for the History and Cultures of the Netherlands (KNAW)"
//                            }
//                        }
//                        "date" {
//                            attribute("when", currentDate)
//                            -currentDate
//                        }
//                        "ptr" {
//                            attribute("target", "https://$projectName.huygens.knaw.nl/edition/entry/${entry.id}")
//                        }
//                    }
//                    "sourceDesc" {
//                        "msDesc" {
//                            "msIdentifier" {
//                                "country" {}
//                                "settlement" { metadataMap[letterMetadata.settlement] ?: "" }
//                                "institution" { metadataMap[letterMetadata.institution] ?: "" }
////                                "repository" { }
////                                { "collection" { -(metadataMap[conversionConfig.letterMetadata.collection] ?: "") } }
//                                "idno" { -(metadataMap[letterMetadata.idno] ?: "") }
//                            }
//                            "physDesc" {
//                                "objectDesc" {
//                                    attribute("form", "letter")
//                                }
//                            }
//                        }
//                    }
//                }
//                "profileDesc" {
//                    "correspDesc" {
//                        sentCorrespActionNode(metadataMap)
//
//                        val receiveString = metadataMap[letterMetadata.recipient] ?: ""
//                        val (firstReceivers, forwardReceivers) = receiveString.biSplit("-->")
//                        correspActionNode(
//                            "received",
//                            firstReceivers,
//                            metadataMap[letterMetadata.recipientPlace]
//                        )
//                        forwardReceivers?.let {
//                            correspActionNode(
//                                "received",
//                                forwardReceivers,
//                                metadataMap[letterMetadata.recipientPlace]
//                            )
//                        }
//                    }
//                }
            }
            facsimileNode(entries, projectName)
            "text" {
                attribute("xml:id", "og")
                "body" {
                    attribute("divRole", "original-translation")
                    "div" {
                        val entryCounter = AtomicInt(1)
                        attribute("xml:lang", "mhg")
                        attribute("xml:id", "og-mhg")
                        attribute("type", "original")
                        entriesPerChapter.forEach { (chapter, entries) ->
                            "div" {
                                attribute("xml:id", "og-mhg-$chapter")
                                attribute("n", chapter)
                                entries.forEach { entry ->
                                    val entryMetadata = entry.metadata.asMap()
                                    val folioNr = entryMetadata["Folionummer"]!!
                                    "pb" {
                                        attribute("xml:id", "pb-mgh-$folioNr")
                                        attribute("facs", "#s${entryCounter.getAndIncrement()}")
                                        attribute("n", folioNr)
                                    }
                                    metadataCommentNodes(entry)
                                }
                            }
                        }
                    }
                    "div" {
                        attribute("xml:lang", "dum")
                        attribute("xml:id", "og-dum")
                        attribute("type", "translation")
                    }
                    "div" {
                        attribute("xml:lang", "de")
                        attribute("xml:id", "og-de")
                        attribute("type", "translation-unaligned")
                    }
                }
            }

        }.toString(printOptions = printOptions)
    }

    private fun loadAnnoNumToRefTarget(annoNumToRefTargetPath: String?): Map<String, String> {
        return if (annoNumToRefTargetPath == null) {
            mapOf()
        } else {
            logger.info { "<= $annoNumToRefTargetPath" }
            val input = Path(annoNumToRefTargetPath).inputStream()
            json.decodeFromStream(input)
        }
    }

    private fun Node.teiHeaderNode(
        entry: Entry,
        title: String,
        editorId: String,
        editorName: String,
        editorUrl: String,
        currentDate: String,
        projectName: String,
        metadataMap: Map<String, String>,
        letterMetadata: LetterMetadataConfig
    ) {
        "teiHeader" {
            fileDesc(
                entry,
                title,
                editorId,
                editorName,
                editorUrl,
                currentDate,
                projectName,
                metadataMap,
                letterMetadata
            )
            profileDesc(metadataMap, letterMetadata)
        }
    }

    private fun Node.fileDesc(
        entry: Entry,
        title: String,
        editorId: String,
        editorName: String,
        editorUrl: String,
        currentDate: String,
        projectName: String,
        metadataMap: Map<String, String>,
        letterMetadata: LetterMetadataConfig
    ) {
        "fileDesc" {
            "titleStmt" {
                "title" {
                    comment(entry.name)
                    -title
                }
                "editor" {
                    attribute("xml:id", editorId)
                    -editorName
                    comment(editorUrl)
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
                    attribute("target", "https://$projectName.huygens.knaw.nl/edition/entry/${entry.id}")
                }
            }
            "sourceDesc" {
                "msDesc" {
                    "msIdentifier" {
                        "country" {}
                        "settlement" { metadataMap[letterMetadata.settlement] ?: "" }
                        "institution" { metadataMap[letterMetadata.institution] ?: "" }
                        //                                "repository" { }
                        //                                { "collection" { -(metadataMap[conversionConfig.letterMetadata.collection] ?: "") } }
                        "idno" { -(metadataMap[letterMetadata.idno] ?: "") }
                    }
                    "physDesc" {
                        "objectDesc" {
                            attribute("form", "letter")
                        }
                    }
                }
            }
        }
    }

    private fun Node.profileDesc(
        metadataMap: Map<String, String>,
        letterMetadata: LetterMetadataConfig
    ) {
        "profileDesc" {
            "correspDesc" {
                sentCorrespActionNode(metadataMap)

                val receiveString = metadataMap[letterMetadata.recipient] ?: ""
                val (firstReceivers, forwardReceivers) = receiveString.biSplit("-->")
                correspActionNode(
                    "received",
                    firstReceivers,
                    metadataMap[letterMetadata.recipientPlace]
                )
                forwardReceivers?.let {
                    correspActionNode(
                        "received",
                        forwardReceivers,
                        metadataMap[letterMetadata.recipientPlace]
                    )
                }
            }
        }
    }

    private fun Node.facsimileNode(
        entries: List<Entry>,
        baseName: String
    ) {
        val facsimiles = entries.flatMap { it.facsimiles }
        if (facsimiles.isNotEmpty()) {
            "facsimile" {
                facsimiles.forEachIndexed { i, facs ->
                    "surface" {
                        attribute("n", "${i + 1}")
                        attribute("xml:id", "s${i + 1}")
                        if (facs.title.isNotEmpty() && facs.title != "facsimile") {
                            comment(facs.title)
                        }
                        "graphic" {
                            attribute("url", "$baseName-${(i + 1).toString().padStart(2, '0')}")
                        }
                    }
                }
            }
        }
    }

    private fun Node.metadataCommentNodes(entry: Entry) {
        entry.metadata
            .filter { it.value.isNotEmpty() }
            .forEach { comment("${it.field} = ${it.value}") }
    }

    private fun Node.textNode(
        entry: Entry,
        metadataMap: Map<String, String>,
        letterMetadata: LetterMetadataConfig
    ): MutableMap<Long, AnnotationData> {
        val annotationMap: MutableMap<Long, AnnotationData> = mutableMapOf()
        "text" {
            "body" {
                attribute("divRole", conversionConfig.divRole)
                entry.parallelTexts
                    .filter { it.value.text.isNotEmpty() }
                    .toSortedMap()
                    //                        .onEach { logger.info { "\ntext=\"\"\"${it.value.text}\"\"\"\"" } }
                    .forEach { (layerName, textLayer) ->
                        val divType = projectConfig.divTypeForLayerName[layerName] ?: layerName.lowercase()
                        val lang = when {
                            (divType == "translation") -> "nl"
                            else -> (metadataMap[letterMetadata.language])?.asIsoLang() ?: "nl"
                        }
                        val layerAnnotationMap = textLayer.annotationData.associateBy { it.n }
                        annotationMap.putAll(layerAnnotationMap.filter { !annoNumToRefTarget.contains(it.key.toString()) })
                        val text = textLayer.text
                            .transform(layerAnnotationMap, annoNumToRefTarget)
                            .removeLineBreaks()
                            .convertVerticalSpace()
                            .convertHorizontalSpace()
                            .setParagraphs(divType, lang)
                            .setPageBreaks(divType, lang, conversionConfig.pageBreakEncoding)
                            //                                .wrapLines(80)
                            .wrapSpaceElementWithNewLines()
                            .replace("\n\n\n", "\n\n")
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
        return annotationMap
    }

    private fun Node.standOffNode(annotationMap: MutableMap<Long, AnnotationData>) {
        if (annotationMap.isNotEmpty()) {
            val noteCounter = AtomicInt(1)
            "standOff" {
                "listAnnotation" {
                    attribute("type", "notes")
                    annotationMap.forEach { (id, data) ->
                        val noteText = data.text.ifEmpty { data.annotatedText }
                        "note" {
                            attribute("xml:id", "note_$id")
                            attribute("n", noteCounter.andIncrement)
                            comment("${data.type.name} / ${data.type.description} / ${data.type.metadata.entries}")
                            "p" { -noteText }
                        }
                    }
                }
            }
        }
    }

    private fun Node.prologNodes(projectType: String) {
        globalProcessingInstruction("editem", Pair("template", projectType))
        globalProcessingInstruction(
            "xml-model",
            Pair("href", "http://xmlschema.huygens.knaw.nl/editem-$projectType.rng"),
            Pair("type", "application/xml"),
            Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
        )
        globalProcessingInstruction(
            "xml-model",
            Pair("href", "http://xmlschema.huygens.knaw.nl/editem-$projectType.rng"),
            Pair("type", "application/xml"),
            Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
        )
        version = XmlVersion.V10
        encoding = "UTF-8"
    }

    private fun Node.correspActionNode(
        type: String,
        correspondentString: String,
        recipientPlace: String?
    ) {
        val (personReceivers, orgReceivers) = correspondentString.biSplit("#")
        "correspAction" {
            attribute("type", type)
            personReceivers.split("/")
                .forEach { personRsNode(it) }
            orgReceivers?.let {
                it.split("/").forEach { org -> orgRsNode(org) }
            }
            recipientPlace?.let { place ->
                "placeName" {
                    -place
                }
            }
        }
    }

    private fun Node.sentCorrespActionNode(
        metadataMap: Map<String, String>
    ) {
        val letterMetadata = conversionConfig.letterMetadata!!
        val senders = (metadataMap[letterMetadata.sender] ?: "").split("/")
        val date = metadataMap[letterMetadata.date] ?: ""
        val place =
            metadataMap[letterMetadata.senderPlace] ?: ""
        "correspAction" {
            attribute("type", "sent")
            senders
                .forEach { sender ->
                    val (person, org) = sender.biSplit("#")
                    personRsNode(person)
                    org?.let { orgRsNode(org) }
                }
            "date" {
                dateAttributeFactory?.getDateAttributes(date)?.forEach {
                    attribute(it.key, it.value)
                }
                -date
            }
            "placeName" {
                -place
            }
        }
    }

    private fun Node.personRsNode(
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

    private fun String.transform(
        annotationMap: Map<Long, AnnotationData>,
        annoNumToRefTarget: Map<String, String>
    ): String {
        val visitor = TranscriptionVisitor(annotationMap = annotationMap, annoNumToRefTarget)
        val wrapped = this
            .replace("\u00A0", " ")
            .replace("&nbsp;", "<nbsp/>")
            .replace(Regex(" +"), " ")

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
            .replace("<br><b>¶</b>", "<br><b>¶</b><br>")

            .replace("<br>", "<br/>\n")
            .trim()
            .wrapInXml()
        val doc = Document.createFromXml(wrapped, false)
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
            .replace("<nbsp></nbsp>", "<nbsp/>")
            .replace(" <nbsp/>", "<nbsp/><nbsp/>")
            .replace("<nbsp/> ", "<nbsp/><nbsp/>")
    }

    private fun String.setParagraphs(divType: String, lang: String): String {
        val visitor = ParagraphVisitor(divType, lang)
        val xml = this.wrapInXml()
        Document.createFromXml(xml, false)
            .accept(visitor)
        return visitor.context.result.unwrapFromXml()
    }

//    private fun String.setParagraphs0(divType: String, lang: String): String {
//        val paraCounter = AtomicInt(1)
//        return this.split("\n")
//            .filter { it.isNotBlank() }
//            .joinToString("\n") {
//                if (it.startsWith("<space ") || it == ENCODED_PAGE_BREAK) {
//                    it
//                } else {
//                    val n = paraCounter.andIncrement
//                    val indent = if (it.startsWith(" ")) {
//                        " rend=\"indent\""
//                    } else {
//                        ""
//                    }
//                    "<p xml:id=\"p.$divType.$lang.$n\" n=\"$n\"$indent>${it.trim()}</p>"
//                }
//            }
//    }

    private fun String.setPageBreaks(divType: String, lang: String, pageBreakEncoding: PageBreakEncoding): String =
        when (pageBreakEncoding) {
            PageBreakEncoding.PILCROW -> this
                .replace("""<hi rend="bold">$ENCODED_PAGE_BREAK</hi>""", ENCODED_PAGE_BREAK)
                .split(ENCODED_PAGE_BREAK)
                .mapIndexed { i, t ->
                    if (i == 0) {
                        t
                    } else {
                        "\n<pb xml:id=\"pb.$divType.$lang.$i\" f=\"$i\" facs=\"#s$i\" n=\"$i\"/>\n$t"
                    }
                }
                .joinToString("")

            PageBreakEncoding.PAGE_BREAK_MARKER -> {
                var str = this
                if (!this.contains("[1]")) {
                    str = "[1]$str"
                }
                str.addPageBreaks(divType, lang)
            }

            PageBreakEncoding.NONE -> this
        }

    val pbRegex = Regex("\\[(\\d+)]")
    fun String.addPageBreaks(divType: String, lang: String): String =
        pbRegex.replace(this) { matchResult ->
            val number = matchResult.groupValues[1]
            "<pb xml:id=\"pb.$divType.$lang.$number\" f=\"$number\" facs=\"#s$number\" n=\"$number\"/>"
        }

    companion object {
        const val SPACE_ELEMENT_LINE = "\n<space dim=\"vertical\" unit=\"lines\" quantity=\"1\"/>\n"
        const val ENCODED_PAGE_BREAK = """<hi rend="bold">¶</hi>"""
        val HI_TAGS: Map<String, String> = mapOf(
            "strong" to "bold",
            "center" to "center",
            "b" to "bold",
            "u" to "underline",
            "em" to "italics",
            "i" to "italics",
            "sub" to "sub",
            "sup" to "super"
        )

        fun horizontalSpaceTag(quantity: Int): String =
            "<space dim=\"horizontal\" unit=\"chars\" quantity=\"$quantity\"/>"

        val regex = "(?:<nbsp/>)+".toRegex()

        //    fun String.convertHorizontalSpace(): String =
        //        this.replace("<nbsp/>", " ")
        fun String.convertHorizontalSpace(): String =
            regex.replace(this) { matchResult ->
                val count = matchResult.value.length / "<nbsp/>".length
                horizontalSpaceTag(count)
            }

        private fun String.removeLineBreaks(): String =
            this.replace(Regex("<lb n=\"\\d+\"/>\n"), "")

        private fun String.convertVerticalSpace(): String =
            this.replace(Regex("\n\\s*\n"), SPACE_ELEMENT_LINE)

        private fun String.wrapSpaceElementWithNewLines(): String =
            this.replace(
                "\n<space dim=\"vertical\" unit=\"lines\" quantity=\"1\"/></p>",
                "</p>\n<space dim=\"vertical\" unit=\"lines\" quantity=\"1\"/>"
            ).replace(
                SPACE_ELEMENT_LINE, "\n$SPACE_ELEMENT_LINE\n"
            )

        private fun String.biSplit(delimiter: String): Pair<String, String?> {
            val parts = split(delimiter)
            return if (parts.size == 1) {
                Pair(this, null)
            } else {
                Pair(parts[0], parts[1])
            }
        }

        private fun String.replaceWhileFound(oldValue: String, newValue: String): String {
            var string = this
            while (string.contains(oldValue)) {
                string = string.replace(oldValue, newValue)
            }
            return string
        }

        fun String.wrapLines(width: Int): String {
            val result = StringBuilder()
            this.trim()
                .split("\n")
                .forEach { line ->
                    var currentLineLength = 0
                    line.split(" ")
                        .forEach { word ->
                            if (currentLineLength + word.length >= width) {
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

        private fun ArrayList<Metadata>.asMap(): Map<String, String> =
            this.associate { it.field to it.value }
    }

}


package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import arrow.atomic.AtomicInt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.redundent.kotlin.xml.Namespace
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
        teiName: String,
        facsimileCounter: AtomicInteger,
        divCounter: AtomicInteger,
        sectionId: Int
    ): Pair<String, Archiver.XIRefs> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = LocalDateTime.now().format(formatter)
        val metadataMap = entry.metadata.associate { it.field to it.value }
        val projectName = projectConfig.projectName
        val title = entry.name
        val editorName = conversionConfig.editor.name
        val editorId = conversionConfig.editor.id
        val editorUrl = conversionConfig.editor.url

        val letterMetadata = conversionConfig.letterMetadata!!
        val startFacsCount = facsimileCounter.get()
        var listAnnotationRefs = emptyList<XIncludeRef>()
        val tei = xml("TEI") {
            prologNodes("book")
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
            facsimileNode(listOf(entry), teiName, facsimileCounter, sectionId)
            metadataCommentNodes(entry)
            val annotationMap: MutableMap<Long, AnnotationData> = textNode(
                entry,
                metadataMap,
                letterMetadata,
                sectionId
            )
            if (annotationMap.isNotEmpty()) {
                listAnnotationRefs = listOf(XIncludeRef("", "listannotation.$sectionId"))
            }
            standOffNode(annotationMap, sectionId)
        }.toString(printOptions = printOptions)
        val surfaceGrpRefs = if (startFacsCount == facsimileCounter.get()) emptyList() else {
            listOf(XIncludeRef("", "surfacegrp.$sectionId"))
        }
        val divRefs = if (entry.parallelTexts.any { it.value.text.isNotEmpty() }) {
            listOf(XIncludeRef("", "div.$sectionId"))
        } else {
            emptyList()
        }

        val xiRefs = Archiver.XIRefs(
            surfaceGrpRefs,
            divRefs,
            listAnnotationRefs
        )
        return Pair(tei, xiRefs)
    }

    fun manuscriptToTEI(entries: List<Entry>, projectName: String): String {
        val entryCounter = AtomicInt(1)
        val manuscriptEntries = entries
            .map { it.toManuscriptEntry("s" + entryCounter.getAndIncrement()) }
            .flatMap { it.splitOnChapterHeading() }
        val entriesPerChapter = entries.groupBy { it.metadata.asMap()["Bladzijde(n)"]!!.replace(" ", "") }
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
            facsimileNode(entries, projectName, AtomicInteger(1), 1)
            "text" {
                attribute("xml:id", "og")
                "body" {
                    attribute("divRole", "original")
                    manuscriptOriginalDivNode(entriesPerChapter)
//                    manuscriptTranslationDivNode(entriesPerChapter)
//                    manuscriptTranslationUnalignedDivNode(entriesPerChapter)
                }
            }

        }.toString(printOptions = printOptions)
    }

    fun bookToTEI(entries: List<Entry>, projectName: String): String =
        xml("TEI") {
            prologNodes("book")
            xmlns = "http://www.tei-c.org/ns/1.0"
            "teiHeader" {
                "fileDesc" {
                    "titleStmt" {
//                        "title" {
//                            -title
//                        }
//                        "editor" {
//                            attribute("xml:id", editorId)
//                            -editorName
//                            comment(editorUrl)
//                        }
                    }
                    "publicationStmt" {
                        "publisher" {
                            "name" {
                                attribute("ref", "https://huygens.knaw.nl")
                                -"Huygens Institute for the History and Cultures of the Netherlands (KNAW)"
                            }
                        }
//                        "date" {
//                            attribute("when", currentDate)
//                            -currentDate
//                        }
                        "ptr" {
                            attribute("target", "https://$projectName.huygens.knaw.nl/edition")
                        }
                    }
                    "sourceDesc" {
                        "msDesc" {
                            "msIdentifier" {
                                "country" {}
//                                "settlement" { metadataMap[letterMetadata.settlement] ?: "" }
//                                "institution" { metadataMap[letterMetadata.institution] ?: "" }
//                                "repository" { }
//                                { "collection" { -(metadataMap[conversionConfig.letterMetadata.collection] ?: "") } }
//                                "idno" { -(metadataMap[letterMetadata.idno] ?: "") }
                            }
                            "physDesc" {
                                "objectDesc" {
                                    attribute("form", "book")
                                }
                            }
                        }
                    }
                }
            }
            facsimileNode(entries, projectName, AtomicInteger(1), 1)
            "text" {
                attribute("xml:id", "og")
                "body" {
                    attribute("divRole", "doc-sections")
                    bookDivNode(entries, "nl")
                }
            }
        }.toString(printOptions = printOptions)

    fun bookMainToTEI(
        projectName: String,
        xiRefs: Archiver.XIRefs,
    ): String {
        val title = conversionConfig.title
        val editorName = conversionConfig.editor.name
        val editorId = conversionConfig.editor.id
        val editorUrl = conversionConfig.editor.url
        val xiNamespace = Namespace("xi", "http://www.w3.org/2001/XInclude")

        return xml("TEI") {
            prologNodes("book")
            xmlns = "http://www.tei-c.org/ns/1.0"
            namespace(xiNamespace)

            "teiHeader" {
                "fileDesc" {
                    "titleStmt" {
                        "title" {
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
                        //                        "date" {
                        //                            attribute("when", currentDate)
                        //                            -currentDate
                        //                        }
                        "ptr" {
                            attribute("target", "https://$projectName.huygens.knaw.nl/edition")
                        }
                    }
                    "sourceDesc" {
                        "msDesc" {
                            "msIdentifier" {
                                "country" {}
                                //                                "settlement" { metadataMap[letterMetadata.settlement] ?: "" }
                                //                                "institution" { metadataMap[letterMetadata.institution] ?: "" }
                                //                                "repository" { }
                                //                                { "collection" { -(metadataMap[conversionConfig.letterMetadata.collection] ?: "") } }
                                //                                "idno" { -(metadataMap[letterMetadata.idno] ?: "") }
                            }
                            "physDesc" {
                                "objectDesc" {
                                    attribute("form", "book")
                                }
                            }
                        }
                    }
                }
            }
            "facsimile" { xIncludes(xiRefs.surfaceGrpRefs) }
            "text" {
                attribute("xml:id", "og")
                "body" {
                    attribute("divRole", "doc-sections")
                    xIncludes(xiRefs.divRefs)
                }
            }
            "standOff" {
//                "listAnnotation" {
//                    attribute("type", "notes")
                xIncludes(xiRefs.listAnnotationRefs)
//                }
            }
        }.toString(printOptions = printOptions)
    }

    private fun Node.xIncludes(xIncludeRefs: List<XIncludeRef>) {
        xIncludeRefs.forEach {
            "xi:include" {
                attribute("href", it.href)
                attribute("xpointer", it.xpointer)
                "xi:fallback" {
                    comment("including ${it.href}#${it.xpointer} failed")
                }
            }
        }
    }

    data class XIncludeRef(
        val href: String,
        val xpointer: String,
    )

    data class ManuscriptEntry(
        val id: String,
        val folioNr: String,
        val facsRef: String,
        val originalLines: List<String>,
        val translationLines: List<String>,
        val translationUnalignedParagraphs: List<String>,
    )

    fun Entry.toManuscriptEntry(facsRef: String): ManuscriptEntry {
        val metadata = metadata.asMap()
        logger.info { metadata }
        val folioNr = metadata["Bladzijde(n)"]!!
        val originalLines = parallelTexts["Diplomatic"]!!.text.split("<br>")
        val translationLines = parallelTexts["Reconstructie"]!!.text.split("<br>")
        val translationUnalignedParagraphs = parallelTexts["Vertaling"]!!.text.split("<br><br>")
        return ManuscriptEntry(
            id.toString(),
            folioNr,
            facsRef,
            originalLines,
            translationLines,
            translationUnalignedParagraphs
        )
    }

    private fun ManuscriptEntry.splitOnChapterHeading(): List<ManuscriptEntry> {
        val predicate = { s: String -> s.contains("Hoofdstuknummer") }
        val segmentedOriginalLines = originalLines.splitOn(predicate)
        val segmentedTranslationLines = translationLines.splitOn(predicate)
        val segmentedTranslationUnalignedParagraphs = translationLines.splitOn(predicate)
        assert(segmentedOriginalLines.size == segmentedTranslationLines.size)
        assert(segmentedTranslationLines.size == segmentedTranslationUnalignedParagraphs.size)
        return IntRange(1, segmentedTranslationLines.size).map { i ->
            copy(
                originalLines = segmentedOriginalLines[i],
                translationLines = segmentedTranslationLines[i],
                translationUnalignedParagraphs = segmentedTranslationUnalignedParagraphs[i]
            )
        }
    }

    private fun Node.manuscriptOriginalDivNode(entriesPerChapter: Map<String, List<Entry>>) {
        val entryCounter = AtomicInt(1)
        val lineCounter = AtomicInt(1)
        "div" {
            attribute("xml:lang", "mhg")
            attribute("xml:id", "og-mhg")
            attribute("type", "original")
            entriesPerChapter.forEach { (chapter, entries) ->
                "div" {
                    attribute("xml:id", "og-mhg-$chapter")
                    attribute("n", chapter)
                    entries.forEach { entry ->
                        val entryMetadata = entry.metadata.asMap()
                        val folioNr = entryMetadata["Bladzijde(n)"]!!
                        "pb" {
                            attribute("xml:id", "pb-mgh-$folioNr")
                            attribute("facs", "#s${entryCounter.getAndIncrement()}")
                            attribute("n", folioNr)
                        }
                        metadataCommentNodes(entry)
                        val textLayer = entry.parallelTexts["Diplomatic"]!!
                        textLayer.text.split("<br>").forEach { line ->
                            val lineNo = lineCounter.getAndIncrement()
                            "l" {
                                attribute("xml:id", "og-mhg-$chapter-$lineNo")
                                attribute("n", lineNo)
                                unsafeText(
                                    line
                                        .replace("&nbsp; ", "&nbsp;&nbsp;")
                                        .replace("&nbsp; ", "&nbsp;&nbsp;")
                                        .convertHorizontalSpace()
                                        .trim()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Node.bookDivNode(entries: List<Entry>, lang: String) {
        val entryCounter = AtomicInt(1)
        val lineCounter = AtomicInt(1)
        "div" {
            attribute("xml:lang", lang)
            attribute("xml:id", "og-$lang")
            attribute("type", "original")
            entries.forEach { entry ->
                val entryMetadata = entry.metadata.asMap()
                val folioNr = entryMetadata["Bladzijde(n)"]!!
                "pb" {
                    attribute("xml:id", "pb-$lang-$folioNr")
                    attribute("facs", "#s${entryCounter.getAndIncrement()}")
                    attribute("n", folioNr)
                }
                metadataCommentNodes(entry)
                entry.parallelTexts["Diplomatic"]!!.text
                    .replace("<b>", "")
                    .replace("</b>", "")
                    .convertVerticalSpace()
                    .split("<br>")
                    .forEach { line ->
                        val lineNo = lineCounter.getAndIncrement()
                        "l" {
//                            attribute("xml:id", "og-$lang-$lineNo")
//                            attribute("n", lineNo)
                            unsafeText(
                                line
                                    .replace("&nbsp; ", "&nbsp;&nbsp;")
                                    .replace("&nbsp; ", "&nbsp;&nbsp;")
                                    .convertHorizontalSpace()
                                    .trim()
                            )
                        }
                    }
            }
        }
    }

    private fun Node.manuscriptTranslationDivNode(entriesPerChapter: Map<String, List<Entry>>) {
        val entryCounter = AtomicInt(1)
        val lineCounter = AtomicInt(1)
        "div" {
            attribute("xml:lang", "dum")
            attribute("xml:id", "og-dum")
            attribute("type", "translation")
            entriesPerChapter.forEach { (chapter, entries) ->
                "div" {
                    attribute("xml:id", "og-dum-$chapter")
                    attribute("n", chapter)
                    attribute("corresp", "#og-mgh-$chapter")
                    entries.forEach { entry ->
                        val entryMetadata = entry.metadata.asMap()
                        val folioNr = entryMetadata["Bladzijde(n)"]!!
                        "pb" {
                            attribute("xml:id", "pb-dum-$folioNr")
                            attribute("corresp", "#pb-mgh-$folioNr")
                            attribute("facs", "#s${entryCounter.getAndIncrement()}")
                            attribute("n", folioNr)
                        }
                        val textLayer = entry.parallelTexts["Reconstructie"]!!
                        textLayer.text.split("<br>").forEach { line ->
                            val lineNo = lineCounter.getAndIncrement()
                            "l" {
                                attribute("xml:id", "og-dum-$chapter-$lineNo")
                                attribute("n", lineNo)
                                attribute("corresp", "#ogb-mgh-$chapter-$lineNo")
                                unsafeText(
                                    line
                                        .replace("&nbsp; ", "&nbsp;&nbsp;")
                                        .replace("&nbsp; ", "&nbsp;&nbsp;")
                                        .convertHorizontalSpace()
                                        .trim()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Node.manuscriptTranslationUnalignedDivNode(entriesPerChapter: Map<String, List<Entry>>) {
        val entryCounter = AtomicInt(1)
        val parCounter = AtomicInt(1)
        "div" {
            attribute("xml:lang", "de")
            attribute("xml:id", "og-de")
            attribute("type", "translation-unaligned")
            entriesPerChapter.forEach { (chapter, entries) ->
                "div" {
                    attribute("xml:id", "og-de-$chapter")
                    attribute("n", chapter)
                    attribute("corresp", "#og-mgh-$chapter")
                    entries.forEach { entry ->
                        val entryMetadata = entry.metadata.asMap()
                        val folioNr = entryMetadata["Folionummer"]!!
                        "pb" {
                            attribute("xml:id", "pb-de-$folioNr")
                            attribute("corresp", "#pb-mgh-$folioNr")
                            attribute("facs", "#s${entryCounter.getAndIncrement()}")
                            attribute("n", folioNr)
                        }
                        val textLayer = entry.parallelTexts["Vertaling"]!!
                        textLayer.text.split("<br><br>").forEach { line ->
                            val parNo = parCounter.getAndIncrement()
                            "p" {
                                attribute("xml:id", "og-de-$chapter-$parNo")
                                attribute("n", parNo)
                                unsafeText(
                                    line
                                        .replace("<br>", "")
                                        .replace("&nbsp;", " ")
                                        .trim()

                                )
                            }
                        }
                    }
                }
            }
        }
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
//            profileDesc(metadataMap, letterMetadata)
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
                            attribute("form", "book")
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
        baseName: String,
        facsimileCounter: AtomicInteger,
        sectionId: Int
    ) {
        val facsimiles = entries.flatMap { it.facsimiles }
        if (facsimiles.isNotEmpty()) {
            "facsimile" {
                "surfaceGrp" {
                    attribute("xml:id", "surfacegrp.$sectionId")
                    facsimiles.forEachIndexed { i, facs ->
                        val n = facsimileCounter.getAndIncrement()
                        "surface" {
                            attribute("n", n)
                            attribute("xml:id", "s$n")
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
    }

    private fun Node.metadataCommentNodes(entry: Entry) {
        entry.metadata
            .filter { it.value.isNotEmpty() }
            .forEach { comment("${it.field} = ${it.value}") }
    }

    private fun Node.textNode(
        entry: Entry,
        metadataMap: Map<String, String>,
        letterMetadata: LetterMetadataConfig,
        sectionId: Int
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
                        val divId = "div.$sectionId"
//                        val divType = projectConfig.divTypeForLayerName[layerName] ?: layerName.lowercase()
                        val divType = "original"
                        val lang = when {
                            (divType == "translation") -> "nl"
                            else -> (metadataMap[letterMetadata.language])?.asIsoLang() ?: "nl"
                        }
                        val layerAnnotationMap = textLayer.annotationData.associateBy { it.n }
                        annotationMap.putAll(layerAnnotationMap.filter { !annoNumToRefTarget.contains(it.key.toString()) })
                        val text = textLayer.text
                            .replace("<b>", "")
                            .replace("</b>", "")
                            .transform(layerAnnotationMap, annoNumToRefTarget)
                            .removeLineBreaks()
                            .convertVerticalSpace()
                            .convertHorizontalSpace()
                            .setParagraphs(divType, lang)
                            .setPageBreaks(divType, lang, conversionConfig.pageBreakEncoding, sectionId)
                            //                                .wrapLines(80)
                            .wrapSpaceElementWithNewLines()
                            .replace("\n\n\n", "\n\n")
                        "div" {
                            attribute("type", divType)
                            attribute("xml:lang", lang)
                            attribute("xml:id", divId)
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

    private fun Node.standOffNode(annotationMap: MutableMap<Long, AnnotationData>, sectionId: Int) {
        if (annotationMap.isNotEmpty()) {
            val noteCounter = AtomicInt(1)
            "standOff" {
                "listAnnotation" {
                    attribute("type", "notes")
                    attribute("xml:id", "listannotation.$sectionId")
                    annotationMap.forEach { (id, data) ->
                        val noteContent = data.text.ifEmpty { data.annotatedText }
                        val noteText = AnnotationBodyConverter.convert(noteContent)
                        "note" {
                            attribute("xml:id", "note_$id")
                            attribute("n", noteCounter.andIncrement)
                            attribute("type", data.type.name.replace(" ", "_"))
                            comment("${data.type.name}")
                            "p" { unsafeText(noteText.replace("<lb/>", "<lb/>\n")) }
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

    private fun String.setPageBreaks(
        divType: String,
        lang: String,
        pageBreakEncoding: PageBreakEncoding,
        sectionId: Int
    ): String =
        when (pageBreakEncoding) {
            PageBreakEncoding.PILCROW -> this
                .replace("""<hi rend="bold">$ENCODED_PAGE_BREAK</hi>""", ENCODED_PAGE_BREAK)
                .split(ENCODED_PAGE_BREAK)
                .mapIndexed { i, t ->
                    if (i == 0) {
                        t
                    } else {
                        "\n<pb xml:id=\"pb.$sectionId.$divType.$lang.$i\" f=\"$i\" facs=\"#s$i\" n=\"$i\"/>\n$t"
                    }
                }
                .joinToString("")

            PageBreakEncoding.PAGE_BREAK_MARKER -> {
                var str = this
                if (!this.contains("[1]")) {
                    str = "[1]$str"
                }
                str.addPageBreaks(divType, lang, sectionId)
            }

            PageBreakEncoding.NONE -> this
        }

    val pbRegex = Regex("\\[(\\d+)]")
    fun String.addPageBreaks(divType: String, lang: String, sectionId: Int): String =
        pbRegex.replace(this) { matchResult ->
            val number = matchResult.groupValues[1]
            "<pb xml:id=\"pb.$sectionId.$divType.$lang.$number\" facs=\"#s$number\" n=\"$number\"/>"
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
            when (quantity) {
                1 -> " "
                else -> """<space dim="horizontal" unit="chars" quantity="$quantity"/>"""
            }

        val NBSP_MILESTONE_REGEX = "(?:<nbsp/>)+".toRegex()
        val NBSP_ENTITY_REGEX = "(?:&nbsp;)+".toRegex()

        fun String.convertHorizontalSpace(): String {
            val intermediate = NBSP_MILESTONE_REGEX.replace(this) { matchResult ->
                val count = matchResult.value.length / "<nbsp/>".length
                horizontalSpaceTag(count)
            }
            return NBSP_ENTITY_REGEX.replace(intermediate) { matchResult ->
                val count = matchResult.value.length / "&nbsp;".length
                horizontalSpaceTag(count)
            }
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


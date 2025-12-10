package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.config.ConfigTool.loadConfig
import nl.knaw.huc.di.elaborate.elabctl.config.ElabCtlConfig
import nl.knaw.huc.di.elaborate.elabctl.config.ProjectType

object Archiver {

    val json = Json { ignoreUnknownKeys = true }

    val teiParts = listOf(
        "about",
        "apparatus",
        "letters",
        "manuscript",
        "book",
    )

    @OptIn(ExperimentalSerializationApi::class)
    fun archive(warPaths: List<String>) {
        val customOrder = json.decodeFromStream<List<String>>(File("data/adk-order.json").inputStream())
        val errors: MutableList<String> = mutableListOf()
        warPaths.forEach { warPath ->
            val projectName = warPath.split('/').last().replace(".war", "")
            val projectConfig = ProjectConfig(
                projectName = projectName.replace("elab4-", ""),
                personIds = loadPersonIdMap(projectName),
                divTypeForLayerName = mapOf("Transcription" to "original", "Translation" to "translation")
            )
            val conversionConfig = loadConfig(projectConfig.projectName)
            val teiBuilder = TEIBuilder(projectConfig, conversionConfig)
            for (part in teiParts) {
                File("build/zip/$projectName/$part").apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
            File("out").mkdirs()
            logger.info { "<= $warPath" }
            val facsimilePaths = mutableListOf<String>()
            val scriptLines = mutableListOf(
                "rm -rf /data/tmp/facsimiles",
                "mkdir -p /data/tmp/facsimiles",
                "cd /data/webapps/jp2"
            )
            val report = ConversionReporter(projectConfig.projectName, conversionConfig)
            ZipFile(warPath).use { zip ->
                val elabConfigEntry = zip.getEntry("data/config.json")
                val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                    json.decodeFromStream(input)
                }
//            prettyPrint(elabConfig)
                val entryTypeName = elabConfig.entryTermSingular
                val originalEntries = elabConfig.entries
                val entries = customOrder.map { filename -> originalEntries.first { it.datafile == filename } }
                println(entries.joinToString("\n"))
                val total = entries.size
                when (conversionConfig.type) {
                    ProjectType.LETTERS -> convertLettersProject(
                        entries,
                        total,
                        entryTypeName,
                        zip,
                        report,
                        scriptLines,
                        facsimilePaths,
                        teiBuilder,
                        projectName,
                        errors
                    )

                    ProjectType.MANUSCRIPT -> convertManuscriptProject(
                        entries,
                        total,
                        entryTypeName,
                        zip,
                        report,
                        scriptLines,
                        facsimilePaths,
                        teiBuilder,
                        projectName,
                        errors
                    )

                    ProjectType.BOOK -> convertBookProject(
                        entries,
                        total,
                        entryTypeName,
                        zip,
                        report,
                        scriptLines,
                        facsimilePaths,
                        teiBuilder,
                        projectName,
                        errors,
                    )

                }
            }
            report.storeAsCsv()
            errors.addAll(convertWordPressExport(projectName, conversionConfig))
            createZip(projectName)
            storeFacsimilePaths(facsimilePaths)
            storeScriptLines(scriptLines)
            if (errors.isNotEmpty()) {
                logger.error { "${errors.size} errors found:" }
                errors.forEach { logger.error { it } }
            }
        }
    }

    fun convertWordPressExport(projectName: String, conversionConfig: ElabCtlConfig): List<String> {
        val outputDir = "build/zip/$projectName/about"
        val errors = mutableListOf<String>()
        val wpePath = "data/${projectName.replace("elab4-", "")}-wpe.xml"
        if (Path(wpePath).exists()) {
            val conversionErrors = WordPressExportConverter(outputDir, conversionConfig).convert(wpePath)
            errors.addAll(conversionErrors)
        } else {
            errors.add("File not found: $wpePath")
        }
        return errors
    }

    private fun loadPersonIdMap(projectName: String): Map<String, String> {
        val path = "data/$projectName/person-ids.json"
        val src = File(path)
        return if (src.exists()) {
            logger.info { "<= $path" }
            jacksonObjectMapper().readValue(src)
        } else {
            emptyMap()
        }
    }

    private fun storeScriptLines(scriptLines: MutableList<String>) {
        scriptLines.add(
            "cd /data/tmp &&" +
                    " if [ -f facsimiles.zip ] ; then rm facsimiles.zip; fi &&" +
                    " echo creating checksum file... &&" +
                    " sha256sum $(find facsimiles/ -type f | sort) > manifest-sha256.txt &&" +
                    " echo moving facsimiles to zip archive... &&" +
                    " zip -r facsimiles.zip $(find facsimiles/ -type f | sort) manifest-sha256.txt &&" +
                    " rm -rf /data/tmp/facsimiles manifest-sha256.txt"
        )
        val path = "out/copy-facsimiles.sh"
        logger.info { "=> $path" }
        val file = File(path)
        file.writeText(scriptLines.joinToString("\n"))
        file.setExecutable(true)
    }

    private fun storeFacsimilePaths(facsimilePaths: List<String>) {
        val path = "out/facsimile-paths.txt"
        logger.info { "=> $path" }
        File(path).writeText(facsimilePaths.sorted().joinToString("\n"))
    }

    private fun createZip(projectName: String) {
        val sourceFile = "build/zip/$projectName/"
        val zipPath = "out/$projectName-archive.zip"
        logger.info("=> $zipPath")
        FileOutputStream(zipPath).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                val fileToZip = File(sourceFile)
                zipFile(fileToZip, fileToZip.name, zipOut)
            }
        }
    }

    @Throws(IOException::class)
    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) {
            return
        }
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            val children: Array<File> = fileToZip.listFiles() ?: emptyArray()
            for (childFile in children.sorted()) {
                zipFile(childFile, "$fileName/${childFile.name}", zipOut)
            }
            return
        }
        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zipOut.putNextEntry(zipEntry)
            val bytes = ByteArray(1024)
            var length: Int
            while ((fis.read(bytes).also { length = it }) >= 0) {
                zipOut.write(bytes, 0, length)
            }
        }
    }

    private fun processFacsimiles(
        baseName: String,
        facsimiles: ArrayList<Facsimile>,
        scriptLines: MutableList<String>
    ) {
//        val client = HttpClient(CIO) {
//            install(HttpTimeout) {
//                requestTimeoutMillis = 10_000
//            }
//            install(HttpRequestRetry) {
//                retryOnServerErrors(maxRetries = 5)
//                exponentialDelay()
//            }
//        }
        scriptLines.add("echo copying ${facsimiles.size} image files...")
        facsimiles.forEachIndexed { i, f ->
            val url = f.thumbnail.replace("/adore-djatoka.*localhost:8080".toRegex(), "")
            logger.info { url }
            val originalFilePath = URLDecoder.decode(url.replace("http.*/jp2/".toRegex(), ""), "UTF-8")
            val imageName = "${baseName}-${(i + 1).toString().padStart(2, '0')}.jp2"

            scriptLines.add("cp \"$originalFilePath\" \"/data/tmp/facsimiles/$imageName\"")
//            val filePath = "build/zip/$projectName/facsimiles/$imageName"
//            runBlocking {
//                val bytes: ByteArray = client.get(url).body<ByteArray>()
//                logger.info { "=> $filePath" }
//                File(filePath).writeBytes(bytes)
//            }
        }
    }

    fun teiName(entryTypeName: String, i: Int, shortName: String): String =
        "$entryTypeName-${i.toString().padStart(4, '0')}-${
            nonAlphaNumericRegex.replace(shortName.trim()) { "_" }
        }".trim(
            '-',
            '_'
        )

    @OptIn(ExperimentalSerializationApi::class)
    fun loadEntry(zip: ZipFile, entryDescription: EntryDescription): Entry {
        val zipEntry = zip.getEntry("data/${entryDescription.datafile}")
        return zip.getInputStream(zipEntry).use { input ->
            json.decodeFromStream<Entry>(input)
        }
    }

//    @OptIn(ExperimentalSerializationApi::class)
//    private fun prettyPrint(elabConfig: EditionConfig) {
//        val prettyJson = Json { // this returns the JsonBuilder
//            prettyPrint = true
//            prettyPrintIndent = " "
//        }
//        logger.info { prettyJson.encodeToString(value = elabConfig) }
//    }

    private fun convertLettersProject(
        entryDescriptions: List<EntryDescription>,
        total: Int,
        entryTypeName: String,
        zip: ZipFile,
        report: ConversionReporter,
        scriptLines: MutableList<String>,
        facsimilePaths: MutableList<String>,
        teiBuilder: TEIBuilder,
        projectName: String,
        errors: MutableList<String>,
    ) {
        entryDescriptions
//                .take(1)
            .forEachIndexed { i, entryDescription ->
                logger.info { "entry ${i + 1} / $total..." }
                logger.info { entryDescription }
                val teiName =
                    teiName(entryTypeName, i + 1, entryDescription.name.lowercase())
                val entry = loadEntry(zip, entryDescription)
                report.addEntry(entry, teiName)

                processFacsimiles(teiName, entry.facsimiles, scriptLines)
                facsimilePaths.addAll(entry.facsimiles.map {
                    it.thumbnail.replace(
                        "http.*/jp2/".toRegex(),
                        ""
                    )
                })

//                logger.info { entry.metadata }
                val (tei, teiPath, xiRefs) = buildLetterTei(
                    teiBuilder,
                    projectName,
                    entry,
                    teiName,
                    AtomicInteger(1),
                    AtomicInteger(1),
                    i
                )
                exportTei(tei, teiPath, errors)
            }
    }

    private fun convertManuscriptProject(
        entryDescriptions: List<EntryDescription>,
        total: Int,
        entryTypeName: String,
        zip: ZipFile,
        report: ConversionReporter,
        scriptLines: MutableList<String>,
        facsimilePaths: MutableList<String>,
        teiBuilder: TEIBuilder,
        projectName: String,
        errors: MutableList<String>
    ) {
        entryDescriptions
//                .take(1)
            .forEachIndexed { i, entryDescription ->
                logger.info { "entry ${i + 1} / $total..." }
                logger.info { entryDescription }
                val teiName =
                    teiName(entryTypeName, i + 1, entryDescription.name)
                val entry = loadEntry(zip, entryDescription)
                report.addEntry(entry, teiName)

                processFacsimiles(teiName, entry.facsimiles, scriptLines)
                facsimilePaths.addAll(entry.facsimiles.map {
                    it.thumbnail.replace(
                        "http.*/jp2/".toRegex(),
                        ""
                    )
                })
            }
        val (tei, teiPath) = buildManuscriptTei(teiBuilder, projectName, entryDescriptions, zip)
        exportTei(tei, teiPath, errors)
    }

    private fun convertBookProject(
        entryDescriptions: List<EntryDescription>,
        total: Int,
        entryTypeName: String,
        zip: ZipFile,
        report: ConversionReporter,
        scriptLines: MutableList<String>,
        facsimilePaths: MutableList<String>,
        teiBuilder: TEIBuilder,
        projectName: String,
        errors: MutableList<String>,
    ) {
        val allSurfaceGrpRefs = mutableListOf<TEIBuilder.XIncludeRef>()
        val allDivRefs = mutableListOf<TEIBuilder.XIncludeRef>()
        val listAnnotationRefs = mutableListOf<TEIBuilder.XIncludeRef>()
        val facsimileCounter = AtomicInteger(1)
        val divCounter = AtomicInteger(1)
        entryDescriptions
            .map { loadEntry(zip, it) }
            .forEachIndexed { i, entry ->
//            }
//
//        entryDescriptions
////                .take(1)
//            .forEachIndexed { i, entryDescription ->
                logger.info { "entry ${i + 1} / $total..." }
//                logger.info { entryDescription }
//                logger.info { entry }
                val teiName =
                    teiName(entryTypeName, i + 1, entry.name.lowercase()).replace(
                        "entry",
                        "section"
                    )
//                val entry = loadEntry(zip, entryDescription)
                report.addEntry(entry, teiName)

                processFacsimiles(teiName, entry.facsimiles, scriptLines)
                facsimilePaths.addAll(entry.facsimiles.map {
                    it.thumbnail.replace(
                        "http.*/jp2/".toRegex(),
                        ""
                    )
                })

//                logger.info { entry.metadata }
                val (tei, teiPath, xiRefs: XIRefs) = buildLetterTei(
                    teiBuilder,
                    projectName,
                    entry,
                    teiName,
                    facsimileCounter,
                    divCounter,
                    i + 1
                )
                val sectionPath = teiPath.replace("letters", "book")
                allSurfaceGrpRefs.addAll(xiRefs.surfaceGrpRefs.map { it.copy(href = sectionPath.substringAfterLast("/")) })
                allDivRefs.addAll(xiRefs.divRefs.map { it.copy(href = sectionPath.substringAfterLast("/")) })
                listAnnotationRefs.addAll(xiRefs.listAnnotationRefs.map {
                    it.copy(
                        href = sectionPath.substringAfterLast(
                            "/"
                        )
                    )
                })
                exportTei(tei, sectionPath, errors)
            }

        val (tei, teiPath) = buildBookTei(
            teiBuilder = teiBuilder,
            projectName = projectName,
            xiRefs = XIRefs(allSurfaceGrpRefs, allDivRefs, listAnnotationRefs),
        )
        exportTei(tei, teiPath, errors)
    }

    data class XIRefs(
        val surfaceGrpRefs: List<TEIBuilder.XIncludeRef>,
        val divRefs: List<TEIBuilder.XIncludeRef>,
        val listAnnotationRefs: List<TEIBuilder.XIncludeRef>,
    )

//    private fun convertBookProject0(
//        entryDescriptions: ArrayList<EntryDescription>,
//        total: Int,
//        entryTypeName: String,
//        zip: ZipFile,
//        report: ConversionReporter,
//        scriptLines: MutableList<String>,
//        facsimilePaths: MutableList<String>,
//        teiBuilder: TEIBuilder,
//        projectName: String,
//        errors: MutableList<String>
//    ) {
//        entryDescriptions
////                .take(1)
//            .forEachIndexed { i, entryDescription ->
//                logger.info { "entry ${i + 1} / $total..." }
//                logger.info { entryDescription }
//                val teiName =
//                    teiName(entryTypeName, i + 1, entryDescription.name).replace("entry", "section")
//                val entry = loadEntry(zip, entryDescription)
//                report.addEntry(entry, teiName)
//
//                processFacsimiles(teiName, entry.facsimiles, scriptLines)
//                facsimilePaths.addAll(entry.facsimiles.map {
//                    it.thumbnail.replace(
//                        "http.*/jp2/".toRegex(),
//                        ""
//                    )
//                })
//            }
//        val (tei, teiPath) = buildBookTei(
//            teiBuilder = teiBuilder,
//            projectName = projectName,
//            title = "Wij Slaven van Suriname",
//            surfaceRefs = listOf(),
//            divRefs = listOf(),
//            noteRefs = listOf(),
//            editorId = TODO(),
//            editorName = TODO(),
//            editorUrl = TODO(),
//        )
//        exportTei(tei, teiPath, errors)
////        exportMainTei(mainTEIPath)
//    }

//    private fun exportMainTei(teiPath: String, tei: String) {
//        logger.info { "=> $teiPath" }
//        Path(teiPath).writeText(tei)
//        logger.info { "" }
//    }

    private fun exportTei(tei: String, teiPath: String, errors: MutableList<String>) {
        if (!tei.isWellFormed()) {
            errors.add("file $teiPath is NOT well-formed!")
        }
        logger.info { "=> $teiPath" }
        Path(teiPath).writeText(tei)
        logger.info { "" }
    }

    private fun buildLetterTei(
        teiBuilder: TEIBuilder,
        projectName: String,
        entry: Entry,
        teiName: String,
        facsimileCounter: AtomicInteger,
        divCounter: AtomicInteger,
        sectionId: Int
    ): Triple<String, String, XIRefs> {
        val (tei, xiRefs) = teiBuilder.entryToTEI(entry, teiName, facsimileCounter, divCounter, sectionId)
        val teiPath = "build/zip/$projectName/letters/${teiName}.xml"
        return Triple(tei, teiPath, xiRefs)
    }

    private fun buildManuscriptTei(
        teiBuilder: TEIBuilder,
        projectName: String,
        entryDescriptions: List<EntryDescription>,
        zip: ZipFile
    ): Pair<String, String> {
        val shortName = projectName.substringAfter("elab4-")
        val tei = teiBuilder.manuscriptToTEI(entryDescriptions.map { loadEntry(zip, it) }, shortName)
        val teiPath = "build/zip/$projectName/manuscript/${shortName}.xml"
        return Pair(tei, teiPath)
    }

    private fun buildBookTei(
        teiBuilder: TEIBuilder,
        projectName: String,
        xiRefs: XIRefs
    ): Pair<String, String> {
        val tei = teiBuilder.bookMainToTEI(
            projectName = projectName,
            xiRefs = xiRefs,
        )
        val teiPath = "build/zip/$projectName/book/main.xml"
        return Pair(tei, teiPath)
    }

    private fun buildBookTei0(
        teiBuilder: TEIBuilder,
        projectName: String,
        entryDescriptions: ArrayList<EntryDescription>,
        zip: ZipFile
    ): Pair<String, String> {
        val shortName = projectName.substringAfter("elab4-")
        val tei = teiBuilder.bookToTEI(entryDescriptions.map { loadEntry(zip, it) }, shortName)

        val teiPath = "build/zip/$projectName/book/${shortName}.xml"
        return Pair(tei, teiPath)
    }

    // TODO:
    // add shs256sum checksums of all files in the archive, in a file called `manifest-sha256.txt`
}


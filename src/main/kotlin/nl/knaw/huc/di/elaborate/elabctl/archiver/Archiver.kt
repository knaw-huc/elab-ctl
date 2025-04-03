package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.TEIBuilder.toTEI

object Archiver {

    val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalSerializationApi::class)
    fun archive(warPaths: List<String>) {
        warPaths.forEach { warPath ->
            val projectName = warPath.split('/').last().replace(".war", "")
            File("build/zip/$projectName").deleteRecursively()
            File("build/zip/$projectName").mkdirs()
            File("out").mkdirs()
            logger.info { "<= $warPath" }
            val facsimilePaths = mutableListOf<String>()
            val scriptLines = mutableListOf(
                "rm -rf /data/tmp/facsimiles",
                "mkdir -p /data/tmp/facsimiles",
                "cd /data/webapps/jp2"
            )
            ZipFile(warPath).use { zip ->
                val elabConfigEntry = zip.getEntry("data/config.json")
                val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                    json.decodeFromStream(input)
                }
//            prettyPrint(elabConfig)
                val entryTypeName = elabConfig.entryTermSingular
                val entries = elabConfig.entries
                val total = entries.size
                entries
//                .take(1)
                    .forEachIndexed { i, entryDescription ->
                        logger.info { "entry ${i + 1} / $total..." }
                        logger.info { entryDescription }
                        val teiName =
                            teiName(entryTypeName, i + 1, entryDescription.shortName)
                        val entry = loadEntry(zip, entryDescription)

                        processFacsimiles(teiName, entry.facsimiles, scriptLines)
                        facsimilePaths.addAll(entry.facsimiles.map {
                            it.thumbnail.replace(
                                "http.*/jp2/".toRegex(),
                                ""
                            )
                        })

//                logger.info { entry.metadata }
                        val tei = entry.toTEI(teiName, projectName.replace("elab4-", ""))
                        val teiPath = "build/zip/$projectName/${teiName}.xml"
                        logger.info { "=> $teiPath" }
                        Path(teiPath).writeText(tei)
                        logger.info { "" }
                    }
            }
            createZip(projectName)
            storeFacsimilePaths(facsimilePaths)
            storeScriptLines(scriptLines)
        }
    }

    private fun storeScriptLines(scriptLines: MutableList<String>) {
        scriptLines.add(
            "cd /data/tmp &&" +
                    " if [ -f facsimiles.zip ] ; then rm facsimiles.zip; fi &&" +
                    " echo creating checksum file... &&" +
                    " sha256sum \$(find facsimiles/ -type f | sort) > manifest-sha256.txt &&" +
                    " echo moving facsimiles to zip archive... &&" +
                    " zip -r facsimiles.zip \$(find facsimiles/ -type f | sort) manifest-sha256.txt &&" +
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

    private fun teiName(entryTypeName: String, i: Int, shortName: String): String =
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

    @OptIn(ExperimentalSerializationApi::class)
    private fun prettyPrint(elabConfig: EditionConfig) {
        val prettyJson = Json { // this returns the JsonBuilder
            prettyPrint = true
            prettyPrintIndent = " "
        }
        logger.info { prettyJson.encodeToString(value = elabConfig) }
    }

    // TODO:
    // add shs256sum checksums of all files in the archive, in a file called `manifest-sha256.txt`
}
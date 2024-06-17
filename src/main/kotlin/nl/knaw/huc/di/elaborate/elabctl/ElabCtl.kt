package nl.knaw.huc.di.elaborate.elabctl

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.TEIBuilder.toTEI

val logger = logger("Main")
val commands = mapOf(
    "archive" to ::archive,
    "help" to ::showHelp
)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        showHelp(args.asList())
    } else {
        val command = args[0]
        if (commands.containsKey(command)) {
            val millis = measureTimeMillis { commands[command]?.call(args.asList()) }
            println("> running `$command` took ${convertMillisToTimeString(millis)}")
        } else {
            showHelp(args.asList())
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun archive(args: List<String>) {
    logger.info { "args=${args}" }
    if (args.size > 1) {
        val warPath = args[1]
        val projectName = warPath.split('/').last().replace(".war", "")
        File("build/zip/$projectName").deleteRecursively()
        File("build/zip/$projectName/facsimiles").mkdirs()
        File("out").mkdirs()
        logger.info { "<= $warPath" }
        val facsimilePaths = mutableListOf<String>()
        ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                Json.decodeFromStream(input)
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
                    val teiName = teiName(entryTypeName, i + 1, entryDescription.shortName)
                    val entry = loadEntry(zip, entryDescription)
                    storeFacsimiles(projectName, teiName, entry.facsimiles)
                    facsimilePaths.addAll(entry.facsimiles.map { it.thumbnail.replace("http.*/jp2/".toRegex(), "") })

//                logger.info { entry.metadata }
                    val tei = entry.toTEI(teiName)
                    val teiPath = "build/zip/$projectName/${teiName}.xml"
                    logger.info { "=> $teiPath" }
                    Path(teiPath).writeText(tei)
                    logger.info { "" }
                }
        }
        createZip(projectName)
        storeFacsimilePaths(facsimilePaths)
    }
}

fun storeFacsimilePaths(facsimilePaths: List<String>) {
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

fun storeFacsimiles(projectName: String, baseName: String, facsimiles: ArrayList<Facsimile>) {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            exponentialDelay()
        }
    }
    facsimiles.forEachIndexed { i, f ->
        val url = f.thumbnail.replace("/adore-djatoka.*localhost:8080".toRegex(), "")
        logger.info { url }
        val filePath = "build/zip/$projectName/facsimiles/${baseName}-${(i + 1).toString().padStart(2, '0')}.jp2"
        runBlocking {
            val bytes: ByteArray = client.get(url).body<ByteArray>()
            logger.info { "=> $filePath" }
            File(filePath).writeBytes(bytes)
        }
    }
}

fun teiName(entryTypeName: String, i: Int, shortName: String): String =
    "$entryTypeName-${i.toString().padStart(4, '0')}-${shortName.trim()}".trim('-')

@OptIn(ExperimentalSerializationApi::class)
fun loadEntry(zip: ZipFile, entryDescription: EntryDescription): Entry {
    val zipEntry = zip.getEntry("data/${entryDescription.datafile}")
    return zip.getInputStream(zipEntry).use { input ->
        Json.decodeFromStream<Entry>(input)
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

fun showHelp(args: List<String>) {
    println("add command arg")
    println("available commands: ")
    commands.keys.sorted().forEach {
        println("  $it")
    }
}

private fun convertMillisToTimeString(milliseconds: Long): String {
    val hours = milliseconds / (1000 * 60 * 60)
    val remainingMinutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
    val remainingSeconds = ((milliseconds % (1000 * 60 * 60)) % (1000 * 60)) / 1000
    val remainingMillis = milliseconds % 1000

    return "%02d:%02d:%02d.%03d".format(hours, remainingMinutes, remainingSeconds, remainingMillis)
}

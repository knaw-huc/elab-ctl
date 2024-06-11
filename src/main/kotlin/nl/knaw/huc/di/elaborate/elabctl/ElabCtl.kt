package nl.knaw.huc.di.elaborate.elabctl

import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
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
    File("build/zip").deleteRecursively()
    File("build/zip/facsimiles").mkdirs()
    if (args.size > 1) {
        val warPath = args[1]
        logger.info { "<= $warPath" }
        ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                Json.decodeFromStream(input)
            }
//            prettyPrint(elabConfig)
            val entryTypeName = elabConfig.entryTermSingular
            val entries = elabConfig.entries
            entries.take(1).forEachIndexed { i, entryDescription ->
                logger.info { entryDescription }
                val teiName = teiName(entryTypeName, i + 1, entryDescription.shortName)
                val entry = loadEntry(zip, entryDescription)
                storeFacsimiles(teiName, entry.facsimiles)
//                logger.info { entry.metadata }
                val tei = entry.toTEI(teiName)
                val teiPath = "build/zip/${teiName}.xml"
                logger.info { "=> $teiPath" }
                Path(teiPath).writeText(tei)
            }
        }
    }
}

fun storeFacsimiles(baseName: String, facsimiles: ArrayList<Facsimile>) {
    facsimiles.forEachIndexed { i, f ->
        val url = f.thumbnail.replace("/adore-djatoka.*localhost:8080".toRegex(), "")
        logger.info { url }
        val filePath = "build/zip/facsimiles/${baseName}-${(i + 1).toString().padStart(2, '0')}.jp2"
        runBlocking {
            val bytes: ByteArray = HttpClient(CIO).get(url).body<ByteArray>()
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

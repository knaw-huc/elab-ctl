package nl.knaw.huc.di.elaborate.elabctl

import java.util.zip.ZipFile
import kotlin.system.measureTimeMillis
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger

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
    println("TODO: implement archive function")
    logger.info { "args=${args}" }
    if (args.size > 1) {
        val warPath = args[1]
        logger.info { "<= $warPath" }
        ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            val elabConfig: JsonObject = zip.getInputStream(elabConfigEntry).use { input ->
                Json.decodeFromStream(input)
            }
            prettyPrint(elabConfig)
            val entryTypeName = elabConfig["entryTermSingular"]
            val entries = elabConfig["entries"]


//            zip.entries()
//                .asSequence()
//                .filter { it.name.startsWith("data/") }
//                .sortedBy { it.name }
//                .forEach { entry ->
//                    logger.info { entry.name }
//                    val map: JsonObject = zip.getInputStream(entry).use { input ->
//                        Json.decodeFromStream(input)
//                    }
//                    logger.info { map }
//                }
        }

    }
    //    load the edition war
    //    unzip edition war
    // convert each entry json to a tei file
    // for each entry, download and store the facsimiles

}

@OptIn(ExperimentalSerializationApi::class)
private fun prettyPrint(elabConfig: JsonObject) {
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

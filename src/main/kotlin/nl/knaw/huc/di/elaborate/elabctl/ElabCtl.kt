package nl.knaw.huc.di.elaborate.elabctl

import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis
import arrow.core.tail
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory
import nl.knaw.huc.di.elaborate.elabctl.archiver.ManifestV3Factory

val logger = logger("Main")
val commands = mapOf(
    "archive" to ::archive,
    "generate-manifests" to ::generateManifests,
    "help" to ::showHelp
)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        showHelp(args.asList())
    } else {
        val command = args[0]
        if (commands.containsKey(command)) {
            val millis = measureTimeMillis { commands[command]?.call(args.asList().tail()) }
            println("> running `$command` took ${convertMillisToTimeString(millis)}")
        } else {
            showHelp(args.asList())
        }
    }
}

fun archive(args: List<String>) {
    logger.debug { "args=${args}" }
    if (args.size > 1) {
        Archiver.archive(args)
    }
}

fun generateManifests(args: List<String>) {
    logger.debug { "args=${args}" }
    if (args.isNotEmpty()) {
        val zipPath = args[0]
        logger.info { "<= $zipPath" }
        FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath(zipPath)
            .groupBy { it.fileName.substringBeforeLast('-') }
            .forEach { (entryName, facsimileDimensions) ->
                val manifestJson = ManifestV3Factory(
                    "https://manifests.editem.huygens.knaw.nl/projectname",
                    "https://iiif.editem.huygens.knaw.nl/projectname"
                ).forEntry(entryName, facsimileDimensions)
                val outPath = "out/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
            }
    }
}

fun showHelp(args: List<String>) {
    logger.debug { args }
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

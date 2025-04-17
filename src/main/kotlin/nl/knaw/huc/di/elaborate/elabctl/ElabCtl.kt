package nl.knaw.huc.di.elaborate.elabctl

import kotlin.system.measureTimeMillis
import arrow.core.tail
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver
import nl.knaw.huc.di.elaborate.elabctl.manifests.ManifestGenerator

val logger = logger("Main")
val commands = mapOf(
    "archive" to ::archive,
    "generate-manifests" to ::generateManifests,
    "help" to ::showHelp
)

fun main(args: Array<String>) {
    println("elabctl - elaborate tools")
    println("-------------------------\n")
    if (args.isEmpty()) {
        println("add command arg")
        showHelp(args.asList())
    } else {
        val command = args[0]
        if (commands.containsKey(command)) {
            println("command: $command\n")
            val millis = measureTimeMillis { commands[command]?.call(args.asList().tail()) }
            println("\n> running `$command` took ${convertMillisToTimeString(millis)}")
        } else {
            println("unknown command: $command\n")
            showHelp(args.asList())
        }
    }
}

fun archive(args: List<String>) {
    logger.debug { "args=${args}" }
    if (args.isNotEmpty()) {
        Archiver.archive(args)
    } else {
        println("missing argument(s): war-path(s) - path(s) to .war-files to generate archive files out of")
    }
}

fun generateManifests(args: List<String>) {
    logger.debug { "args=${args}" }
    if (args.size == 2) {
        val zipPath = args[0]
        val projectName = args[1]
        ManifestGenerator.generateFrom(zipPath, projectName)
    } else {
        println("missing argument(s): zip-path - path to the facsimiles.zip")
        println("                     project-name - the project name")
    }
}

fun showHelp(args: List<String>) {
    logger.debug { args }
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

package nl.knaw.huc.di.elaborate.elabctl

import kotlin.system.measureTimeMillis
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver

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

fun archive(args: List<String>) {
    logger.info { "args=${args}" }
    if (args.size > 1) {
        Archiver.archive(args.subList(1, args.size))
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

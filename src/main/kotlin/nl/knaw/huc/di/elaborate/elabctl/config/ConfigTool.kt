package nl.knaw.huc.di.elaborate.elabctl.config

import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import org.apache.logging.log4j.kotlin.logger

object ConfigTool {
    fun loadConfig(projectName: String): ElabCtlConfig {
        val path = Path("conf").resolve("$projectName.yml")
        if (path.notExists()) {
            logger.error { "No config found at $path; creating default config. Please edit this config and run elabctl again." }
            path.writeText(Yaml.default.encodeToString(defaultConfig()))
            exitProcess(-1)
        } else {
            val yaml = path.readText()
            return Yaml.default.decodeFromString(ElabCtlConfig.serializer(), yaml)
        }
    }

    fun defaultConfig(): ElabCtlConfig =
        ElabCtlConfig(
            projectName = "",
            title = "",
            type = ProjectType.LETTERS,
            editor = EditorConfig(
                id = "",
                name = "",
                url = ""
            ),
            divRole = "",
            pageBreakEncoding = PageBreakEncoding.NONE,
            letterDates = LetterDateConfig(
                earliestYear = 1900,
                latestYear = 2000
            ),
            letterMetadata = LetterMetadataConfig(
                sender = "Sender",
                senderPlace = "Place",
                recipient = "",
                date = "",
                language = ""
            )
        )
}
package nl.knaw.huc.di.elaborate.elabctl.config

import kotlin.io.path.Path
import kotlin.io.path.readText
import com.charleskorn.kaml.Yaml

object ConfigTool {
    fun loadConfig(projectName: String): ElabCtlConfig {
        val yaml = Path("conf").resolve("$projectName.yml").readText()
        return Yaml.default.decodeFromString(ElabCtlConfig.serializer(), yaml)
    }
}
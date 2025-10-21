package nl.knaw.huc.di.elaborate.elabctl.manifests

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.appendLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory

object ManifestGenerator {

    const val PROD_IIIF_BASE_URL = "https://iiif-text.huc.knaw.nl/iiif/3"
    const val DEV_IIIF_BASE_URL = "https://tt-iiif.dev.diginfra.org/iiif/3"

    @OptIn(ExperimentalSerializationApi::class)
    fun generateFrom(zipPath: String, warPath: String, mode: Mode) {
//        val projectName = "brieven-correspondenten-1900"
        val projectName = warPath.split('/').last().replace("elab4-", "").replace(".war", "")
        val elabConfig: EditionConfig = ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            zip.getInputStream(elabConfigEntry).use { input ->
                json.decodeFromStream(input)
            }
        }
        val destDir = "out/$projectName"
        File(destDir).mkdirs()
        val manifestFactory = ManifestV3Factory(
            "https://editem.huygens.knaw.nl/files/$projectName/static/manifests",
            "$PROD_IIIF_BASE_URL/$projectName%7Cpages%7C"
        )
        val pageSizesPath = "$destDir/sizes_pages.tsv"

        logger.info { "=> $pageSizesPath" }
        val pageSizesFile = Path(pageSizesPath)
        pageSizesFile.writeLines(listOf("file\twidth\theight"))

        logger.info { "<= $zipPath" }
        val groups = FacsimileDimensionsFactory
            .readFacsimileDimensionsFromZipFilePath(zipPath)
            .groupBy { it.fileName.substringBeforeLast('-') }
        when (mode) {
            Mode.ENTRY -> groups.forEach { (entryName, facsimileDimensions) ->
                val manifestJson = manifestFactory.forEntry(entryName, facsimileDimensions, elabConfig)
                val outPath = "$destDir/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
                updatePageSizes(facsimileDimensions, pageSizesFile)
            }

            Mode.PROJECT -> {
                groups.forEach { (_, facsimileDimensions) ->
                    updatePageSizes(facsimileDimensions, pageSizesFile)
                }
                val manifestJson = manifestFactory.forProject(projectName, elabConfig, groups)
                val outPath = "$destDir/$projectName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
            }
        }
    }

    private fun updatePageSizes(
        facsimileDimensions: List<FacsimileDimensionsFactory.FacsimileDimensions>,
        pageSizesFile: Path
    ) {
        val pageSizeLines = facsimileDimensions
            .map { fd -> "${fd.fileName.substringBeforeLast(".")}\t${fd.width}\t${fd.height}" }
            .asSequence()
        pageSizesFile.appendLines(pageSizeLines)
    }

    enum class Mode() {
        PROJECT, ENTRY
    }

}
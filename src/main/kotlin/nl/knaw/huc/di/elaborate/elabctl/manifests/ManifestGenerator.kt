package nl.knaw.huc.di.elaborate.elabctl.manifests

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.appendLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory

object ManifestGenerator {
    const val PROD_IIIF_BASE_URL = "https://iiif-text.huc.knaw.nl/iiif/3"

    @Serializable
    data class LetterMetadata(
        val id: String,
        val manifest: String,
        val pageMetadata: Map<String, PageMetadata>,
    )

    @Serializable
    data class PageMetadata(
        val iiifBaseUrl: String,
        val manifestUrl: String,
    )

    //    const val DEV_IIIF_BASE_URL = "https://tt-iiif.dev.diginfra.org/iiif/3"
    @OptIn(ExperimentalSerializationApi::class)
    val prettyJson = Json { // this returns the JsonBuilder
        prettyPrint = true
        prettyPrintIndent = "  "
        explicitNulls = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun generateFrom(zipPath: String, warPath: String, mode: Mode) {
//        val projectName = "brieven-correspondenten-1900"
        val projectName = warPath.split('/')
            .last()
            .replace("elab4-", "")
            .replace(".war", "")
        val elabConfig: EditionConfig = ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            zip.getInputStream(elabConfigEntry).use { input ->
                json.decodeFromStream(input)
            }
        }
        val destDir = "out/$projectName"
        File(destDir).mkdirs()
        val manifestFactory = ManifestV3Factory(
            "https://editem.pages.huc.knaw.nl/$projectName/manifests",
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
                val (manifest, entryMetadata) = manifestFactory.forEntry(entryName, facsimileDimensions, elabConfig)
                val manifestsPath = "$destDir/manifests"
                File(manifestsPath).mkdirs()
                val outPath = "$manifestsPath/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifest.toString())
                val metadataDir = "$destDir/metadata/$entryName"
                File(metadataDir).mkdirs()
                val metadataPath = "$metadataDir/metadata.json"
                logger.info { "=> $metadataPath" }
                Path(metadataPath).writeText(prettyJson.encodeToString(entryMetadata))
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
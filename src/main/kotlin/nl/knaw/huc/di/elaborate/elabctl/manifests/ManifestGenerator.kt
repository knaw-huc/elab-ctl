package nl.knaw.huc.di.elaborate.elabctl.manifests

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.appendLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory
import nl.knaw.huc.di.elaborate.elabctl.logger

object ManifestGenerator {
    const val PROD_IIIF_BASE_URL = "https://iiif-text.huc.knaw.nl/iiif/3"
    const val DEV_IIIF_BASE_URL = "https://tt-iiif.dev.diginfra.org/iiif/3"

    enum class Mode() {
        PROJECT, ENTRY
    }

    fun generateFrom(zipPath: String, projectName: String, mode: Mode) {
//        val projectName = "brieven-correspondenten-1900"
        val destDir = "out/$projectName"
        File(destDir).mkdirs()
        val manifestFactory = ManifestV3Factory(
            "https://manifests.editem.huygens.knaw.nl/$projectName",
            "$PROD_IIIF_BASE_URL/$projectName%7Cpages%7C"
        )
        val pageSizesPath = "$destDir/sizes_pages.tsv"
        logger.info { "<= $zipPath" }
        logger.info { "=> $pageSizesPath" }

        val pageSizesFile = Path(pageSizesPath)
        pageSizesFile.writeLines(listOf("file\twidth\theight"))
        val groups = FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath(zipPath)
            .groupBy { it.fileName.substringBeforeLast('-') }
        when (mode) {
            Mode.ENTRY -> groups.forEach { (entryName, facsimileDimensions) ->
                val manifestJson = manifestFactory.forEntry(entryName, facsimileDimensions)
                val outPath = "$destDir/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
                val pageSizeLines = facsimileDimensions
                    .map { fd -> "${fd.fileName.substringBeforeLast(".")}\t${fd.width}\t${fd.height}" }
                    .asSequence()
                pageSizesFile.appendLines(pageSizeLines)
            }

            Mode.PROJECT -> {
                groups.forEach { (_, facsimileDimensions) ->
                    val pageSizeLines = facsimileDimensions
                        .map { fd -> "${fd.fileName.substringBeforeLast(".")}\t${fd.width}\t${fd.height}" }
                        .asSequence()
                    pageSizesFile.appendLines(pageSizeLines)
                }
                val manifestJson = manifestFactory.forProject(projectName,groups)
                val outPath = "$destDir/$projectName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
            }
        }
    }

}
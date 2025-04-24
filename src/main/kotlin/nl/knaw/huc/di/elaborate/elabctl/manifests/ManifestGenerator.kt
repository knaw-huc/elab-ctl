package nl.knaw.huc.di.elaborate.elabctl.manifests

import kotlin.io.path.Path
import kotlin.io.path.appendLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory
import nl.knaw.huc.di.elaborate.elabctl.logger

object ManifestGenerator {

    fun generateFrom(zipPath: String, projectName: String) {
//        val projectName = "brieven-correspondenten-1900"
        val manifestFactory = ManifestV3Factory(
            "https://manifests.editem.huygens.knaw.nl/$projectName",
            "https://tt-iiif.dev.diginfra.org/iiif/3/$projectName%7Cpages%7C"
        )
        val pageSizesPath = "out/sizes_pages.tsv"
        logger.info { "<= $zipPath" }
        logger.info { "=> $pageSizesPath" }

        val pageSizesFile = Path(pageSizesPath)
        pageSizesFile.writeLines(listOf("file\twidth\theight"))
        FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath(zipPath)
            .groupBy { it.fileName.substringBeforeLast('-') }
            .forEach { (entryName, facsimileDimensions) ->
                val manifestJson = manifestFactory.forEntry(entryName, facsimileDimensions)
                val outPath = "out/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
                val pageSizeLines = facsimileDimensions
                    .map { fd -> "${fd.fileName.substringBeforeLast(".")}\t${fd.width}\t${fd.height}" }
                    .asSequence()
                pageSizesFile.appendLines(pageSizeLines)
            }
    }

}
package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.awt.Dimension
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileCacheImageInputStream
import org.junit.jupiter.api.Test
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import org.apache.logging.log4j.kotlin.logger

class ManifestFactoryTest {

    @Test
    fun `test ManifestV2Factory`() {
        val json = ManifestV2Factory.createFromImages(listOf())
        println(json)
    }

    @Test
    fun `test ManifestV3Factory`() {
        val json = ManifestV3Factory.createFromImages(listOf())
        println(json)
    }

    @Test
    fun `test ManifestFactory manifest`() {
        val json = ManifestV2Factory.manifest()
        println(json)
    }

    @Test
    fun `test ManifestFactory v3 manifest`() {
        val json = ManifestV3Factory.manifest()
        println(json)
    }

    @Test
    fun `test reading facsimiles zip`() {
        val facsimileDimensions = readFacsimileDimensions()
        logger.info { "size=" + facsimileDimensions.toList().size }
    }

    data class FacsimileDimensions(
        val fileName: String,
        val width: Int,
        val height: Int
    )

    fun readFacsimileDimensions(): Sequence<FacsimileDimensions> =
        sequence {
            val zipFilePath = "facsimiles.zip"

            ZipFile(zipFilePath).use { zipFile ->
                zipFile
                    .entries()
                    .toList()
                    .filterNot { it.isDirectory }
                    .filter { it.name.startsWith("facsimiles/") }
                    .forEach { entry ->
                        zipFile.getInputStream(entry).use { inputStream ->
                            either {
                                val dimension = getImageDimension(inputStream, entry.name).bind()
                                FacsimileDimensions(
                                    fileName = entry.name.split("/").last(),
                                    width = dimension.width,
                                    height = dimension.height
                                )
                            }
                        }.fold(
                            { error -> throw error },
                            { facsimileDimensions -> yield(facsimileDimensions) }
                        )
                    }
            }
        }

    fun getImageDimension(inputStream: InputStream, fileName: String): Either<Exception, Dimension> {
        val suffix = fileName.split(".").last()
        val iterator = ImageIO.getImageReadersBySuffix(suffix)
        while (iterator.hasNext()) {
            val reader: ImageReader = iterator.next()
            try {
                val dimension = FileCacheImageInputStream(inputStream, null).use { stream ->
                    reader.setInput(stream)
                    val width: Int = reader.getWidth(reader.getMinIndex())
                    val height: Int = reader.getHeight(reader.getMinIndex())
                    Dimension(width, height)
                }
                return Right(dimension)
            } catch (e: Exception) {
                return Left(e)
            } finally {
                reader.dispose()
            }
        }

        return Left(IOException("Not a known image file: $fileName"))
    }
}
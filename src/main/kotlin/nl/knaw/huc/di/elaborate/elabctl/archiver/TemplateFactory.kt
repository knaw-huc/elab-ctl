package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.io.FileWriter
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import com.github.mustachejava.DefaultMustacheFactory

object TemplateFactory {
    val mustacheFactory = DefaultMustacheFactory()

    fun expandToString(template: String, data: Map<String, Any>): String {
        val writer: Writer = StringWriter()
        compileAndExecute(template, writer, data)
        return writer.toString()
    }

    fun expandToFile(template: String, data: Map<String, Any>, path: String) {
        val writer: Writer = FileWriter(path)
        compileAndExecute(template, writer, data)
    }

    private fun compileAndExecute(
        template: String,
        writer: Writer,
        data: Map<String, Any>
    ) {
        mustacheFactory
            .compile(StringReader(template), "")
            .execute(writer, data)
        writer.flush()
    }
}

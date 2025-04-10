package nl.knaw.huc.di.elaborate.elabctl

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.archiver.TemplateFactory

class TemplateTest {

    @Test
    fun `test mustache template to string`() {
        val template = "And {{nested.key}} said: {{greeting}}, {{#names}}{{.}}, {{/names}}!"
        val map = mapOf(
            "greeting" to "Hello",
            "names" to listOf("world", "anybody", "everybody"),
            "nested" to mapOf("key" to "it")
        )
        val result = TemplateFactory.expandToString(template, map)
        val expectation = "And it said: Hello, world, anybody, everybody, !"
        assertEquals(expectation, result)
    }

    @Test
    fun `test mustache template to file`() {
        val template = "And {{nested.key}} said: {{greeting}}, {{#names}}{{.}}, {{/names}}!"
        val map = mapOf(
            "greeting" to "Hello",
            "names" to listOf("world", "anybody", "everybody"),
            "nested" to mapOf("key" to "it")
        )
        val path = "out/out.txt"
        TemplateFactory.expandToFile(template, map, path)
        val expectation = "And it said: Hello, world, anybody, everybody, !"
        val result = Path(path).readText()
        assertEquals(expectation, result)
    }

    @Test
    fun `test mustache template expansion with missing variables`() {
        val template = "And {{nested.key}} said: {{greeting}}, {{#names}}{{.}}, {{/names}}!"
        val map = mapOf(
            "greeting" to "Hello",
            "names" to listOf("world", "anybody", "everybody"),
        )
        val result = TemplateFactory.expandToString(template, map)
        val expectation = "And  said: Hello, world, anybody, everybody, !"
        assertEquals(expectation, result)
    }

}
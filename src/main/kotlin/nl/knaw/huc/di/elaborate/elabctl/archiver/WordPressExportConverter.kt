package nl.knaw.huc.di.elaborate.elabctl.archiver

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.io.path.Path
import kotlin.io.path.writeText
import org.apache.logging.log4j.kotlin.logger
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class WordPressExportConverter(private val outputDir: String) {
    object WordPressExportNamespaceContext : NamespaceContext {
        val namespaceMap = mapOf(
            "excerpt" to "http://wordpress.org/export/1.2/excerpt/",
            "content" to "http://purl.org/rss/1.0/modules/content/",
            "dc" to "http://purl.org/dc/elements/1.1/",
            "wp" to "http://wordpress.org/export/1.2/"
        )
        val reverseMap = namespaceMap.map { it.value to it.key }.toMap()

        override fun getNamespaceURI(prefix: String?): String? =
            when (prefix) {
                in namespaceMap -> namespaceMap[prefix]
                else -> null
            }

        override fun getPrefix(namespaceURI: String?): String? =
            when (namespaceURI) {
                in reverseMap -> reverseMap[namespaceURI]
                else -> null
            }

        override fun getPrefixes(namespaceURI: String?): Iterator<String?>? =
            when (namespaceURI) {
                in reverseMap -> listOf(reverseMap[namespaceURI]).iterator()
                else -> null
            }

    }

    val xpath: XPath = XPathFactory.newInstance().newXPath().apply {
        namespaceContext = WordPressExportNamespaceContext
    }

    fun convert(xmlPath: String): List<String> {
        val errors = mutableListOf<String>()
        val builder = DocumentBuilderFactory
            .newInstance()
            .apply { this.isNamespaceAware = true }
            .newDocumentBuilder()
        logger.info { "<= $xmlPath" }
        builder.parse(xmlPath).let { doc ->
            val nodes = doc.getNodeList("//item")
            for (i in 0..<nodes.length) {
                val itemNode = nodes.item(i)
                val link = itemNode.getNode("./link")?.textContent
                val title = itemNode.getNode("./title")?.textContent
                val content = itemNode.getNode("./content:encoded")?.textContent
                if (content?.isNotBlank() ?: false) {
                    logger.info { "item $i:\n  title=$title\n  link=$link\n  content='$content'" }
                }
            }
            logger.info { "${nodes.length} item nodes found" }
        }

        val outPath = "$outputDir/about.xml"
        logger.info { "=> $outPath" }
        Path(outPath).writeText("<xml/>")

        return errors
    }

    private fun Node.getNodeList(xpathExpression: String): NodeList =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NODESET) as NodeList

    private fun Node.getNode(xpathExpression: String): Node? =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NODE) as? Node

    private fun Node.getNumber(xpathExpression: String): Number? =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NUMBER) as? Number

    private fun Node.getString(xpathExpression: String): String? =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.STRING) as? String

    private fun Node.getBoolean(xpathExpression: String): Boolean? =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.BOOLEAN) as? Boolean
}
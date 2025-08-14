package nl.knaw.huc.di.elaborate.elabctl.archiver

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.io.path.Path
import kotlin.io.path.writeText
import org.apache.logging.log4j.kotlin.logger
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
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
            doc.getNodeSequence("//item")
                .filter { node -> node.hasContent() }
                .filter { node -> !node.postName().startsWith("hello-world") }
                .filter { node -> node.status() == "publish" }
                .forEach { itemNode ->
                    val content = itemNode.getString("./content:encoded/text()")
                    val link = itemNode.getString("./link/text()")
                    val creator = itemNode.getString("./dc:creator/text()")
                    val title = itemNode.getString("./title/text()")
                    val postName = itemNode.postName()
                    val lastModified = itemNode.getString("./wp:post_modified/text()")
//                    logNode(title, lastModified, link, postName, creator, content)
                    val xmlSrc = buildXML(content, link, creator, title, lastModified)
                    val outPath = "$outputDir/$postName.xml"
                    logger.info { "=> $outPath" }
                    Path(outPath).writeText(xmlSrc)
                }
        }

        return errors
    }

    val printOptions = PrintOptions(
        singleLineTextElements = true,
        indent = "  ",
        useSelfClosingTags = true
    )

    private fun buildXML(content: String, link: String, creator: String, title: String, lastModified: String): String =
        xml("TEI") {
            "text" {
                comment("link = $link")
                comment("creator = $creator")
                comment("title = $title")
                comment("lastModified = $lastModified")
                "body" {
                    unsafeText(content)
                }
            }
        }.toString(printOptions = printOptions)

    private fun logNode(
        title: String,
        lastModified: String,
        link: String,
        postName: String,
        creator: String,
        content: String
    ) {
        logger.info {
            """item:
      title=$title
      modified=$lastModified
      link=$link
      post-name='$postName'
      creator='$creator'
      content='$content'"""
        }
    }

    private fun Node.postName(): String =
        getString("./wp:post_name/text()")

    private fun Node.status(): String =
        getString("./wp:status/text()")

    private fun Node.hasContent(): Boolean =
        getString("./content:encoded/text()").isNotBlank()

    private fun NodeList.asSequence(): Sequence<Node> =
        sequence {
            for (i in 0..<this@asSequence.length) {
                yield(item(i))
            }
        }

    private fun Node.getNodeSequence(xpathExpression: String): Sequence<Node> =
        this.getNodeList(xpathExpression).asSequence()

    private fun Node.getNodeList(xpathExpression: String): NodeList =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NODESET) as NodeList

    private fun Node.getNode(xpathExpression: String): Node =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NODE) as Node

    private fun Node.getNumber(xpathExpression: String): Number =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.NUMBER) as Number

    private fun Node.getString(xpathExpression: String): String =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.STRING) as String

    private fun Node.getBoolean(xpathExpression: String): Boolean =
        xpath.compile(xpathExpression)
            .evaluate(this, XPathConstants.BOOLEAN) as Boolean
}
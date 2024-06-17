package nl.knaw.huc.di.elaborate.elabctl

import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

private const val XML_CLOSE_TAG = "</xml>"
private const val XML_OPEN_TAG = "<xml>"

fun unwrapFromXml(xml: String): String =
    xml.replaceFirst(XML_OPEN_TAG.toRegex(), "")
        .replaceFirst(XML_CLOSE_TAG.toRegex(), "")
        .replace("&apos;".toRegex(), "'")

fun wrapInXml(xmlContent: String): String =
    "$XML_OPEN_TAG$xmlContent$XML_CLOSE_TAG"

fun isWellFormed(body: String): Boolean {
    try {
        val dh = DefaultHandler()
        val stringReader = StringReader(body)
        val inputSource = InputSource(stringReader)
        SAXParserFactory
            .newInstance()
            .newSAXParser()
            .parse(inputSource, dh)
    } catch (e1: ParserConfigurationException) {
        e1.printStackTrace()
        return false
    } catch (e1: SAXException) {
        e1.printStackTrace()
        logger.error { "body=$body" }
        return false
    } catch (e: IOException) {
        e.printStackTrace()
        return false
    }
    return true
}

fun fixXhtml(badXml: String): String {
    val doc = Jsoup.parse(badXml)
    doc.outputSettings().indentAmount(0).prettyPrint(false).escapeMode(Entities.EscapeMode.xhtml).charset("UTF-8")
    return doc.body()
        .html()
        .replace(" />", "/>")
        .replace("\u00A0", "&#160;")
        .replace("<br>", "<br/>")
    // return Jsoup.clean(badxml, Whitelist.relaxed());
}


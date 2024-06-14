package nl.knaw.huc.di.elaborate.elabctl

import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
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


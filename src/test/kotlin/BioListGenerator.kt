import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.writeText
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.XmlVersion
import org.redundent.kotlin.xml.xml

@Serializable
data class Person(
    @SerialName("@type")
    val type: String,
    @SerialName("_id")
    val id: String,
    @SerialName("^rev")
    val rev: Long,
    @SerialName("^deleted")
    val deleted: Boolean,
    @SerialName("^pid")
    val pid: String,
    @SerialName("^rdfAlternatives")
    val rdfAlternatives: List<String?>,
    @SerialName("@variationRefs")
    val variationRefs: List<VariationRef>,
    @SerialName("^modified")
    val modified: Modified,
    @SerialName("^created")
    val created: Created,
    val names: List<Name>,
    val birthDate: String? = null,
    val deathDate: String? = null,
    val gender: String,
    val name: String,
    val koppelnaam: String,
    val networkDomains: List<String>,
    val domains: List<String>,
    val subDomains: List<String>,
    val combinedDomains: List<String>,
    val characteristics: List<String>,
    val periodicals: List<String>,
    val memberships: List<String>,
    val biodesurl: String,
    val dbnlUrl: String,
    val verwijzingen: List<Verwijzing>? = listOf(),
    val notities: String,
    val opmerkingen: String,
    val aantekeningen: String,
    val altNames: List<AltName>? = listOf(),
    val cnwBirthYear: String? = null,
    val cnwDeathYear: String? = null,
    val relatives: List<String>? = listOf(),
    val birthdateQualifier: String? = null,
    val deathdateQualifier: String? = null,
    val shortDescription: String,
    @SerialName("@displayName")
    val displayName: String,
)

@Serializable
data class VariationRef(
    val id: String,
    val type: String,
)

@Serializable
data class Modified(
    val timeStamp: Long,
    val userId: String,
    val vreId: String?,
    val username: String,
)

@Serializable
data class Created(
    val timeStamp: Long,
    val userId: String,
    val vreId: String?,
    val username: String,
)

@Serializable
data class Name(
    val components: List<Component>,
)

@Serializable
data class Verwijzing(
    val url: String,
    val identifier: String,
    val soort: String,
    val opmerkingen: String
)

@Serializable
data class Component(
    val type: String,
    val value: String,
)

@Serializable
data class AltName(
    val nametype: String,
    val displayName: String,
)

object App {
    @OptIn(ExperimentalSerializationApi::class)
    fun run() {
        val path = "data/brieven-correspondenten-1900-cnwpersons-dump.json"
        logger.info { "<= $path" }

        val persons = Json.decodeFromStream<List<Person>>(File(path).inputStream())
        val personXmlNodes = mutableListOf<Node>()
        val koppelnaamToPersonId = mutableMapOf<String, String>()
        persons.forEachIndexed { i, person ->
            val xmlId = """pers${(i + 1).toString().padStart(3, '0')}"""
            koppelnaamToPersonId[person.koppelnaam] = xmlId
            val xml = xml("person") {
                attribute("xml:id", xmlId)
                attribute("sex", person.sex())
                person.source()?.let { attribute("source", it) }
                person.names.forEach { name ->
                    val forename = name.nameComponent("FORENAME")
                    val surname = name.nameComponent("SURNAME")
                    val genName = name.nameComponent("GEN_NAME")
                    val nameLink = name.nameComponent("NAME_LINK")
                    "persName" {
                        attribute("full", "yes")
                        forename?.let { "forename" { -it } }
                        nameLink?.let { "nameLink" { -it } }
                        surname?.let { "surname" { -it } }
                        genName?.let { "addname" { -it } }
                    }
                }
                person.birthDate?.let {
                    "birth" { attribute("when", it) }
                }
                person.deathDate?.let {
                    "death" { attribute("when", it) }
                }
                "note" {
                    attribute("type", "shortdesc")
                    -person.shortDescription
                }
            }
            personXmlNodes.add(xml)
        }

        val xml = xml("TEI") {
            globalProcessingInstruction("editem", Pair("template", "biolist"))
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-biolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
            )
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-biolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
            )
            version = XmlVersion.V10
            encoding = "UTF-8"
            xmlns = "http://www.tei-c.org/ns/1.0"
            namespace("ed", "http://xmlschema.huygens.knaw.nl/ns/editem") // TODO: make conditional
            "teiHeader" {
                "fileDesc" {
                }
                "profileDesc" {
                }
            }
            "text" {
                "body" {
                    "listPerson" {
                        addElements(personXmlNodes)
                    }
                }
            }
        }

        val personIdPath = "out/person-ids.json"
        logger.info { "=> $personIdPath" }
        ObjectMapper().writeValue(File(personIdPath), koppelnaamToPersonId)

        val bioPath = "out/bio.xml"
        logger.info { "=> $bioPath" }
        Path(bioPath).writeText(xml.toString(true))

    }

    private fun Name.nameComponent(type: String): String? {
        val nameParts = components.filter { it.type == type }.map { it.value }
        return if (nameParts.isNotEmpty()) {
            nameParts.first()
        } else {
            null
        }
    }

    val genderToSex = mapOf(
        "MALE" to 1,
        "FEMALE" to 2,
        "UNKNOWN" to 9
    )

    private fun Person.sex(): Int = genderToSex[gender] ?: 9

    private fun Person.source(): String? =
        when {
            dbnlUrl.isNotEmpty() -> dbnlUrl
            biodesurl.isNotEmpty() -> biodesurl
            else -> null
        }

}

fun main() {
    App.run()
}

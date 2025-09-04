package nl.knaw.huc.di.elaborate.elabctl

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.archiver.AnnotationData
import nl.knaw.huc.di.elaborate.elabctl.archiver.AnnotationType
import nl.knaw.huc.di.elaborate.elabctl.archiver.TranscriptionVisitor
import nl.knaw.huc.di.elaborate.elabctl.archiver.isWellFormed
import nl.knaw.huc.di.elaborate.elabctl.archiver.unwrapFromXml
import nl.knaw.huc.di.elaborate.elabctl.archiver.wrapInXml
import nl.knaw.huygens.tei.Document

class TEIBuilderTest {

    private val text =
        """Mons[ieu]r mon bon amis sen partant se porter nepuis fallir de faire se mot pour vous salluer des recommandations de ma femme et de moij comme de mes enfans et comme je neij a vous dire recommandes quemporte ie vous vous feraij doncques part du fet de nre sante qu'est bonne de tout mon menaige sauff ma fillie Petronelle que ne se portte trop bien nous avons depuis peu perdu <span data-id="9000491" data-marker="begin" data-param-person_id="PE00011543" data-type="nl.knaw.huc.di.elaborate.elabctl.apparatus.Person"></span>ma cousine de Van der Aa<sup data-id="9000491" data-marker="end">1</sup> vre bonne amie se me serat plaisir sil vous plet me faire part de vre santte et residence ie ne vous escripts aucune nouvelle pour ne avoir point de bonnes que sont seures iaij quelcq<sup>ue </sup>fois espere vre venue en ce paijs et certtes me seroit plaisir de vous voire et vous complaire en quelcque chose pour lobligation que ie vous aij pour lassistence que vous aves donne aux miens durant nre premiere affliction a tant me recommande avecque ma femme et enfans a vre bonne grace priant a dieu vous conserver<br> <sup><br> </sup>Mons[ieu]r et bon amis en sa s<sup>te</sup> garde de Laijden ce IIII de desembre 1590<br> <br> <span data-id="9091281" data-marker="begin" data-param-person_id="PE00011357" data-type="nl.knaw.huc.di.elaborate.elabctl.apparatus.Person"></span>Hoghelande <sup data-id="9091281" data-marker="end">2</sup>qui me donne commodite descrire vous voudrat sil vous plet commodite de me recommandre<br> <br> vre bien bon amis a vous servir<br> <span data-id="9091282" data-marker="begin" data-param-person_id="PE00011313" data-type="nl.knaw.huc.di.elaborate.elabctl.apparatus.Person"></span>g Vander <span>Aa<sup data-id="9091282" data-marker="end">3</sup></span>"""

    @Test
    fun `test TranscriptionVisitor`() {
        val annotationMap = mapOf(
            9000491L to AnnotationData(
                annotatedText = "",
                n = 1,
                text = "something",
                type = AnnotationType(
                    1,
                    "nl.knaw.huc.di.elaborate.elabctl.apparatus.Person",
                    "description",
                    mapOf("person_id" to "PE00011543")
                )
            ),
            9091281L to AnnotationData(
                annotatedText = "",
                n = 2,
                text = "something",
                type = AnnotationType(
                    1,
                    "nl.knaw.huc.di.elaborate.elabctl.apparatus.Person",
                    "description",
                    mapOf("person_id" to "PE00011357")
                )
            ),
            9091282L to AnnotationData(
                annotatedText = "",
                n = 3,
                text = "something",
                type = AnnotationType(
                    1,
                    "nl.knaw.huc.di.elaborate.elabctl.apparatus.Person",
                    "description",
                    mapOf("person_id" to "PE00011313")
                )
            )
        )
        val visitor = TranscriptionVisitor(annotationMap = annotationMap, annoNumToRefTarget = mapOf())
        val prepared = text.replace("<br>", "<br/>\n")
        val wrapped = prepared.wrapInXml()
        val doc = Document.createFromXml(wrapped, false)
        doc.accept(visitor)
        val context = visitor.context
        val result = context.result
        println("result=" + result.unwrapFromXml())
        assertTrue { result.isWellFormed() }
    }
}
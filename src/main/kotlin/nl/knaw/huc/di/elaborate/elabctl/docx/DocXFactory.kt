package nl.knaw.huc.di.elaborate.elabctl.docx

import java.io.File
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.EndnotesPart

object DocXFactory {
    fun generate() {
        val wordPackage = WordprocessingMLPackage.createPackage()

        wordPackage.mainDocumentPart.apply {
            addParagraphOfText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam volutpat consectetur sem ac pellentesque. Morbi nunc tellus, sagittis quis justo sit amet, molestie euismod justo. Donec at arcu elit. Suspendisse nulla metus, congue quis volutpat vitae, euismod vel metus. Pellentesque condimentum arcu enim, et suscipit velit cursus nec. Nulla a molestie neque. Quisque non orci non lorem dignissim tempus a a ipsum. Cras id lobortis eros, ut volutpat libero. Proin sit amet eleifend ligula. Etiam commodo leo vitae dui efficitur rhoncus. Etiam vitae bibendum ante. Sed nunc sem, convallis in viverra sed, egestas sed nunc. Aenean fringilla justo ut augue euismod pellentesque. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.")
            addParagraphOfText("Etiam id accumsan justo. Nullam et nibh tortor. Curabitur sed nisl sit amet eros sagittis efficitur. Vivamus at mollis elit. Etiam vehicula eros eu erat tempor posuere. Nunc dignissim sapien at volutpat faucibus. Donec fringilla lorem at hendrerit molestie. Pellentesque viverra neque sit amet eleifend blandit. Sed congue nec augue eget pharetra.")

            val endNotesPart = endNotesPart
            val enp = EndnotesPart()
            addTargetPart(enp)

        }
        val exportFile = File("welcome.docx")
        wordPackage.save(exportFile)
    }
}
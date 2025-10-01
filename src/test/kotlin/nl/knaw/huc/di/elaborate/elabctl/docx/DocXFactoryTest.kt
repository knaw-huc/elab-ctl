package nl.knaw.huc.di.elaborate.elabctl.docx

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.commons.validator.GenericValidator

class DocXFactoryTest {
    @Test
    fun `generate docx`() {
        val docx = DocXFactory.generate()
        assertNotNull(docx)
    }

    @Test
    fun `validate dates`() {
        val incorrectDate = "1887-11-31"
        val correctDate = "1887-11-30"
        assertTrue(GenericValidator.isDate("2019-02-28", "yyyy-MM-dd", true));
        assertFalse(GenericValidator.isDate("2019-02-29", "yyyy-MM-dd", true));
        assertTrue(GenericValidator.isDate(correctDate, "yyyy-MM-dd", true));
        assertFalse(GenericValidator.isDate(incorrectDate, "yyyy-MM-dd", true));
    }

}
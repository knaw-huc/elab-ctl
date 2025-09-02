package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DateRegexTest {

    @Test
    fun `valid date detection`() {
        assertEquals(DateRegex.VALID_DATE, DateRegex.detect("2023-09-02"))
        assertTrue(DateRegex.VALID_DATE.matches("2023-09-02"))
    }

    @Test
    fun `uncertain month date detection`() {
        assertEquals(DateRegex.UNCERTAIN_MONTH_DATE, DateRegex.detect("1893-XX-XX"))
    }

    @Test
    fun `uncertain day date detection`() {
        assertEquals(DateRegex.UNCERTAIN_DAY_DATE, DateRegex.detect("1893-02-XX"))
    }

    @Test
    fun `date range detection`() {
        assertEquals(DateRegex.DATE_RANGE, DateRegex.detect("1893-01-01+1893-12-31"))
    }

    @Test
    fun `day range detection`() {
        assertEquals(DateRegex.DAY_RANGE, DateRegex.detect("1893-03-06_07"))
        assertEquals(DateRegex.DAY_RANGE, DateRegex.detect("1895-09-22+26"))
    }

    @Test
    fun `year range detection`() {
        assertEquals(DateRegex.YEAR_RANGE, DateRegex.detect("1893_1894-XX-XX"))
    }

    @Test
    fun `month range detection`() {
        assertEquals(DateRegex.MONTH_RANGE, DateRegex.detect("1893-01_04-XX"))
    }

    @Test
    fun `unknown month detection`() {
        assertEquals(DateRegex.UNKNOWN_MONTH, DateRegex.detect("1893-XX-23"))
    }

    @Test
    fun `unknown date detection`() {
        assertEquals(DateRegex.UNKNOWN_DATE, DateRegex.detect("XXXX-XX-XX"))
    }

    @Test
    fun `invalid date returns null`() {
        assertNull(DateRegex.detect("not-a-date"))
    }

    @ParameterizedTest(name = "{0} → notBefore={1}, notAfter={2}")
    @CsvSource(
        // UNCERTAIN_MONTH_DATE
        "1893-XX-XX,1893-01-01,1893-12-31",

        // UNCERTAIN_DAY_DATE (non-leap)
        "2023-02-XX,2023-02-01,2023-02-28",

        // UNCERTAIN_DAY_DATE (leap)
        "2024-02-XX,2024-02-01,2024-02-29",

        // DATE_RANGE
        "1893-01-01+1893-12-31,1893-01-01,1893-12-31",

        // DAY_RANGE1
        "1893-03-06_07,1893-03-06,1893-03-07",

        // DAY_RANGE2
        "1895-09-22_26,1895-09-22,1895-09-26",

        // YEAR_RANGE
        "1893_1894-XX-XX,1893-01-01,1894-12-31",

        // MONTH_RANGE
        "1893-01_04-XX,1893-01-01,1893-04-30",

        // UNKNOWN_MONTH
        "1893-XX-23,1893-01-23,1893-12-23",

        // UNKNOWN_DATE
        "XXXX-XX-XX,1877-01-01,1917-12-31",

        "1891-01-08+09,1891-01-08,1891-01-09",
        "188X-XX-XX,1880-01-01,1889-12-31",
        "1894-08-19_1894-09-24,1894-08-19,1894-09-24",
        "1887-10-XX _1887-11-XX,1887-10-01,1887-11-30",
        "1889_1890-03-11,1889-03-11,1890-03-11",
        "XXXX-12-XX,1877-12-01,1917-12-31",
        "1890_1895-12-XX,1890-12-01,1895-12-31"
    )

    fun `test getDateAttributes`(input: String, expectedNotBefore: String, expectedNotAfter: String) {
        val result = DateRegex.getDateAttributes(input)
        assertEquals(expectedNotBefore, result["notBefore"], "wrong notBefore for $input")
        assertEquals(expectedNotAfter, result["notAfter"], "wrong notAfter for $input")
    }
}
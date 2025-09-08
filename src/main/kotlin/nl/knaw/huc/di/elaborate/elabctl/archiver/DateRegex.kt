package nl.knaw.huc.di.elaborate.elabctl.archiver

enum class DateRegex(val pattern: Regex) {
    UNCERTAIN_DATE(Regex("\\[\\d{4}-\\d{2}-\\d{2}]")),
    VALID_DATE(Regex("\\d{4}-\\d{2}-\\d{2}")),
    UNCERTAIN_DAY_DATE(Regex("\\d{4}-\\d{2}-XX")),           // "1893-02-XX"
    UNCERTAIN_MONTH_DATE(Regex("\\d{4}-XX-XX")),             // "1893-XX-XX"
    DATE_RANGE(Regex("\\d{4}-\\d{2}-\\d{2}[_+]\\d{4}-\\d{2}-\\d{2}")),
    DATE_RANGE_UNKNOWN_DAY_2(Regex("\\d{4}-\\d{2}-\\d{2}\\+\\d{4}-\\d{2}-XX")),
    DAY_RANGE(Regex("\\d{4}-\\d{2}-\\d{2}[_+]\\d{2}")),        // "1893-03-06_07"
    YEAR_RANGE(Regex("\\d{4}_\\d{4}-XX-XX")),                // "1893_1894-XX-XX"
    MONTH_RANGE(Regex("\\d{4}-\\d{2}_\\d{2}-XX")),           // "1893-01_04-XX"
    UNKNOWN_MONTH(Regex("\\d{4}-XX-\\d{2}")),                // "1893-XX-23"
    UNKNOWN_DATE(Regex("XXXX-XX-XX")),
    PARTIALLY_UNKNOWN_YEAR(Regex("\\d{3}X-XX-XX")),
    UNKNOWN_YEAR(Regex("\\d{4}_\\d{4}-[\\dX]{2}-[\\dX]{2}")),
    DATE_RANGE_2(Regex("[\\dX]{4}-[\\dX]{2}-[\\dX]{2}[_+][\\dX]{4}-[\\dX]{2}-[\\dX]{2}")),
    JUST_THE_MONTH(Regex("XXXX-\\d{2}-XX"));

    fun matches(input: String): Boolean = pattern.matches(input)

    companion object {
        fun detect(input: String): DateRegex? =
            entries.firstOrNull { it.matches(input) }
    }

}


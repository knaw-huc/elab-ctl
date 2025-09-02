package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.YearMonth
import org.apache.logging.log4j.kotlin.logger

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

        fun getDateAttributes(date: String): Map<String, String> {
            val firstYear = 1877
            val lastYear = 1917
            return when (detect(date.replace(" ", ""))) {
                VALID_DATE, UNCERTAIN_DATE -> mapOf("when" to date)

                UNCERTAIN_MONTH_DATE -> {
                    val year = date.take(4)
                    mapOf(
                        "notBefore" to "$year-01-01",
                        "notAfter" to "$year-12-31"
                    )
                }

                UNCERTAIN_DAY_DATE -> {
                    val year = date.take(4)
                    val month = date.substring(5, 7).toInt()
                    val ym = YearMonth.of(year.toInt(), month)
                    mapOf(
                        "notBefore" to "$year-${"%02d".format(month)}-01",
                        "notAfter" to "$year-${"%02d".format(month)}-${ym.lengthOfMonth()}"
                    )
                }

                DATE_RANGE -> {
                    val (notBefore, notAfter) = date.split("+", "_")
                    mapOf("notBefore" to notBefore, "notAfter" to notAfter)
                }

                DATE_RANGE_2 -> {
                    val (first, last) = date.split("+", "_")
                    val firstAttributes = getDateAttributes(first)
                    val lastAttributes = getDateAttributes(last)
                    val notBefore = firstAttributes["notBefore"] ?: firstAttributes["when"]!!
                    val notAfter = lastAttributes["notAfter"] ?: lastAttributes["when"]!!

                    mapOf(
                        "notBefore" to notBefore,
                        "notAfter" to notAfter
                    )
                }

                DATE_RANGE_UNKNOWN_DAY_2 -> {
                    val (notBefore, notAfter) = date.split("+")
                    val attributes = getDateAttributes(notAfter)
                    mapOf("notBefore" to notBefore, "notAfter" to attributes["notAfter"]!!)
                }

                DAY_RANGE -> {
                    val base = date.take(10) // yyyy-MM-dd
                    val extraDay = date.takeLast(2)
                    val notBefore = base
                    val notAfter = base.take(8) + extraDay
                    mapOf("notBefore" to notBefore, "notAfter" to notAfter)
                }

                YEAR_RANGE -> {
                    val (year1, rest) = date.split("_")
                    val year2 = rest.take(4)
                    mapOf(
                        "notBefore" to "$year1-01-01",
                        "notAfter" to "$year2-12-31"
                    )
                }

                MONTH_RANGE -> {
                    val year = date.take(4).toInt()
                    val m1 = date.substring(5, 7).toInt()
                    val m2 = date.substring(8, 10).toInt()
                    val ym2 = YearMonth.of(year, m2)
                    mapOf(
                        "notBefore" to "$year-${"%02d".format(m1)}-01",
                        "notAfter" to "$year-${"%02d".format(m2)}-${ym2.lengthOfMonth()}"
                    )
                }

                UNKNOWN_MONTH -> {
                    val year = date.take(4).toInt()
                    val day = date.takeLast(2).toInt()

                    val firstMonth = YearMonth.of(year, 1)
                    val lastMonth = YearMonth.of(year, 12)

                    val notBeforeDay = minOf(day, firstMonth.lengthOfMonth())
                    val notAfterDay = minOf(day, lastMonth.lengthOfMonth())

                    mapOf(
                        "notBefore" to "$year-01-${"%02d".format(notBeforeDay)}",
                        "notAfter" to "$year-12-${"%02d".format(notAfterDay)}"
                    )
                }

                PARTIALLY_UNKNOWN_YEAR -> {
                    val decade = date.take(3)
                    mapOf(
                        "notBefore" to "${decade}0-01-01",
                        "notAfter" to "${decade}9-12-31"
                    )
                }

                UNKNOWN_YEAR -> {
                    val years = date.take(9)
                    val rest = date.substring(10)
                    val (y1, y2) = years.split("_")
                    val d1 = "$y1-$rest"
                    val d2 = "$y2-$rest"
                    val firstAttributes = getDateAttributes(d1)
                    val lastAttributes = getDateAttributes(d2)
                    val notBefore = firstAttributes["notBefore"] ?: firstAttributes["when"]!!
                    val notAfter = lastAttributes["notAfter"] ?: lastAttributes["when"]!!
                    mapOf(
                        "notBefore" to notBefore,
                        "notAfter" to notAfter
                    )
                }

                JUST_THE_MONTH -> {
                    val month = date.substring(5, 7)
                    val ym2 = YearMonth.of(lastYear, month.toInt())

                    mapOf(
                        "notBefore" to "$firstYear-${month}-01",
                        "notAfter" to "$lastYear-${month}-${ym2.lengthOfMonth()}"
                    )
                }

                UNKNOWN_DATE ->
                    mapOf(
                        "notBefore" to "$firstYear-01-01",
                        "notAfter" to "$lastYear-12-31"
                    )

                null -> {
                    logger.warn { "unrecognized date pattern: $date" }
//                    throw RuntimeException("unrecognized date pattern: $date")
                    mapOf()
                }
            }
        }
    }

}

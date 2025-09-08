package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.YearMonth
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.config.LetterDateConfig

class DateAttributeFactory(val letterDateConfig: LetterDateConfig) {

    fun getDateAttributes(date: String): Map<String, String> {
        val firstYear = letterDateConfig.earliestYear
        val lastYear = letterDateConfig.latestYear
        return when (DateRegex.detect(date.replace(" ", ""))) {
            DateRegex.VALID_DATE, DateRegex.UNCERTAIN_DATE -> mapOf("when" to date)

            DateRegex.UNCERTAIN_MONTH_DATE -> {
                val year = date.take(4)
                mapOf(
                    "notBefore" to "$year-01-01",
                    "notAfter" to "$year-12-31"
                )
            }

            DateRegex.UNCERTAIN_DAY_DATE -> {
                val year = date.take(4)
                val month = date.substring(5, 7).toInt()
                val ym = YearMonth.of(year.toInt(), month)
                mapOf(
                    "notBefore" to "$year-${"%02d".format(month)}-01",
                    "notAfter" to "$year-${"%02d".format(month)}-${ym.lengthOfMonth()}"
                )
            }

            DateRegex.DATE_RANGE -> {
                val (notBefore, notAfter) = date.split("+", "_")
                mapOf("notBefore" to notBefore, "notAfter" to notAfter)
            }

            DateRegex.DATE_RANGE_2 -> {
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

            DateRegex.DATE_RANGE_UNKNOWN_DAY_2 -> {
                val (notBefore, notAfter) = date.split("+")
                val attributes = getDateAttributes(notAfter)
                mapOf("notBefore" to notBefore, "notAfter" to attributes["notAfter"]!!)
            }

            DateRegex.DAY_RANGE -> {
                val base = date.take(10) // yyyy-MM-dd
                val extraDay = date.takeLast(2)
                val notBefore = base
                val notAfter = base.take(8) + extraDay
                mapOf("notBefore" to notBefore, "notAfter" to notAfter)
            }

            DateRegex.YEAR_RANGE -> {
                val (year1, rest) = date.split("_")
                val year2 = rest.take(4)
                mapOf(
                    "notBefore" to "$year1-01-01",
                    "notAfter" to "$year2-12-31"
                )
            }

            DateRegex.MONTH_RANGE -> {
                val year = date.take(4).toInt()
                val m1 = date.substring(5, 7).toInt()
                val m2 = date.substring(8, 10).toInt()
                val ym2 = YearMonth.of(year, m2)
                mapOf(
                    "notBefore" to "$year-${"%02d".format(m1)}-01",
                    "notAfter" to "$year-${"%02d".format(m2)}-${ym2.lengthOfMonth()}"
                )
            }

            DateRegex.UNKNOWN_MONTH -> {
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

            DateRegex.PARTIALLY_UNKNOWN_YEAR -> {
                val decade = date.take(3)
                mapOf(
                    "notBefore" to "${decade}0-01-01",
                    "notAfter" to "${decade}9-12-31"
                )
            }

            DateRegex.UNKNOWN_YEAR -> {
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

            DateRegex.JUST_THE_MONTH -> {
                val month = date.substring(5, 7)
                val ym2 = YearMonth.of(lastYear, month.toInt())

                mapOf(
                    "notBefore" to "$firstYear-${month}-01",
                    "notAfter" to "$lastYear-${month}-${ym2.lengthOfMonth()}"
                )
            }

            DateRegex.UNKNOWN_DATE ->
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
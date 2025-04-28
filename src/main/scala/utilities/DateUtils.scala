package utilities

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random

object DateUtils {

  private def formatter(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)

  /** Returns the current date formatted */
  def getDateNow(format: String): String =
    LocalDate.now().format(formatter(format))

  /** Returns a past date by subtracting fixed years, months, days */
  def getDatePast(format: String, years: Int = 0, months: Int = 0, days: Int = 0): String =
    LocalDate.now().minusYears(years).minusMonths(months).minusDays(days).format(formatter(format))

  /** Returns a random past date by subtracting random years, months, days within provided ranges */
  def getDatePastRandom(format: String,
                        minYears: Int = 0, maxYears: Int = 0,
                        minMonths: Int = 0, maxMonths: Int = 0,
                        minDays: Int = 0, maxDays: Int = 0): String = {
    val years = randomBetween(minYears, maxYears)
    val months = randomBetween(minMonths, maxMonths)
    val days = randomBetween(minDays, maxDays)
    LocalDate.now().minusYears(years).minusMonths(months).minusDays(days).format(formatter(format))
  }

  /** Returns a future date by adding fixed years, months, days */
  def getDateFuture(format: String, years: Int = 0, months: Int = 0, days: Int = 0): String =
    LocalDate.now().plusYears(years).plusMonths(months).plusDays(days).format(formatter(format))

  /** Returns a random future date by adding random years, months, days within provided ranges */
  def getDateFutureRandom(format: String,
                          minYears: Int = 0, maxYears: Int = 0,
                          minMonths: Int = 0, maxMonths: Int = 0,
                          minDays: Int = 0, maxDays: Int = 0): String = {
    val years = randomBetween(minYears, maxYears)
    val months = randomBetween(minMonths, maxMonths)
    val days = randomBetween(minDays, maxDays)
    LocalDate.now().plusYears(years).plusMonths(months).plusDays(days).format(formatter(format))
  }

  /** Helper method to generate random value between min and max (inclusive) */
  private def randomBetween(min: Int, max: Int): Int =
    if (min == max) min else Random.between(min, max + 1)

}

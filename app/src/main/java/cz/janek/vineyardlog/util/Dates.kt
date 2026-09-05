package cz.janek.vineyardlog.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val fullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M. yyyy")
private val shortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M.")

fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)
fun LocalDate.toEpochDayLong(): Long = this.toEpochDay()
fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun formatDate(epochDay: Long): String = epochDay.toLocalDate().format(fullFormatter)
fun formatDateShort(epochDay: Long): String = epochDay.toLocalDate().format(shortFormatter)

fun yearOf(epochDay: Long): Int = epochDay.toLocalDate().year

fun formatDateTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d. M. yyyy HH:mm"))

/** Epoch day range covering the whole calendar year. */
fun yearRange(year: Int): LongRange =
    LocalDate.of(year, 1, 1).toEpochDay()..LocalDate.of(year, 12, 31).toEpochDay()

/** Day-of-year based on Jan 1 = 1. */
fun dayOfYear(epochDay: Long): Int = epochDay.toLocalDate().dayOfYear

/** Milliseconds (UTC) <-> epoch day for the Material DatePicker. */
fun epochDayToMillis(epochDay: Long): Long = epochDay * 86_400_000L
fun millisToEpochDay(millis: Long): Long = Math.floorDiv(millis, 86_400_000L)

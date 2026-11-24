package com.micontrolfinanciero.app.core.domain.finance

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** Zona horaria inicial del proyecto (configurable más adelante). */
val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Bogota")

/**
 * Un mes calendario convertido a milisegundos UTC: [startMillis] incluido, [endMillis] excluido.
 * Así "noviembre" son exactamente los movimientos del 1 de noviembre 00:00 al
 * 1 de diciembre 00:00 en hora de Colombia.
 */
data class MonthRange(
    val month: YearMonth,
    val startMillis: Long,
    val endMillis: Long,
) {
    operator fun contains(epochMillis: Long): Boolean = epochMillis in startMillis until endMillis

    fun previous(zone: ZoneId = DEFAULT_ZONE) = of(month.minusMonths(1), zone)
    fun next(zone: ZoneId = DEFAULT_ZONE) = of(month.plusMonths(1), zone)

    companion object {
        fun of(month: YearMonth, zone: ZoneId = DEFAULT_ZONE): MonthRange = MonthRange(
            month = month,
            startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        fun current(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = DEFAULT_ZONE): MonthRange =
            of(YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone)), zone)
    }
}

/** Conversión entre fecha + hora locales y milisegundos UTC. */
object LocalTimes {
    fun toMillis(date: LocalDate, time: LocalTime, zone: ZoneId = DEFAULT_ZONE): Long =
        LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

    fun toLocal(epochMillis: Long, zone: ZoneId = DEFAULT_ZONE): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()
}

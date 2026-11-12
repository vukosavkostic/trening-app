package com.vukos.trening.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Raspored merenja:
 *  - svake nedelje ujutru: kilaza
 *  - svake druge nedelje: kilaza + slike tela
 * Merenje je jednom sedmicno, termin je nedelja. Ciklus krece od [ANCHOR] (prvi dan sa slikama).
 *  - nedelja bez unosa  -> "danas je merenje" (obican podsetnik)
 *  - od ponedeljka, ako i dalje nije uneto -> crveno (overdue) dok se ne unese
 * Unos od nedelje do sledece subote se racuna za tu sedmicu.
 */
object CheckInSchedule {
    val ANCHOR: LocalDate = LocalDate.of(2026, 11, 15)
    val DAY: DayOfWeek = DayOfWeek.SUNDAY

    data class Due(
        val checkInDay: LocalDate,
        val photoDay: Boolean,
        val weightMissing: Boolean,
        val photosMissing: Boolean,
        /** true kad je nedelja prosla, a nesto i dalje fali (prikaz crveno). */
        val overdue: Boolean,
    ) {
        val anythingMissing get() = weightMissing || photosMissing
    }

    /** Poslednji dan merenja <= today, ili null ako ciklus jos nije poceo. */
    fun currentCheckInDay(today: LocalDate): LocalDate? {
        val last = today.with(TemporalAdjusters.previousOrSame(DAY))
        return if (last.isBefore(ANCHOR)) null else last
    }

    fun isPhotoDay(day: LocalDate): Boolean =
        !day.isBefore(ANCHOR) && day.dayOfWeek == DAY &&
            ChronoUnit.WEEKS.between(ANCHOR, day) % 2 == 0L

    /** Sledeci dan merenja >= today (za zakazivanje notifikacija). */
    fun nextCheckInDay(today: LocalDate): LocalDate {
        val next = today.with(TemporalAdjusters.nextOrSame(DAY))
        return if (next.isBefore(ANCHOR)) ANCHOR else next
    }

    fun due(today: LocalDate, hasWeight: Boolean, photoCount: Int): Due? {
        val day = currentCheckInDay(today) ?: return null
        val photo = isPhotoDay(day)
        val weightMissing = !hasWeight
        val photosMissing = photo && photoCount == 0
        val overdue = (weightMissing || photosMissing) && today.isAfter(day)
        return Due(day, photo, weightMissing, photosMissing, overdue)
    }

    /** Prozor [nedelja, subota] u kome se unos racuna za dati dan merenja. */
    fun window(checkInDay: LocalDate): Pair<LocalDate, LocalDate> = checkInDay to checkInDay.plusDays(6)

    /** Proverava bazu i vraca sta fali za tekuci dan merenja (null = nista nije zakazano). */
    suspend fun dueFromDb(dao: BodyDao, today: LocalDate = LocalDate.now()): Due? {
        val day = currentCheckInDay(today) ?: return null
        val (from, to) = window(day)
        val hasWeight = dao.entriesBetween(from.toEpochDay(), to.toEpochDay()).any { it.weightKg != null }
        val photos = dao.photoCountBetween(from.toEpochDay(), to.toEpochDay())
        return due(today, hasWeight, photos)
    }
}

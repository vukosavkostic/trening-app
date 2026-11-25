package com.vukos.trening.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vukos.trening.TreningApp

/**
 * Odmor izmedju setova. Zivi van ekrana (preziveti okretanje telefona i povratak na trening):
 * cuva se trenutak kraja, a preostalo vreme se racuna iz sata.
 */
object RestTimer {
    /** Kraj odmora (ms), 0 = nema odmora. */
    var endsAt by mutableLongStateOf(0L)
        private set
    /** Ukupno trajanje tekuceg odmora (s), za prsten i traku. */
    var totalSec by mutableStateOf(0)
        private set

    val defaultSec: Int get() = TreningApp.instance.settings.restDefaultSec

    fun remainingSec(now: Long = System.currentTimeMillis()): Int =
        if (endsAt == 0L) 0 else ((endsAt - now + 999) / 1000).toInt().coerceAtLeast(0)

    fun active(now: Long = System.currentTimeMillis()) = endsAt != 0L && endsAt > now

    fun start(sec: Int = defaultSec) {
        totalSec = sec
        endsAt = System.currentTimeMillis() + sec * 1000L
    }

    fun add(sec: Int) {
        val now = System.currentTimeMillis()
        if (!active(now)) { start(sec); return }
        val rem = remainingSec(now) + sec
        endsAt = now + rem * 1000L
        totalSec = maxOf(totalSec, rem)
    }

    fun subtract(sec: Int) {
        val now = System.currentTimeMillis()
        if (!active(now)) return
        val rem = (remainingSec(now) - sec).coerceAtLeast(1)
        endsAt = now + rem * 1000L
    }

    fun skip() { endsAt = 0L }

    /** Nova podrazumevana vrednost; ako tece odmor sa starom vrednoscu, prilagodi ga. */
    fun setDefault(sec: Int) {
        val old = defaultSec
        TreningApp.instance.settings.restDefaultSec = sec
        val now = System.currentTimeMillis()
        if (active(now) && totalSec == old) {
            val rem = minOf(remainingSec(now), sec)
            totalSec = sec
            endsAt = now + rem * 1000L
        }
    }

    val OPTIONS = listOf(45, 60, 90, 120, 150, 180)

    fun mmss(sec: Int): String = "${sec / 60}:${"%02d".format(sec % 60)}"
}

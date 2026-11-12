package com.vukos.trening.data

import java.time.DayOfWeek

/** Dani u nedelji kao bitmaska (bit 0 = ponedeljak ... bit 6 = nedelja), isti nazivi kao u Notionu. */
object Weekdays {
    val LABELS = listOf("Pon", "Uto", "Sre", "Čet", "Pet", "Sub", "Ned")
    val FULL = listOf("Ponedeljak", "Utorak", "Sreda", "Četvrtak", "Petak", "Subota", "Nedelja")

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun has(mask: Int, day: DayOfWeek) = mask and bit(day) != 0
    fun toggle(mask: Int, index: Int) = mask xor (1 shl index)

    fun fromLabels(labels: List<String>): Int =
        labels.fold(0) { m, l ->
            val i = LABELS.indexOfFirst { it.equals(l.trim(), ignoreCase = true) || l.trim().startsWith(it, ignoreCase = true) }
            if (i >= 0) m or (1 shl i) else m
        }

    fun toLabels(mask: Int): List<String> = LABELS.filterIndexed { i, _ -> mask and (1 shl i) != 0 }

    fun describe(mask: Int): String = if (mask == 0) "bez dana" else toLabels(mask).joinToString(", ")
}

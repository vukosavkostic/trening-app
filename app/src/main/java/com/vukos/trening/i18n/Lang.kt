package com.vukos.trening.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Jezik aplikacije (srpski / engleski). Stanje je "snapshot state", pa se ceo UI odmah
 * ponovo iscrtava na izabranom jeziku. Podaci (nazivi obroka, suplemenata...) se ne prevode.
 */
object Lang {
    var code by mutableStateOf("sr")
        private set
    val en: Boolean get() = code == "en"

    fun init(context: Context) {
        code = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("lang", "sr") ?: "sr"
    }

    fun set(context: Context, c: String) {
        code = c
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("lang", c).apply()
    }

    val label: String get() = if (en) "English" else "Srpski"
}

/** Tekst na trenutnom jeziku. */
fun tr(sr: String, en: String): String = if (Lang.en) en else sr

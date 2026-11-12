package com.vukos.trening.data

import com.vukos.trening.TreningApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Predlog brzog unosa: tekst + poslednje poznate kcal/protein + koliko puta je unet. */
data class FoodSuggestion(val text: String, val kcal: Int?, val proteinG: Int?, val count: Int, val meal: String)

object FoodQuick {
    /** Najcesci unosi (najpre za izabrani obrok), bez duplikata po tekstu. */
    fun suggestions(recent: List<FoodEntry>, meal: String, limit: Int = 8): List<FoodSuggestion> =
        recent.filter { it.meal != Supplements.MEAL }.groupBy { it.text.trim().lowercase() }
            .map { (_, list) ->
                val last = list.maxBy { it.createdAt }
                val topMeal = list.groupingBy { it.meal }.eachCount().maxBy { it.value }.key
                FoodSuggestion(last.text.trim(), last.kcal, last.proteinG, list.size, topMeal)
            }
            .sortedWith(compareByDescending<FoodSuggestion> { it.meal == meal }.thenByDescending { it.count })
            .take(limit)

    /** Kopira sve obroke sa dana [from] na dan [to]. Vraca broj kopiranih. */
    suspend fun copyDay(from: Long, to: Long, supplements: Boolean = false): Int {
        val dao = TreningApp.instance.db.food()
        val src = dao.getDay(from).filter { (it.meal == Supplements.MEAL) == supplements }
        val now = System.currentTimeMillis()
        dao.upsertAll(src.mapIndexed { i, f ->
            FoodEntry(epochDay = to, meal = f.meal, text = f.text, kcal = f.kcal, proteinG = f.proteinG, createdAt = now + i)
        })
        return src.size
    }
}

/** Proizvod iz Open Food Facts (vrednosti na 100 g). */
data class FoodProduct(val name: String, val kcal100: Double?, val protein100: Double?)

/** Besplatna otvorena baza proizvoda po barkodu (bez naloga i kljuca). */
object OpenFoodFacts {
    private val http = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    suspend fun lookup(barcode: String): FoodProduct? = withContext(Dispatchers.IO) {
        val code = barcode.filter(Char::isDigit).takeIf { it.length in 6..14 } ?: return@withContext null
        val req = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,product_name_sr,brands,nutriments")
            .header("User-Agent", "TreningApp/1.0 (licna aplikacija)")
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@withContext null
            parse(r.body!!.string())
        }
    }

    fun parse(json: String): FoodProduct? {
        val o = JSONObject(json)
        if (o.optInt("status", 0) != 1) return null
        val p = o.getJSONObject("product")
        val n = p.optJSONObject("nutriments") ?: JSONObject()
        val name = p.optString("product_name_sr").ifBlank { p.optString("product_name") }.trim()
        val brand = p.optString("brands").split(',').firstOrNull()?.trim().orEmpty()
        val full = listOf(name, brand).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifBlank { "Proizvod" }
        fun num(k: String) = if (n.has(k)) n.optDouble(k).takeIf { !it.isNaN() } else null
        val kcal = num("energy-kcal_100g") ?: num("energy_100g")?.let { it / 4.184 }
        return FoodProduct(full, kcal, num("proteins_100g"))
    }
}

package com.vukos.trening.data

/**
 * Suplementi se cuvaju kao unosi ishrane sa obrokom "Suplementi" (isti dan, ista sinhronizacija sa Notionom).
 * Tekst unosa: "Naziv · doza", npr. "Kreatin monohidrat · 5 g".
 */
data class SupplementDef(
    val name: String,
    val dose: String,
    val category: String,
    /** Za proteinske suplemente: vrednosti za podrazumevanu dozu. */
    val kcal: Int? = null,
    val proteinG: Int? = null,
    /** Dodatne reci za pretragu (engleski nazivi, skracenice). */
    val aliases: String = "",
)

object Supplements {
    const val MEAL = "Suplementi"
    const val SEP = " · "

    /** Naziv kategorije za prikaz (u podacima ostaje srpski). */
    fun categoryLabel(c: String): String = if (!com.vukos.trening.i18n.Lang.en) c else when (c) {
        "Performanse" -> "Performance"; "Protein" -> "Protein"; "Oporavak i san" -> "Recovery & sleep"
        "Vitamini i minerali" -> "Vitamins & minerals"; "Zdravlje i ostalo" -> "Health & other"; "Skoro korišćeno" -> "Recently used"
        else -> c
    }

    val CATEGORIES = listOf("Performanse", "Protein", "Oporavak i san", "Vitamini i minerali", "Zdravlje i ostalo")

    /** Najcesci trening suplementi (doze su uobicajene pocetne vrednosti; menjaju se pri unosu). */
    /** Spisak na jeziku aplikacije; na engleskom srpski naziv ostaje u aliasima za pretragu. */
    val LIST: List<SupplementDef> get() = if (!com.vukos.trening.i18n.Lang.en) LIST_SR else LIST_SR.map {
        it.copy(name = EN_NAMES[it.name] ?: it.name, dose = enDose(it.dose), aliases = it.aliases + " " + it.name)
    }

    private fun enDose(d: String): String = d.replace("1 merica", "1 scoop").replace("1 kesica", "1 sachet")
        .replace("1 kom", "1 pc").replace("1 doza", "1 serving").replace("1 tableta", "1 tablet")
        .replace("1 kapsula", "1 capsule").replace(Regex("(\\d),(\\d)"), "$1.$2")

    private val EN_NAMES = mapOf(
        "Kreatin monohidrat" to "Creatine monohydrate", "Kofein" to "Caffeine", "Beta-alanin" to "Beta-alanine",
        "Citrulin malat" to "Citrulline malate", "L-arginin" to "L-arginine", "Betain" to "Betaine", "Taurin" to "Taurine",
        "Elektroliti" to "Electrolytes", "Sok od cvekle (nitrati)" to "Beetroot juice (nitrates)",
        "Natrijum bikarbonat" to "Sodium bicarbonate", "Ugljeni hidrati (dekstrin)" to "Carbs (dextrin)",
        "Whey izolat" to "Whey isolate", "Kazein" to "Casein", "Biljni protein" to "Plant protein",
        "Goveđi protein" to "Beef protein", "Proteinska čokoladica" to "Protein bar", "Kolagen" to "Collagen",
        "Glutamin" to "Glutamine", "Ašvaganda (KSM-66)" to "Ashwagandha (KSM-66)", "Sok od višnje" to "Tart cherry juice",
        "Kurkumin" to "Curcumin", "Glicin" to "Glycine", "L-teanin" to "L-theanine", "Omega-3 (riblje ulje)" to "Omega-3 (fish oil)",
        "Magnezijum (bisglicinat)" to "Magnesium (bisglycinate)", "Cink" to "Zinc", "Gvožđe" to "Iron", "Kalcijum" to "Calcium",
        "Kalijum" to "Potassium", "B kompleks" to "B complex", "Selen" to "Selenium", "Probiotik" to "Probiotic",
        "Psilijum (vlakna)" to "Psyllium (fiber)", "L-karnitin" to "L-carnitine", "Koenzim Q10" to "Coenzyme Q10",
        "Glukozamin" to "Glucosamine",
    )

    private val LIST_SR = listOf(
        // Performanse
        SupplementDef("Kreatin monohidrat", "5 g", "Performanse", aliases = "creatine kreatin"),
        SupplementDef("Kofein", "200 mg", "Performanse", aliases = "caffeine kafa"),
        SupplementDef("Pre-workout", "1 merica", "Performanse", aliases = "preworkout pre workout predtrening"),
        SupplementDef("Beta-alanin", "3,2 g", "Performanse", aliases = "beta alanine"),
        SupplementDef("Citrulin malat", "6 g", "Performanse", aliases = "citrulline pump"),
        SupplementDef("L-arginin", "3 g", "Performanse", aliases = "arginine"),
        SupplementDef("Betain", "2,5 g", "Performanse", aliases = "betaine tmg"),
        SupplementDef("Taurin", "2 g", "Performanse", aliases = "taurine"),
        SupplementDef("Elektroliti", "1 kesica", "Performanse", aliases = "electrolytes natrijum so"),
        SupplementDef("Sok od cvekle (nitrati)", "500 mg", "Performanse", aliases = "beetroot nitrate cvekla"),
        SupplementDef("Natrijum bikarbonat", "20 g", "Performanse", aliases = "sodium bicarbonate soda bikarbona"),
        SupplementDef("Ugljeni hidrati (dekstrin)", "30 g", "Performanse", kcal = 115, aliases = "carbs cyclic dextrin maltodekstrin"),
        // Protein
        SupplementDef("Whey protein", "30 g", "Protein", kcal = 120, proteinG = 24, aliases = "protein surutka koncentrat"),
        SupplementDef("Whey izolat", "30 g", "Protein", kcal = 110, proteinG = 27, aliases = "isolate protein surutka"),
        SupplementDef("Kazein", "30 g", "Protein", kcal = 110, proteinG = 24, aliases = "casein protein nocni"),
        SupplementDef("Biljni protein", "30 g", "Protein", kcal = 115, proteinG = 21, aliases = "vegan plant protein grasak pirinac"),
        SupplementDef("Goveđi protein", "30 g", "Protein", kcal = 110, proteinG = 25, aliases = "beef protein"),
        SupplementDef("Gainer", "100 g", "Protein", kcal = 380, proteinG = 30, aliases = "mass gainer masa"),
        SupplementDef("Proteinska čokoladica", "1 kom", "Protein", kcal = 200, proteinG = 20, aliases = "protein bar plocica"),
        SupplementDef("EAA", "10 g", "Protein", aliases = "esencijalne amino kiseline amino"),
        SupplementDef("BCAA", "5 g", "Protein", aliases = "amino kiseline leucin"),
        SupplementDef("Kolagen", "10 g", "Protein", kcal = 36, proteinG = 9, aliases = "collagen peptidi"),
        // Oporavak i san
        SupplementDef("Glutamin", "5 g", "Oporavak i san", aliases = "glutamine"),
        SupplementDef("HMB", "3 g", "Oporavak i san"),
        SupplementDef("ZMA", "1 doza", "Oporavak i san", aliases = "cink magnezijum b6"),
        SupplementDef("Ašvaganda (KSM-66)", "600 mg", "Oporavak i san", aliases = "ashwagandha asvaganda"),
        SupplementDef("Melatonin", "1 mg", "Oporavak i san", aliases = "san sleep"),
        SupplementDef("Sok od višnje", "30 ml", "Oporavak i san", aliases = "tart cherry visnja"),
        SupplementDef("Kurkumin", "500 mg", "Oporavak i san", aliases = "curcumin kurkuma turmeric"),
        SupplementDef("Glicin", "3 g", "Oporavak i san", aliases = "glycine"),
        SupplementDef("L-teanin", "200 mg", "Oporavak i san", aliases = "theanine"),
        // Vitamini i minerali
        SupplementDef("Omega-3 (riblje ulje)", "2 g", "Vitamini i minerali", aliases = "omega 3 fish oil epa dha"),
        SupplementDef("Vitamin D3", "2000 IU", "Vitamini i minerali", aliases = "d3 d vitamin"),
        SupplementDef("Vitamin K2", "100 µg", "Vitamini i minerali", aliases = "k2 mk7"),
        SupplementDef("Magnezijum (bisglicinat)", "300 mg", "Vitamini i minerali", aliases = "magnesium magnezijum citrat"),
        SupplementDef("Cink", "15 mg", "Vitamini i minerali", aliases = "zinc"),
        SupplementDef("Gvožđe", "18 mg", "Vitamini i minerali", aliases = "iron gvozdje"),
        SupplementDef("Kalcijum", "500 mg", "Vitamini i minerali", aliases = "calcium"),
        SupplementDef("Kalijum", "99 mg", "Vitamini i minerali", aliases = "potassium"),
        SupplementDef("Multivitamin", "1 tableta", "Vitamini i minerali", aliases = "multi vitamini"),
        SupplementDef("Vitamin C", "500 mg", "Vitamini i minerali", aliases = "c vitamin"),
        SupplementDef("B kompleks", "1 tableta", "Vitamini i minerali", aliases = "b complex b vitamini"),
        SupplementDef("Vitamin B12", "1000 µg", "Vitamini i minerali", aliases = "b12 kobalamin"),
        SupplementDef("Selen", "55 µg", "Vitamini i minerali", aliases = "selenium"),
        // Zdravlje i ostalo
        SupplementDef("Probiotik", "1 kapsula", "Zdravlje i ostalo", aliases = "probiotic"),
        SupplementDef("Psilijum (vlakna)", "5 g", "Zdravlje i ostalo", aliases = "psyllium fiber vlakna"),
        SupplementDef("L-karnitin", "2 g", "Zdravlje i ostalo", aliases = "carnitine"),
        SupplementDef("Koenzim Q10", "100 mg", "Zdravlje i ostalo", aliases = "coq10 q10"),
        SupplementDef("Glukozamin", "1500 mg", "Zdravlje i ostalo", aliases = "glucosamine zglobovi"),
        SupplementDef("MSM", "1 g", "Zdravlje i ostalo", aliases = "zglobovi"),
        SupplementDef("Spirulina", "3 g", "Zdravlje i ostalo"),
    )

    fun entryText(name: String, dose: String) = if (dose.isBlank()) name.trim() else "${name.trim()}$SEP${dose.trim()}"
    fun nameOf(text: String) = text.substringBefore(SEP).trim()
    fun doseOf(text: String) = if (SEP in text) text.substringAfter(SEP).trim() else ""

    private fun norm(s: String) = ExerciseSearch.normalize(s)

    /**
     * Pretraga: prvo suplementi koje si vec koristio (sa tvojom poslednjom dozom), pa spisak.
     * Svaka rec upita mora da se nadje u nazivu, kategoriji ili aliasima.
     */
    fun search(query: String, history: List<FoodEntry>): List<SupplementDef> {
        val used = history.filter { it.meal == MEAL }.sortedByDescending { it.createdAt }
            .distinctBy { norm(nameOf(it.text)) }
            .map { e ->
                val known = (LIST + LIST_SR).firstOrNull { norm(it.name) == norm(nameOf(e.text)) }
                SupplementDef(nameOf(e.text), doseOf(e.text).ifBlank { known?.dose ?: "" }, "Skoro korišćeno",
                    e.kcal ?: known?.kcal, e.proteinG ?: known?.proteinG, known?.aliases ?: "")
            }
        val usedNames = used.map { norm(it.name) }.toSet()
        val all = used + LIST.filter { norm(it.name) !in usedNames }
        val tokens = norm(query).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return all
        return all.filter { d -> val hay = norm("${d.name} ${d.category} ${d.aliases}"); tokens.all { it in hay } }
    }
}

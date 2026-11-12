package com.vukos.trening.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/*
 * Ceo model podataka je definisan vec u fazi 1, da kasnije faze ne bi trazile migracije.
 * Polja notionPageId / updatedAt / dirty / deleted sluze sinhronizaciji sa Notionom (faza 6):
 *  - dirty = true  -> red jos nije poslat u Notion
 *  - deleted = true -> obrisan u aplikaciji; stranica u Notionu ce biti arhivirana, pa se red brise
 */

fun newId(): String = UUID.randomUUID().toString()

@Entity(tableName = "exercise", indices = [Index("name")])
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscle: String,
    /** Zarezom razdvojena lista. */
    val secondaryMuscles: String = "",
    val equipment: String = "",
    val category: String = "strength",
    val level: String = "",
    /** Koraci uputstva razdvojeni sa \n. */
    val instructions: String = "",
    /** Putanje slika u free-exercise-db repozitorijumu, razdvojene zarezom. */
    val imagePaths: String = "",
    val isCustom: Boolean = false,
    /** Lokalna slika za sopstvene vezbe (file:// u internom skladistu aplikacije). */
    val customImage: String? = null,
    /** Licna beleska uz vezbu (npr. "sediste na 4"), prikazuje se na svakom treningu. */
    @ColumnInfo(defaultValue = "") val notes: String = "",
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = false,
    val deleted: Boolean = false,
) {
    val imageUrls: List<String>
        get() = customImage?.let { listOf(it) }
            ?: imagePaths.split(',').filter { it.isNotBlank() }.map { IMAGE_BASE + it }

    val thumbnailUrl: String? get() = imageUrls.firstOrNull()

    val secondaryList: List<String>
        get() = secondaryMuscles.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        const val IMAGE_BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    }
}

/** Rutina = plan treninga (npr. "Push"). weekdays je bitmaska: bit 0 = ponedeljak ... bit 6 = nedelja. */
@Entity(tableName = "routine")
data class Routine(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val weekdays: Int = 0,
    val notes: String = "",
    val sortOrder: Int = 0,
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

@Entity(tableName = "routine_exercise", indices = [Index("routineId")])
data class RoutineExercise(
    @PrimaryKey val id: String = newId(),
    val routineId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int = 3,
    val repsMin: Int? = 8,
    val repsMax: Int? = 12,
    val targetKg: Double? = null,
    val notes: String = "",
)

@Entity(tableName = "workout", indices = [Index("startedAt")])
data class Workout(
    @PrimaryKey val id: String = newId(),
    val routineId: String? = null,
    val name: String,
    val startedAt: Long = System.currentTimeMillis(),
    /** null = trening je u toku. */
    val finishedAt: Long? = null,
    val notes: String = "",
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "workout_set",
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutSet(
    @PrimaryKey val id: String = newId(),
    val workoutId: String,
    val exerciseId: String,
    /** Redosled vezbe u treningu. */
    val exercisePosition: Int,
    /** Redni broj seta u okviru vezbe (1, 2, 3...). */
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val done: Boolean = false,
    /** normal / warmup / drop / failure (vidi [SetType]). */
    @ColumnInfo(defaultValue = "normal") val setType: String = SetType.NORMAL,
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

/* ---------- Telo i ishrana ---------- */

/** Jedno merenje: kilaza za dan (epochDay = LocalDate.toEpochDay()). */
@Entity(tableName = "body_entry", indices = [Index("epochDay")])
data class BodyEntry(
    @PrimaryKey val id: String = newId(),
    val epochDay: Long,
    val weightKg: Double? = null,
    val notes: String = "",
    /** Obimi u cm i procenat masti (opciono). */
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val bodyFatPct: Double? = null,
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

/** Slika tela, cuva se samo u internom skladistu aplikacije (ne u galeriji). pose: front / side / back. */
@Entity(tableName = "body_photo", indices = [Index("bodyEntryId")])
data class BodyPhoto(
    @PrimaryKey val id: String = newId(),
    val bodyEntryId: String,
    val pose: String,
    val localPath: String,
    /** ID fajla posle upload-a u Notion (faza sinhronizacije). */
    val notionFileId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

@Entity(tableName = "food_entry", indices = [Index("epochDay")])
data class FoodEntry(
    @PrimaryKey val id: String = newId(),
    val epochDay: Long,
    /** Dorucak / Rucak / Vecera / Uzina */
    val meal: String,
    val text: String,
    val kcal: Int? = null,
    val proteinG: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notionPageId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val deleted: Boolean = false,
)

/** Tipovi setova. Zagrevanje se ne racuna u volumen, rekorde ni progresiju. */
object SetType {
    const val NORMAL = "normal"
    const val WARMUP = "warmup"
    const val DROP = "drop"
    const val FAILURE = "failure"
    val ALL = listOf(NORMAL, WARMUP, DROP, FAILURE)
    fun label(t: String) = when (t) { WARMUP -> "Zagrevanje"; DROP -> "Drop set"; FAILURE -> "Do otkaza"; else -> "Normalan set" }
    fun short(t: String) = when (t) { WARMUP -> "W"; DROP -> "D"; FAILURE -> "F"; else -> null }
}

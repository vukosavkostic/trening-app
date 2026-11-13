package com.vukos.trening.data

import com.vukos.trening.i18n.tr

import android.content.Context
import android.net.Uri
import com.vukos.trening.TreningApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate

object BodyRepo {
    private val dao get() = TreningApp.instance.db.body()

    /** Tekuce stanje merenja (null = nista nije zakazano), osvezava se pri svakoj promeni u bazi. */
    fun observeDue(today: () -> LocalDate = { LocalDate.now() }): Flow<CheckInSchedule.Due?> =
        combine(dao.observeAll(), dao.observeAllPhotos()) { entries, photos ->
            val t = today()
            val day = CheckInSchedule.currentCheckInDay(t) ?: return@combine null
            val (from, to) = CheckInSchedule.window(day).let { it.first.toEpochDay() to it.second.toEpochDay() }
            val inWindow = entries.filter { it.epochDay in from..to }
            val ids = inWindow.map { it.id }.toSet()
            CheckInSchedule.due(
                t,
                hasWeight = inWindow.any { it.weightKg != null },
                photoCount = photos.count { it.bodyEntryId in ids },
            )
        }

    /**
     * Cuva merenje za dan: kilaza + do 3 nove slike (poza -> uri). Postojeca slika iste poze se menja.
     */
    suspend fun save(
        context: Context,
        day: LocalDate,
        weightKg: Double?,
        newPhotos: Map<Pose, Uri>,
        measurements: Measurements = Measurements(),
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dao.getByDay(day.toEpochDay())
        val entry = (existing ?: BodyEntry(epochDay = day.toEpochDay()))
            .copy(
                weightKg = weightKg, waistCm = measurements.waist, chestCm = measurements.chest,
                armCm = measurements.arm, thighCm = measurements.thigh, bodyFatPct = measurements.bodyFat,
                updatedAt = now, dirty = true,
            )
        dao.upsert(entry)
        val old = dao.photosFor(entry.id).associateBy { it.pose }
        for ((pose, uri) in newPhotos) {
            val id = newId()
            val path = SecurePhotoStore.importImage(context, uri, id)
            old[pose.key]?.let {
                SecurePhotoStore.delete(it.localPath)
                dao.upsertPhoto(it.copy(deleted = true, dirty = true, updatedAt = now))
            }
            dao.upsertPhoto(BodyPhoto(id = id, bodyEntryId = entry.id, pose = pose.key, localPath = path))
        }
    }

    /** Brise merenje dana: kilazu i slike (sifrovani fajlovi se brisu odmah, Notion stranica se arhivira pri sinhronizaciji). */
    suspend fun delete(day: LocalDate) = withContext(Dispatchers.IO) {
        val entry = dao.getByDay(day.toEpochDay()) ?: return@withContext
        val now = System.currentTimeMillis()
        dao.photosFor(entry.id).forEach {
            SecurePhotoStore.delete(it.localPath)
            dao.upsertPhoto(it.copy(deleted = true, dirty = true, updatedAt = now))
        }
        dao.upsert(entry.copy(deleted = true, dirty = true, updatedAt = now))
    }
}

/** Obimi (cm) i procenat masti - sve opciono. */
data class Measurements(
    val waist: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val thigh: Double? = null,
    val bodyFat: Double? = null,
) {
    val isEmpty get() = listOf(waist, chest, arm, thigh, bodyFat).all { it == null }

    companion object {
        fun of(e: BodyEntry) = Measurements(e.waistCm, e.chestCm, e.armCm, e.thighCm, e.bodyFatPct)

        /** Polja za prikaz: naziv, jedinica, vrednost iz merenja. */
        val FIELDS: List<Triple<String, String, (Measurements) -> Double?>> get() = listOf(
            Triple(tr("Struk", "Waist"), "cm", { it.waist }), Triple(tr("Grudi", "Chest"), "cm", { it.chest }),
            Triple(tr("Ruka", "Arm"), "cm", { it.arm }), Triple(tr("Butina", "Thigh"), "cm", { it.thigh }),
            Triple(tr("Masti", "Body fat"), "%", { it.bodyFat }),
        )
    }
}

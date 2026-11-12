package com.vukos.trening.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Upiti za sinhronizaciju sa Notionom. "Oznaci sinhronizovano" vazi samo ako se red nije menjao
 * u medjuvremenu (updatedAt isti), da se izmena tokom slanja ne izgubi.
 */
@Dao
interface SyncDao {
    /** Broj reda koji cekaju slanje (setovi samo iz zavrsenih/odbacenih treninga). */
    @Query(
        """SELECT (SELECT COUNT(*) FROM exercise WHERE dirty = 1)
         + (SELECT COUNT(*) FROM routine WHERE dirty = 1)
         + (SELECT COUNT(*) FROM workout WHERE dirty = 1 AND (finishedAt IS NOT NULL OR deleted = 1))
         + (SELECT COUNT(*) FROM workout_set s JOIN workout w ON w.id = s.workoutId
              WHERE s.dirty = 1 AND (w.finishedAt IS NOT NULL OR w.deleted = 1))
         + (SELECT COUNT(*) FROM body_entry WHERE dirty = 1)
         + (SELECT COUNT(*) FROM food_entry WHERE dirty = 1)"""
    )
    fun observePending(): Flow<Int>

    @Query("SELECT * FROM exercise WHERE dirty = 1") suspend fun dirtyExercises(): List<Exercise>
    @Query("SELECT * FROM routine WHERE dirty = 1") suspend fun dirtyRoutines(): List<Routine>
    @Query("SELECT * FROM workout WHERE dirty = 1 AND (finishedAt IS NOT NULL OR deleted = 1)") suspend fun dirtyWorkouts(): List<Workout>
    @Query(
        """SELECT s.* FROM workout_set s JOIN workout w ON w.id = s.workoutId
           WHERE s.dirty = 1 AND (w.finishedAt IS NOT NULL OR w.deleted = 1)"""
    )
    suspend fun dirtySets(): List<WorkoutSet>
    @Query("SELECT * FROM body_entry WHERE dirty = 1") suspend fun dirtyBody(): List<BodyEntry>
    @Query("SELECT * FROM food_entry WHERE dirty = 1") suspend fun dirtyFood(): List<FoodEntry>

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId AND notionPageId IS NOT NULL")
    suspend fun syncedSetsOf(workoutId: String): List<WorkoutSet>
    @Query("SELECT COUNT(*) FROM body_photo WHERE bodyEntryId = :entryId AND deleted = 0")
    suspend fun photoCount(entryId: String): Int

    @Query("UPDATE exercise SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun exerciseSynced(id: String, pageId: String?, updatedAt: Long)
    @Query("UPDATE exercise SET notionPageId = :pageId WHERE id = :id")
    suspend fun exercisePage(id: String, pageId: String)
    @Query("UPDATE routine SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun routineSynced(id: String, pageId: String?, updatedAt: Long)
    @Query("UPDATE workout SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun workoutSynced(id: String, pageId: String?, updatedAt: Long)
    @Query("UPDATE workout_set SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun setSynced(id: String, pageId: String?, updatedAt: Long)
    @Query("UPDATE body_entry SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun bodySynced(id: String, pageId: String?, updatedAt: Long)
    @Query("UPDATE food_entry SET notionPageId = :pageId, dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun foodSynced(id: String, pageId: String?, updatedAt: Long)

    // Vracanje iz Notiona: dodaju se samo redovi kojih nema lokalno.
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertExercises(items: List<Exercise>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRoutines(items: List<Routine>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRoutineExercises(items: List<RoutineExercise>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertWorkouts(items: List<Workout>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSets(items: List<WorkoutSet>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBody(items: List<BodyEntry>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertFood(items: List<FoodEntry>)

    @Query("SELECT id FROM routine") suspend fun routineIds(): List<String>
    @Query("SELECT id FROM workout") suspend fun workoutIds(): List<String>
}

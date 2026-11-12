package com.vukos.trening.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observe(id: String): Flow<Exercise?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun get(id: String): Exercise?

    @Query("SELECT COUNT(*) FROM exercise WHERE isCustom = 0")
    suspend fun builtInCount(): Int

    /** Ugradjene vezbe: zamena postojecih (osim notionPageId, koji cuva [upsertBuiltIn]). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<Exercise>)

    @Upsert
    suspend fun upsert(item: Exercise)

    @Query("UPDATE exercise SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id AND isCustom = 1")
    suspend fun softDeleteCustom(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine WHERE deleted = 0 ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Routine>>

    @Upsert
    suspend fun upsert(item: Routine)

    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY position")
    fun observeExercises(routineId: String): Flow<List<RoutineExercise>>

    @Upsert
    suspend fun upsertExercises(items: List<RoutineExercise>)

    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId")
    suspend fun clearExercises(routineId: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout WHERE deleted = 0 ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Workout>>

    @Upsert
    suspend fun upsert(item: Workout)

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId AND deleted = 0 ORDER BY exercisePosition, setIndex")
    fun observeSets(workoutId: String): Flow<List<WorkoutSet>>

    @Upsert
    suspend fun upsertSets(items: List<WorkoutSet>)
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_entry WHERE deleted = 0 ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<BodyEntry>>

    @Query("SELECT * FROM body_entry WHERE deleted = 0 AND epochDay BETWEEN :from AND :to")
    suspend fun entriesBetween(from: Long, to: Long): List<BodyEntry>

    @Query(
        "SELECT COUNT(*) FROM body_photo p JOIN body_entry e ON p.bodyEntryId = e.id " +
            "WHERE p.deleted = 0 AND e.deleted = 0 AND e.epochDay BETWEEN :from AND :to"
    )
    suspend fun photoCountBetween(from: Long, to: Long): Int

    @Upsert
    suspend fun upsert(item: BodyEntry)

    @Query("SELECT * FROM body_photo WHERE bodyEntryId = :entryId AND deleted = 0")
    fun observePhotos(entryId: String): Flow<List<BodyPhoto>>

    @Upsert
    suspend fun upsertPhoto(item: BodyPhoto)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entry WHERE deleted = 0 AND epochDay = :day ORDER BY createdAt")
    fun observeDay(day: Long): Flow<List<FoodEntry>>

    @Upsert
    suspend fun upsert(item: FoodEntry)
}

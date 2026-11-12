package com.vukos.trening.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exercise::class, Routine::class, RoutineExercise::class, Workout::class, WorkoutSet::class,
        BodyEntry::class, BodyPhoto::class, FoodEntry::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun routines(): RoutineDao
    abstract fun workouts(): WorkoutDao
    abstract fun body(): BodyDao
    abstract fun food(): FoodDao
    abstract fun sync(): SyncDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "trening.db").build()
    }
}

package com.vukos.trening.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Exercise::class, Routine::class, RoutineExercise::class, Workout::class, WorkoutSet::class,
        BodyEntry::class, BodyPhoto::class, FoodEntry::class,
    ],
    version = 2,
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
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).addMigrations(MIGRATION_1_2).build()

        const val NAME = "trening.db"

        /** v2: tip seta, beleska uz vezbu, obimi tela i % masti. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_set ADD COLUMN setType TEXT NOT NULL DEFAULT 'normal'")
                db.execSQL("ALTER TABLE exercise ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                for (c in listOf("waistCm", "chestCm", "armCm", "thighCm", "bodyFatPct"))
                    db.execSQL("ALTER TABLE body_entry ADD COLUMN $c REAL")
            }
        }
    }
}

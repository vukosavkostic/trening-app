package com.vukos.trening.data

/**
 * Rotacija planova ("Sledeći" na tabu Trening): redosled planova u listi "Moji planovi" je rotacija.
 * Sledeci je plan posle onog koji je poslednji odradjen; slobodan trening (bez plana) ne pomera rotaciju,
 * a ako se preskoci na drugi plan, rotacija se nastavlja od plana koji je stvarno odradjen.
 */
object Rotation {
    data class Result(val next: Routine?, val last: Routine?)

    fun next(routines: List<Routine>, finished: List<Workout>): Result {
        if (routines.isEmpty()) return Result(null, null)
        val ids = routines.map { it.id }
        val lastW = finished.filter { !it.deleted && it.finishedAt != null && it.routineId in ids }.maxByOrNull { it.startedAt }
            ?: return Result(routines.first(), null)
        val i = ids.indexOf(lastW.routineId)
        return Result(routines[(i + 1) % routines.size], routines[i])
    }
}

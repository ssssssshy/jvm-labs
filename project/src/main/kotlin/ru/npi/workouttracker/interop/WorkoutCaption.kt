@file:JvmName("WorkoutCaptions")

package ru.npi.workouttracker.interop

import ru.npi.workouttracker.domain.Workout
import ru.npi.workouttracker.domain.WorkoutStatus

fun caption(workout: Workout): String =
    "${workout.type()} — ${statusTitle(workout.status())}"

private fun statusTitle(status: WorkoutStatus): String = when (status) {
    WorkoutStatus.PLANNED -> "запланирована"
    WorkoutStatus.IN_PROGRESS -> "выполняется"
    WorkoutStatus.COMPLETED -> "выполнена"
    WorkoutStatus.ATTENTION -> "требует внимания"
}

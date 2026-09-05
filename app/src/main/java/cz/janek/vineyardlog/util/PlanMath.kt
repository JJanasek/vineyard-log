package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.SeasonTask
import cz.janek.vineyardlog.data.model.TaskDone

/** Tasks whose month window covers [month] and that are not ticked off in [done]. */
fun openTasksThisMonth(tasks: List<SeasonTask>, done: List<TaskDone>, month: Int): Int {
    val doneIds = done.map { it.taskId }.toSet()
    return tasks.count { t ->
        val inWindow = if (t.monthFrom <= t.monthTo) month in t.monthFrom..t.monthTo else (month >= t.monthFrom || month <= t.monthTo)
        inWindow && t.id !in doneIds
    }
}

package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.guide.Bi

/**
 * Must sugar categories of the Czech wine law (zákon č. 321/2004 Sb., § 17–19). A degree of the
 * normalised must meter (°NM) is defined there as kilograms of fermentable sugar per hectolitre of must.
 * These thresholds are the only hard numbers on the °NM scale; everything else (what to aim for in a
 * given vineyard) is the grower's decision.
 */
object SugarGrades {
    data class Grade(val nm: Double, val label: Bi)

    val all = listOf(
        Grade(14.0, Bi("regional wine", "zemské víno")),
        Grade(15.0, Bi("quality wine", "jakostní víno")),
        Grade(19.0, Bi("kabinett", "kabinetní víno")),
        Grade(21.0, Bi("late harvest", "pozdní sběr")),
        Grade(24.0, Bi("selection of grapes", "výběr z hroznů")),
        Grade(27.0, Bi("selection of berries / ice / straw wine", "výběr z bobulí / ledové / slámové")),
        Grade(32.0, Bi("selection of raisined berries", "výběr z cibéb")),
    )

    /** Highest category the must reaches, or null below 14 °NM. */
    fun gradeFor(nm: Double): Grade? = all.lastOrNull { nm + 1e-9 >= it.nm }

    fun labelFor(nm: Double, czech: Boolean): String? = gradeFor(nm)?.label?.get(czech)
}

package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Thresholds of zákon č. 321/2004 Sb., § 17–19. */
class SugarGradesTest {
    private fun cs(nm: Double) = SugarGrades.labelFor(nm, czech = true)

    @Test fun thresholdsMatchTheWineLaw() {
        assertNull(cs(13.9))
        assertEquals("zemské víno", cs(14.0))
        assertEquals("jakostní víno", cs(15.0))
        assertEquals("jakostní víno", cs(18.9))
        assertEquals("kabinetní víno", cs(19.0))
        assertEquals("pozdní sběr", cs(21.0))
        assertEquals("pozdní sběr", cs(23.5))
        assertEquals("výběr z hroznů", cs(24.0))
        assertEquals("výběr z bobulí / ledové / slámové", cs(27.0))
        assertEquals("výběr z cibéb", cs(32.0))
        assertEquals("výběr z cibéb", cs(40.0))
    }

    @Test fun englishLabelsAndGradeObject() {
        assertEquals("late harvest", SugarGrades.labelFor(21.0, czech = false))
        assertEquals(19.0, SugarGrades.gradeFor(20.0)!!.nm, 1e-9)
    }
}

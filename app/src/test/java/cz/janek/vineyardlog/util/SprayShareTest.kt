package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SprayShareTest {
    private fun b(id: Long, area: Double? = null, vines: Int? = null) = Block(id = id, name = "b$id", areaHa = area, vineCount = vines)

    @Test fun splitsByAreaWhenEveryBlockHasOne() {
        val s = SprayShare.shares(listOf(b(1, area = 0.35), b(2, area = 0.15)))
        assertEquals(0.7, s.getValue(1), 1e-9)
        assertEquals(0.3, s.getValue(2), 1e-9)
        val v = SprayShare.splitVolume(100.0, listOf(b(1, area = 0.35), b(2, area = 0.15)))
        assertEquals(70.0, v.getValue(1)!!, 1e-9)
        assertEquals(30.0, v.getValue(2)!!, 1e-9)
    }

    @Test fun fallsBackToVinesThenToAnEvenSplit() {
        val vines = SprayShare.shares(listOf(b(1, vines = 100), b(2, vines = 50)))
        assertEquals(2.0 / 3, vines.getValue(1), 1e-9)
        // one block without area or vines: everyone gets the same
        val even = SprayShare.shares(listOf(b(1, area = 0.3), b(2)))
        assertEquals(0.5, even.getValue(1), 1e-9)
        assertEquals(0.5, even.getValue(2), 1e-9)
    }

    @Test fun sharesAlwaysSumToOne() {
        listOf(
            listOf(b(1, area = 0.35), b(2, area = 0.15), b(3, area = 0.5)),
            listOf(b(1, vines = 60), b(2, vines = 90)),
            listOf(b(1), b(2), b(3)),
        ).forEach { assertEquals(1.0, SprayShare.shares(it).values.sum(), 1e-9) }
    }

    @Test fun noBlocksAndNoVolume() {
        assertTrue(SprayShare.shares(emptyList()).isEmpty())
        assertNull(SprayShare.splitVolume(null, listOf(b(1, area = 0.2))).getValue(1))
    }
}

package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NitrogenBalanceTest {
    @Test fun coverageAndPercent() {
        val s = NitrogenBalance.status(18.0, 45.0)
        assertEquals(0.4, s.coverage, 1e-9)
        assertEquals(40.0, s.percent, 1e-9)
        assertEquals(NitrogenBalance.Level.LOW, s.level)
    }

    @Test fun theBandAroundTheTarget() {
        // 60 % of the target is the floor of "on target", 110 % the ceiling
        assertEquals(NitrogenBalance.Level.LOW, NitrogenBalance.status(23.9, 40.0).level)
        assertEquals(NitrogenBalance.Level.OK, NitrogenBalance.status(24.0, 40.0).level)
        assertEquals(NitrogenBalance.Level.OK, NitrogenBalance.status(44.0, 40.0).level)
        assertEquals(NitrogenBalance.Level.HIGH, NitrogenBalance.status(44.1, 40.0).level)
    }

    @Test fun nothingAppliedIsShortAndNoTargetIsNeverJudged() {
        assertEquals(NitrogenBalance.Level.LOW, NitrogenBalance.status(0.0, 40.0).level)
        assertEquals(NitrogenBalance.Level.OK, NitrogenBalance.status(80.0, 0.0).level)
        assertEquals(0.0, NitrogenBalance.status(80.0, 0.0).coverage, 1e-9)
    }

    @Test fun foliarStaysOutOfTheBaseSupply() {
        val season = Nutrients.Season(
            soil = Nutrients.Npk(18.0, 0.0, 75.0),
            foliar = Nutrients.Npk(0.4, 0.4, 0.3),
            coverCropN = 8.0,
        )
        assertEquals(26.0, season.baseN, 1e-9)
        assertEquals(NitrogenBalance.Level.OK, NitrogenBalance.status(season.baseN, 40.0).level)
    }
}

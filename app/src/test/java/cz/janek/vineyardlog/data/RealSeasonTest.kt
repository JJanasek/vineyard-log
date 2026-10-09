package cz.janek.vineyardlog.data

import cz.janek.vineyardlog.data.backup.BackupCodec
import cz.janek.vineyardlog.data.backup.BackupData
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.UsageWithProduct
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.util.DiseaseRisk
import cz.janek.vineyardlog.util.Gdd
import cz.janek.vineyardlog.util.Phi
import cz.janek.vineyardlog.util.RiskLevel
import cz.janek.vineyardlog.util.SoilAdvice
import cz.janek.vineyardlog.util.SprayShare
import cz.janek.vineyardlog.util.Steberla
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.util.yearOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The calculations run over two real seasons of the Vlčnov vineyard: 2025 and 2026, three blocks,
 * 619 measured weather days from ČHMÚ. Unit tests elsewhere check each formula against numbers
 * chosen to exercise it; this one checks that the whole chain still produces the same answers on
 * data that was not chosen at all, which is what catches a refactor quietly changing a result.
 *
 * The fixture is a reduction of a real backup with the free text removed, made by
 * `tools/make_test_fixture.py`. The grower's own backup stays out of the repository.
 */
class RealSeasonTest {
    private val backup: BackupData = BackupCodec.decode(
        requireNotNull(javaClass.getResourceAsStream("/season-real.json")) {
            "season-real.json missing; regenerate it with tools/make_test_fixture.py"
        }.bufferedReader().readText()
    )
    private val settings = Settings()

    private fun details(): List<EntryWithDetails> {
        val byProduct = backup.products.associateBy { it.id }
        return backup.entries.map { e ->
            EntryWithDetails(
                entry = e,
                usages = backup.usages.filter { it.entryId == e.id }.map { UsageWithProduct(it, byProduct[it.productId]) },
                measurements = backup.measurements.filter { it.entryId == e.id },
            )
        }
    }

    @Test fun theBackupSurvivesItsOwnCodec() {
        val again = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup.totalRows, again.totalRows)
        assertEquals(backup.entries.map { it.id }, again.entries.map { it.id })
        assertEquals(backup.weather.size, again.weather.size)
        // the oldest entries predate several schema versions and still read back
        assertEquals(2025, yearOf(backup.entries.minOf { it.date }))
        assertEquals(56, backup.entries.size)
    }

    @Test fun seasonSummariesMatchTheMeasuredWeather() {
        val a = Gdd.summary(backup.weather, 2025, settings)
        assertEquals(1280.5, a.gdd, 0.5)
        assertEquals(471.0, a.rainMm, 0.5)
        assertEquals(214, a.daysWithTemp)
        assertEquals(93, a.frostDays)
        assertEquals(35.7, a.tMaxAbs!!, 1e-9)
        assertEquals(-9.5, a.tMinAbs!!, 1e-9)

        val b = Gdd.summary(backup.weather, 2026, settings)
        assertEquals(1298.4, b.gdd, 0.5)
        assertEquals(220.3, b.rainMm, 0.5)
        assertEquals(38.5, b.tMaxAbs!!, 1e-9)
        assertEquals(-14.5, b.tMinAbs!!, 1e-9)

        // the two seasons ripened almost the same, but 2026 was half as wet and more extreme
        assertTrue(kotlin.math.abs(a.gdd - b.gdd) < 25.0)
        assertTrue(b.rainMm < a.rainMm / 2)
        assertTrue(b.tMaxAbs!! > a.tMaxAbs!! && b.tMinAbs!! < a.tMinAbs!!)
    }

    @Test fun steberlaSeparatesTheWetYearFromTheDryOne() {
        val end2025 = Steberla.evaluate(backup.weather, 2025, LocalDate.of(2025, 7, 30).toEpochDay())!!
        val end2026 = Steberla.evaluate(backup.weather, 2026, LocalDate.of(2026, 7, 30).toEpochDay())!!
        // the curves only depend on the date, so both years are judged against the same thresholds
        assertEquals(end2025.a, end2026.a, 1e-9)
        assertEquals(217.15, end2025.a, 0.01)
        assertEquals(350.94, end2025.b, 0.01)

        assertEquals(242.0, end2025.cumulativeMm, 0.5)
        assertEquals(Steberla.Zone.SPORADIC, end2025.zone)   // above curve A: more than basic sprays
        assertEquals(134.4, end2026.cumulativeMm, 0.5)
        assertEquals(Steberla.Zone.NON_CALAMITOUS, end2026.zone)
        assertEquals(91, end2025.daysWithRain)
    }

    @Test fun theWeatherEndsOnPowderyMildewPressure() {
        val risk = requireNotNull(DiseaseRisk.summarize(backup.weather, emptyList(), LocalDate.of(2026, 5, 1).toEpochDay()))
        assertEquals(LocalDate.of(2026, 9, 11).toEpochDay(), risk.date)
        assertEquals(RiskLevel.HIGH, risk.oidium)
        assertEquals(80, risk.oidiumIndex)
        assertEquals(RiskLevel.MODERATE, risk.botrytis)
        assertEquals(1, risk.wetDaysLast3)
        assertEquals(RiskLevel.LOW, risk.peronospora)   // the dry summer kept downy mildew down
    }

    @Test fun sugarReadingsForecastTheHarvest() {
        fun trendFor(blockId: Long) = WineMath.sugarTrend(
            backup.measurements.filter { it.blockId == blockId && yearOf(it.date) == 2026 && it.kind == MeasurementKind.NM }
        )
        // Modrý Portugal was still short of 21 °NM and gaining about 0.1 a day
        val mp = requireNotNull(trendFor(1))
        assertEquals(19.0, mp.lastValue, 1e-9)
        assertEquals(0.105, mp.perDay, 0.005)
        assertEquals(20, WineMath.daysTo(mp, 21.0))
        // the two Pinots were already past the target, so there is nothing to wait for
        assertEquals(23.0, trendFor(2)!!.lastValue, 1e-9)
        assertEquals(22.0, trendFor(3)!!.lastValue, 1e-9)
        assertNull(WineMath.daysTo(trendFor(2)!!, 21.0))
        assertNull(WineMath.daysTo(trendFor(3)!!, 21.0))
    }

    @Test fun noHarvestBrokeAPreHarvestInterval() {
        val d = details()
        val earliest = requireNotNull(Phi.earliestHarvest(d))
        assertEquals(LocalDate.of(2026, 7, 31), LocalDate.ofEpochDay(earliest))
        val harvests = d.filter { it.entry.type == EntryType.HARVEST && yearOf(it.entry.date) == 2026 }
        assertEquals(3, harvests.size)
        assertTrue(harvests.all { it.entry.date >= earliest })
        assertEquals(231.0, harvests.sumOf { it.entry.quantity ?: 0.0 }, 1e-9)
        // 2025 brought a third more fruit than the dry 2026
        val y2025 = d.filter { it.entry.type == EntryType.HARVEST && yearOf(it.entry.date) == 2025 }
        assertEquals(301.5, y2025.sumOf { it.entry.quantity ?: 0.0 }, 1e-9)
    }

    @Test fun theSoilAnalysisRaisesEveryWarningItShould() {
        val soil = backup.measurements.filter { it.blockId == 1L && it.kind.name.startsWith("SOIL") }
        fun v(k: MeasurementKind) = soil.firstOrNull { it.kind == k }?.value
        assertEquals(7.3, v(MeasurementKind.SOIL_PH)!!, 1e-9)
        val notes = SoilAdvice.evaluate(
            SoilAdvice.Soil(
                ph = v(MeasurementKind.SOIL_PH), humusPct = v(MeasurementKind.SOIL_ORGANIC_MATTER),
                kMgPerKg = v(MeasurementKind.SOIL_K), mgMgPerKg = v(MeasurementKind.SOIL_MG),
            )
        )
        assertEquals(3, notes.count { it.level == SoilAdvice.Level.WARN })
        assertEquals(0.78, SoilAdvice.kMgRatio(v(MeasurementKind.SOIL_K), v(MeasurementKind.SOIL_MG))!!, 0.005)
        assertTrue(notes.any { it.text.cs.contains("zásadité") })
        assertTrue(notes.any { it.text.cs.contains("Humus") })
        assertTrue(notes.any { it.text.cs.contains("K/Mg") })
    }

    /**
     * What the season does not record. Every spray went in without a block and the blocks carry no
     * area, so anything per hectare has nothing to divide by. This is the shape of real data a
     * hobby grower produces, and the code has to stay quiet rather than guess.
     */
    @Test fun missingBlockAndAreaLeaveThePerHectareFiguresEmpty() {
        assertTrue(backup.blocks.all { it.areaHa == null })
        assertTrue(backup.blocks.all { (it.vineCount ?: 0) > 0 })
        val sprays = details().filter { it.entry.type == EntryType.SPRAY }
        assertEquals(5, sprays.size)
        assertTrue(sprays.all { it.entry.blockId == null })
        assertTrue(sprays.all { it.entry.waterLPerHa == null })

        // with no area the split falls back to vine counts, which the blocks do have
        val shares = SprayShare.shares(backup.blocks)
        assertEquals(1.0, shares.values.sum(), 1e-9)
        assertEquals(40.0 / 153, shares.getValue(1), 1e-9)
        assertEquals(63.0 / 153, shares.getValue(2), 1e-9)
        assertEquals(50.0 / 153, shares.getValue(3), 1e-9)
        // 30 litres of mix over the three blocks
        val split = SprayShare.splitVolume(30.0, backup.blocks)
        assertEquals(30.0, split.values.filterNotNull().sum(), 1e-9)
        assertEquals(12.35, split.getValue(2)!!, 0.01)
    }
}

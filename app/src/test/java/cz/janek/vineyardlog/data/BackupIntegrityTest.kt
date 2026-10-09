package cz.janek.vineyardlog.data

import cz.janek.vineyardlog.data.backup.BackupCodec
import cz.janek.vineyardlog.data.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Checks a real backup lying in the project root, the newest `vineyard-log-*.json`. Those files are
 * the grower's own data and stay out of the repository, so the test skips when none is there and
 * never runs in CI; `RealSeasonTest` covers the committed fixture instead.
 *
 * What it looks for is what merging two backups can quietly produce: the same day or the same entry
 * twice, and rows pointing at a product, block or batch that did not come across.
 */
class BackupIntegrityTest {
    private val file: File? = sequenceOf(File("."), File(".."))
        .mapNotNull { it.listFiles { _, n -> n.startsWith("vineyard-log-") && n.endsWith(".json") }?.toList() }
        .flatten().maxByOrNull { it.lastModified() }

    @Test fun aRealBackupIsConsistent() {
        assumeTrue("no vineyard-log-*.json in the project root", file != null)
        val b = BackupCodec.decode(file!!.readText())
        val where = file.name

        // one row per day: a merge that appends instead of replacing shows up here first
        val dupDays = b.weather.groupBy { it.date }.filterValues { it.size > 1 }.keys
        assertEquals("$where: weather days stored twice: ${dupDays.take(5).map(LocalDate::ofEpochDay)}", emptySet<Long>(), dupDays)

        val dupEntries = b.entries
            .groupBy { listOf(it.date, it.type, it.blockId, it.batchId, it.title.trim()) }
            .filterValues { it.size > 1 }.keys
        assertEquals("$where: the same entry twice: ${dupEntries.take(5)}", emptySet<List<Any?>>(), dupEntries)

        // nothing may point at a row that did not come across
        val products = b.products.map { it.id }.toSet()
        val entries = b.entries.map { it.id }.toSet()
        val blocks = b.blocks.map { it.id }.toSet()
        val batches = b.batches.map { it.id }.toSet()
        assertEquals("$where: usages of an unknown product", 0, b.usages.count { it.productId !in products })
        assertEquals("$where: usages on an unknown entry", 0, b.usages.count { it.entryId !in entries })
        assertEquals("$where: measurements on an unknown entry", 0, b.measurements.count { it.entryId != null && it.entryId !in entries })
        assertEquals("$where: entries on an unknown block", 0, b.entries.count { it.blockId != null && it.blockId !in blocks })
        assertEquals("$where: entries on an unknown batch", 0, b.entries.count { it.batchId != null && it.batchId !in batches })
        assertEquals("$where: batch sources of an unknown batch", 0, b.batchSources.count { it.batchId !in batches })

        // values that cannot be right
        val today = LocalDate.now().toEpochDay()
        assertEquals("$where: entries dated in the future", 0, b.entries.count { it.date > today })
        assertTrue("$where: a harvest with a non-positive weight",
            b.entries.none { it.type == EntryType.HARVEST && (it.quantity ?: 1.0) <= 0.0 })
        assertTrue("$where: a block with a non-positive vine count",
            b.blocks.none { it.vineCount != null && it.vineCount!! <= 0 })
        assertTrue("$where: a weather day with tMin above tMax",
            b.weather.none { it.tMin != null && it.tMax != null && it.tMin!! > it.tMax!! })
    }

    /** Two catalogue imports can leave the same product under two ids; harmless, but worth seeing. */
    @Test fun productNamesAreReportedWhenTheyRepeat() {
        assumeTrue(file != null)
        val b = BackupCodec.decode(file!!.readText())
        val repeated = b.products.groupBy { it.name.trim().lowercase() }.filterValues { it.size > 1 }
        if (repeated.isNotEmpty()) {
            println("${file.name}: ${repeated.size} product names appear more than once: ${repeated.keys.take(5)}")
        }
        assertTrue("more than a handful of duplicate product names: ${repeated.keys.take(10)}", repeated.size <= 10)
    }
}

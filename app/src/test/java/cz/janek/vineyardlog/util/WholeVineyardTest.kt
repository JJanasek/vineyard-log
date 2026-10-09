package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.UsageWithProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class WholeVineyardTest {
    private val blocks = listOf(
        Block(id = 1, name = "MP", vineCount = 40),
        Block(id = 2, name = "RB", vineCount = 63),
        Block(id = 3, name = "RS", vineCount = 50),
    )

    private fun e(id: Long, type: EntryType, blockId: Long?, qty: Double? = null, total: Double? = null) = EntryWithDetails(
        entry = LogEntry(id = id, date = 100L + id, domain = Domain.VINEYARD, type = type, blockId = blockId, quantity = qty),
        usages = total?.let { listOf(UsageWithProduct(ProductUsage(productId = 1, totalAmount = total), null)) } ?: emptyList(),
        measurements = emptyList(),
    )

    @Test
    fun wholeVineyardSprayCountsForEveryBlockScaledByShare() {
        val all = listOf(e(1, EntryType.SPRAY, null, qty = 153.0, total = 30.6), e(2, EntryType.SPRAY, 3, qty = 10.0))
        val rs = WholeVineyard.forBlock(all, blocks, 3)
        assertEquals(2, rs.count { it.entry.type == EntryType.SPRAY })
        val shared = rs.first { it.entry.id == 1L }
        assertEquals(50.0, shared.entry.quantity!!, 1e-9)
        assertEquals(10.0, shared.usages.single().usage.totalAmount!!, 1e-9)
        assertEquals(1, WholeVineyard.forBlock(all, blocks, 1).size)
    }

    @Test
    fun harvestsAndOtherBlocksStayOut() {
        val all = listOf(e(1, EntryType.HARVEST, null, qty = 300.0), e(2, EntryType.SPRAY, 2), e(3, EntryType.RIPENESS, null))
        assertEquals(0, WholeVineyard.forBlock(all, blocks, 1).size)
    }
}

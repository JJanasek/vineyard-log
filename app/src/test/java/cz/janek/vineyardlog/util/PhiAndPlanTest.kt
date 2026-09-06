package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.SeasonTask
import cz.janek.vineyardlog.data.model.TaskDone
import cz.janek.vineyardlog.data.model.UsageWithProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhiAndPlanTest {
    private fun spray(date: Long, vararg phis: Int?): EntryWithDetails = EntryWithDetails(
        entry = LogEntry(date = date, domain = Domain.VINEYARD, type = EntryType.SPRAY),
        usages = phis.mapIndexed { i, phi -> UsageWithProduct(ProductUsage(productId = i.toLong()), Product(name = "p$i", category = ProductCategory.FUNGICIDE, phiDays = phi)) },
        measurements = emptyList(),
    )

    @Test fun earliestHarvestIsTheLatestPhiEnd() {
        val entries = listOf(spray(100, 28), spray(110, 7, 21), spray(120, null))
        assertEquals(131L, Phi.earliestHarvest(entries))   // 110 + 21 beats 100 + 28
        assertNull(Phi.earliestHarvest(listOf(spray(120, null))))
        assertEquals(135L, Phi.endOfPhi(100, listOf(7, 35, null)))
        assertNull(Phi.endOfPhi(100, listOf(null)))
    }

    @Test fun openTasksHonourWindowsThatWrapTheYear() {
        val tasks = listOf(
            SeasonTask(id = 1, title = "Winter pruning", monthFrom = 12, monthTo = 2),
            SeasonTask(id = 2, title = "Harvest", monthFrom = 9, monthTo = 10),
            SeasonTask(id = 3, title = "Done already", monthFrom = 9, monthTo = 9),
        )
        val done = listOf(TaskDone(taskId = 3, year = 2026, doneDate = 0))
        assertEquals(1, openTasksThisMonth(tasks, done, 9))   // harvest open, the ticked task not counted
        assertEquals(1, openTasksThisMonth(tasks, done, 1))
        assertEquals(0, openTasksThisMonth(tasks, done, 5))
    }
}

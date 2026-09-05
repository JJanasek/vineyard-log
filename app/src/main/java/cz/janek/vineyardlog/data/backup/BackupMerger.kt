package cz.janek.vineyardlog.data.backup

import cz.janek.vineyardlog.data.db.AppDatabase
import cz.janek.vineyardlog.data.model.LogEntry

data class MergeResult(val entries: Int, val products: Int, val blocks: Int, val batches: Int, val weather: Int, val tasks: Int)

/**
 * Adds the content of a backup to the current database without deleting anything.
 * Products, blocks, batches and tasks are matched by name; entries by date + type + title + target.
 * Photo files are not part of the JSON, so photo rows are skipped.
 */
class BackupMerger(private val db: AppDatabase) {
    suspend fun merge(data: BackupData): MergeResult {
        val backup = db.backupDao()
        // products
        val productIdMap = HashMap<Long, Long>()
        val existingProducts = backup.allProducts().associateBy { it.name.trim().lowercase() to it.supplier }.toMutableMap()
        val byNameOnly = backup.allProducts().associateBy { it.name.trim().lowercase() }.toMutableMap()
        var newProducts = 0
        data.products.forEach { p ->
            val key = p.name.trim().lowercase()
            val found = existingProducts[key to p.supplier] ?: byNameOnly[key]
            val id = found?.id ?: db.productDao().upsert(p.copy(id = 0)).also { newProducts++ }
            productIdMap[p.id] = id
        }
        // blocks
        val blockIdMap = HashMap<Long, Long>()
        val existingBlocks = backup.allBlocks().associateBy { it.name.trim().lowercase() }
        var newBlocks = 0
        data.blocks.forEach { b ->
            val id = existingBlocks[b.name.trim().lowercase()]?.id ?: db.blockDao().upsert(b.copy(id = 0)).also { newBlocks++ }
            blockIdMap[b.id] = id
        }
        // batches (+ sources)
        val batchIdMap = HashMap<Long, Long>()
        val existingBatches = backup.allBatches().associateBy { it.name.trim().lowercase() to it.vintage }
        var newBatches = 0
        data.batches.forEach { b ->
            val found = existingBatches[b.name.trim().lowercase() to b.vintage]
            val id = found?.id ?: db.batchDao().insert(b.copy(id = 0)).also { newBatches++ }
            batchIdMap[b.id] = id
            if (found == null) {
                val sources = data.batchSources.filter { it.batchId == b.id }.mapNotNull { s ->
                    blockIdMap[s.blockId]?.let { s.copy(batchId = id, blockId = it) }
                }
                if (sources.isNotEmpty()) db.batchDao().insertSources(sources)
            }
        }
        // entries with usages + measurements
        val existingKeys = backup.allEntries().map { it.key() }.toHashSet()
        var newEntries = 0
        data.entries.forEach { e ->
            val mapped = e.copy(
                id = 0,
                blockId = e.blockId?.let { blockIdMap[it] },
                batchId = e.batchId?.let { batchIdMap[it] },
            )
            if (mapped.key() in existingKeys) return@forEach
            val usages = data.usages.filter { it.entryId == e.id }.mapNotNull { u ->
                productIdMap[u.productId]?.let { u.copy(productId = it) }
            }
            val measurements = data.measurements.filter { it.entryId == e.id }
            db.entryDao().save(mapped, usages, measurements)
            existingKeys.add(mapped.key()); newEntries++
        }
        // standalone measurements (not tied to an entry)
        data.measurements.filter { it.entryId == null }.forEach { m ->
            db.entryDao().insertMeasurements(listOf(m.copy(id = 0, blockId = m.blockId?.let { blockIdMap[it] }, batchId = m.batchId?.let { batchIdMap[it] })))
        }
        // weather: only missing days
        val existingDays = backup.allWeather().map { it.date }.toHashSet()
        val newDays = data.weather.filter { it.date !in existingDays }
        db.weatherDao().upsertAll(newDays)
        // tasks by title
        val existingTasks = backup.allTasks().associateBy { it.title.trim().lowercase() }
        var newTasks = 0
        val taskIdMap = HashMap<Long, Long>()
        data.tasks.forEach { t ->
            val id = existingTasks[t.title.trim().lowercase()]?.id ?: db.taskDao().upsertTask(t.copy(id = 0)).also { newTasks++ }
            taskIdMap[t.id] = id
        }
        data.taskDone.forEach { d -> taskIdMap[d.taskId]?.let { db.taskDao().setDone(d.copy(taskId = it)) } }
        return MergeResult(newEntries, newProducts, newBlocks, newBatches, newDays.size, newTasks)
    }

    private fun LogEntry.key() = listOf(date, type.name, title.trim().lowercase(), blockId ?: -1, batchId ?: -1).joinToString("|")
}

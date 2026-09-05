package cz.janek.vineyardlog.data.backup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.util.Gdd
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.first
import java.io.OutputStream

/** Season report as PDF (A4, plain text layout) and CSV files. Labels come from string resources so they follow the app language. */
class SeasonExport(private val context: Context, private val c: AppContainer) {
    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    private suspend fun load(year: Int): Season {
        val entries = c.entryDao.observeAll().first().filter { yearOf(it.entry.date) == year }
        val blocks = c.blockDao.observeAll().first()
        val batches = c.batchDao.observeAll().first()
        val weather = c.weatherDao.observeAll().first().filter { yearOf(it.date) == year }
        val settings = c.settings.settings.first()
        return Season(year, entries, blocks.associateBy { it.id }, batches.associateBy { it.id }, weather, settings)
    }

    private class Season(
        val year: Int, val entries: List<EntryWithDetails>,
        val blocks: Map<Long, cz.janek.vineyardlog.data.model.Block>, val batches: Map<Long, cz.janek.vineyardlog.data.model.Batch>,
        val weather: List<cz.janek.vineyardlog.data.model.WeatherDay>, val settings: Settings,
    )

    private fun typeLabel(e: EntryWithDetails) = context.getString(e.entry.type.labelRes)

    private fun entryLine(e: EntryWithDetails): String {
        val parts = mutableListOf<String>()
        parts += formatDate(e.entry.date)
        parts += typeLabel(e) + (e.entry.title.takeIf { it.isNotBlank() }?.let { " – $it" } ?: "")
        e.entry.phenologyStage?.let { parts += context.getString(it.labelRes) }
        e.entry.quantity?.let { parts += "${it.fmt()} ${e.entry.quantityUnit}".trim() }
        e.entry.waterLPerHa?.let { parts += "${it.fmt()} l/ha" }
        if (e.usages.isNotEmpty()) parts += e.usages.joinToString("; ") { u ->
            val name = u.product?.name ?: "?"
            val dose = u.usage.dose?.let { " ${it.fmt()} ${u.usage.doseUnit}".trimEnd() } ?: ""
            val phi = u.product?.phiDays?.let { " (PHI $it d)" } ?: ""
            "$name$dose$phi"
        }
        if (e.measurements.isNotEmpty()) parts += e.measurements.joinToString("; ") { m ->
            "${context.getString(m.kind.labelRes)} ${m.value.fmt()} ${m.kind.unit}".trim()
        }
        if (e.entry.notes.isNotBlank()) parts += e.entry.notes.replace("\n", " ")
        return parts.joinToString(" | ")
    }

    // ---------------- PDF ----------------
    suspend fun writePdf(year: Int, out: OutputStream) {
        val season = load(year)
        val doc = PdfDocument()
        val pageW = 595; val pageH = 842; val margin = 40
        val body = TextPaint().apply { textSize = 9.5f; isAntiAlias = true }
        val h1 = TextPaint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val h2 = TextPaint().apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val small = TextPaint().apply { textSize = 8f; color = 0xFF666666.toInt(); isAntiAlias = true }
        var page: PdfDocument.Page? = null; var canvas: Canvas? = null; var y = 0; var pageNo = 0
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page!!.canvas; y = margin
            canvas!!.drawText("${s(R.string.pdf_title, year)} · $pageNo", margin.toFloat(), (pageH - 20).toFloat(), small)
        }
        fun text(t: String, paint: TextPaint, indent: Int = 0, gapAfter: Int = 3) {
            if (canvas == null) newPage()
            val width = pageW - 2 * margin - indent
            val layout = StaticLayout.Builder.obtain(t, 0, t.length, paint, width).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            if (y + layout.height > pageH - margin - 20) newPage()
            canvas!!.save(); canvas!!.translate((margin + indent).toFloat(), y.toFloat()); layout.draw(canvas!!); canvas!!.restore()
            y += layout.height + gapAfter
        }
        newPage()
        text(s(R.string.pdf_title, year), h1, gapAfter = 2)
        text(s(R.string.pdf_exported, formatDate(java.time.LocalDate.now().toEpochDay())), small, gapAfter = 10)

        // vineyard by block
        text(s(R.string.pdf_vineyard), h2, gapAfter = 6)
        val vineyard = season.entries.filter { it.entry.domain == cz.janek.vineyardlog.data.model.Domain.VINEYARD }
        val byBlock = vineyard.groupBy { it.entry.blockId }
        (season.blocks.values.sortedBy { it.name }.map { it.id to it.name } + listOf(null to s(R.string.whole_vineyard))).forEach { (id, name) ->
            val list = byBlock[id] ?: return@forEach
            val b = id?.let { season.blocks[it] }
            val head = listOfNotNull(name, b?.variety?.takeIf { it.isNotBlank() }, b?.areaHa?.let { "${it.fmt(3)} ha" }).joinToString(" · ")
            text(head, h2.apply { textSize = 11f }, gapAfter = 2)
            list.sortedBy { it.entry.date }.forEach { text(entryLine(it), body, indent = 10, gapAfter = 2) }
            y += 6
        }
        if (vineyard.isEmpty()) text(s(R.string.pdf_no_entries), body)

        // cellar by batch
        y += 6; text(s(R.string.pdf_cellar), h2.apply { textSize = 12f }, gapAfter = 6)
        val cellar = season.entries.filter { it.entry.domain == cz.janek.vineyardlog.data.model.Domain.CELLAR }
        cellar.groupBy { it.entry.batchId }.toSortedMap(compareBy { it ?: Long.MAX_VALUE }).forEach { (id, list) ->
            val b = id?.let { season.batches[it] }
            val head = b?.let { listOfNotNull("${it.name} (${it.vintage})", it.variety.takeIf { v -> v.isNotBlank() }, it.volumeL?.let { v -> "${v.fmt()} L" }).joinToString(" · ") } ?: "—"
            text(head, h2.apply { textSize = 11f }, gapAfter = 2)
            list.sortedBy { it.entry.date }.forEach { text(entryLine(it), body, indent = 10, gapAfter = 2) }
            y += 6
        }
        if (cellar.isEmpty()) text(s(R.string.pdf_no_entries), body)

        // weather
        y += 6; text(s(R.string.pdf_weather), h2.apply { textSize = 12f }, gapAfter = 6)
        val sum = Gdd.summary(season.weather, year, season.settings)
        text(s(R.string.pdf_weather_line, sum.gdd.fmt(0), season.settings.gddBase.fmt(), sum.rainMm.fmt(0), sum.frostDays, sum.hailDays, sum.daysWithTemp), body)
        page?.let { doc.finishPage(it) }
        doc.writeTo(out); doc.close()
    }

    // ---------------- CSV ----------------
    private fun csv(v: Any?): String {
        val t = v?.toString() ?: ""
        return if (t.any { it == ',' || it == '"' || it == '\n' || it == ';' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
    }

    suspend fun writeEntriesCsv(year: Int, out: OutputStream) {
        val season = load(year)
        val sb = StringBuilder("\uFEFF")
        sb.append(listOf("date", "domain", "type", "block", "batch", "title", "stage", "quantity", "unit", "water_l_ha", "products", "measurements", "temp_c", "wind_kmh", "humidity_pct", "labour_h", "cost", "notes").joinToString(";")).append('\n')
        season.entries.sortedBy { it.entry.date }.forEach { e ->
            val en = e.entry
            val products = e.usages.joinToString(" | ") { u -> "${u.product?.name ?: "?"} ${u.usage.dose?.fmt() ?: ""} ${u.usage.doseUnit}".trim() }
            val meas = e.measurements.joinToString(" | ") { m -> "${m.kind.name}=${m.value.fmt()}${m.kind.unit.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}" }
            sb.append(listOf(
                formatDate(en.date), en.domain.name, context.getString(en.type.labelRes), en.blockId?.let { season.blocks[it]?.name } ?: "",
                en.batchId?.let { season.batches[it]?.name } ?: "", en.title, en.phenologyStage?.let { context.getString(it.labelRes) } ?: "",
                en.quantity?.fmt() ?: "", en.quantityUnit, en.waterLPerHa?.fmt() ?: "", products, meas,
                en.tempC?.fmt() ?: "", en.windKmh?.fmt() ?: "", en.humidityPct?.fmt() ?: "", en.laborHours?.fmt() ?: "", en.cost?.fmt() ?: "", en.notes,
            ).joinToString(";") { csv(it) }).append('\n')
        }
        out.write(sb.toString().toByteArray())
    }

    suspend fun writeWeatherCsv(year: Int, out: OutputStream) {
        val season = load(year)
        val sb = StringBuilder("\uFEFF")
        sb.append(listOf("date", "t_min", "t_max", "rain_mm", "humidity_pct", "frost", "hail", "wet_hours", "warm_hours_21_30", "hot_hours_35", "gdd", "source", "note").joinToString(";")).append('\n')
        season.weather.sortedBy { it.date }.forEach { d ->
            sb.append(listOf(
                formatDate(d.date), d.tMin?.fmt() ?: "", d.tMax?.fmt() ?: "", d.rainMm?.fmt() ?: "", d.humidityPct?.fmt() ?: "",
                if (d.frost) 1 else 0, if (d.hail) 1 else 0, d.wetHours ?: "", d.warmHours ?: "", d.hotHours ?: "",
                Gdd.daily(d, season.settings.gddBase)?.fmt(1) ?: "", d.source, d.note,
            ).joinToString(";") { csv(it) }).append('\n')
        }
        out.write(sb.toString().toByteArray())
    }
}

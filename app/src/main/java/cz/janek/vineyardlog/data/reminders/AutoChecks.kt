package cz.janek.vineyardlog.data.reminders

import android.content.Context
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.BatchStatus
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.data.web.Chmi
import cz.janek.vineyardlog.data.web.OpenMeteo
import cz.janek.vineyardlog.ui.nav.Routes
import cz.janek.vineyardlog.ui.nav.Tab
import cz.janek.vineyardlog.util.DiseaseRisk
import cz.janek.vineyardlog.util.Steberla
import cz.janek.vineyardlog.util.RiskLevel
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The daily background checks behind the automatic alerts. Each check is independent, catches its
 * own failures and remembers in SharedPreferences when it last spoke so it does not nag every day.
 */
object AutoChecks {
    private const val PREFS = "auto_checks"
    private const val ID_RISK = 9001
    private const val ID_PLAN = 9002
    private const val ID_FERM = 9100
    private const val ID_SAMPLING = 9200
    private const val ID_FROST = 9300

    /** Runs every enabled check; returns one line per alert posted (for the "run now" button). */
    suspend fun run(c: AppContainer): List<String> {
        val s = c.settings.settings.first()
        val prefs = c.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayEpochDay()
        val out = ArrayList<String>()
        if (s.autoRisk) runCatching { checkRisk(c, s, prefs, today) }.getOrNull()?.let(out::add)
        if (s.autoPlan) runCatching { checkPlan(c, prefs) }.getOrNull()?.let(out::add)
        if (s.autoFermentation) runCatching { checkFermentation(c, prefs, today) }.getOrNull()?.let(out::addAll)
        if (s.autoSampling) runCatching { checkSampling(c, s, prefs, today) }.getOrNull()?.let(out::addAll)
        if (s.autoFrost) runCatching { checkFrost(c, s, prefs, today) }.getOrNull()?.let(out::addAll)
        prefs.edit().putLong("last_run", System.currentTimeMillis()).apply()
        return out
    }

    fun lastRun(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_run", 0L)

    /** Refresh the last two weeks of Open-Meteo data (hourly aggregates included) and alert on a high risk in season. */
    private suspend fun checkRisk(c: AppContainer, s: Settings, prefs: android.content.SharedPreferences, today: Long): String? {
        val lat = s.latitude ?: return null; val lon = s.longitude ?: return null
        val ctx = c.appContext
        val from = LocalDate.now().minusDays(14); val to = LocalDate.now()
        val fetched = OpenMeteo.fetchDaily(lat, lon, from, to)
        val existing = c.weatherDao.listRange(from.toEpochDay(), to.toEpochDay()).associateBy { it.date }
        val toWrite = fetched.mapNotNull { day ->
            val old = existing[day.date]
            when {
                old == null -> day
                old.source == OpenMeteo.SOURCE -> day.copy(hail = old.hail, note = old.note, frost = day.frost || old.frost)
                // measured or typed rows keep their values, only the hourly indicators and the hourly temperature profile are refreshed
                else -> old.copy(wetHours = day.wetHours ?: old.wetHours, warmHours = day.warmHours ?: old.warmHours, hotHours = day.hotHours ?: old.hotHours, tHourly = day.tHourly.ifBlank { old.tHourly })
            }
        }
        if (toWrite.isNotEmpty()) c.weatherDao.upsertAll(toWrite)
        val month = LocalDate.now().monthValue
        if (month !in 4..10) return null
        val forecast = runCatching { OpenMeteo.fetchForecast(lat, lon) }.getOrDefault(emptyList())
        val all = c.weatherDao.listRange(today - 40, today)
        val recent = all.filter { it.wetHours != null }
        if (recent.isEmpty()) return null
        val year = LocalDate.now().year
        val entries = c.entryDao.observeAll().first()
        val budBreak = entries.filter { it.entry.type == EntryType.PHENOLOGY && it.entry.phenologyStage == PhenologyStage.BUD_BREAK && yearOf(it.entry.date) == year }
            .minOfOrNull { it.entry.date }
        val shootsOut = budBreak?.plus(15) ?: LocalDate.of(year, 5, 1).toEpochDay()
        val risk = DiseaseRisk.summarize(recent, forecast, shootsOut) ?: return null
        // Šteberla zone from the season's rain (approximate curves); say it at most once a week
        val may1 = LocalDate.of(year, 5, 1).toEpochDay()
        val seasonRain = c.weatherDao.listRange(may1, today)
        val st = Steberla.evaluate(seasonRain, year, today)
        if (st != null && !st.periodOver && st.zone == Steberla.Zone.CALAMITOUS && prefs.getLong("steberla", 0L) < today - 7) {
            prefs.edit().putLong("steberla", today).apply()
            c.reminders.alert(ID_RISK + 1, ctx.getString(R.string.alert_risk_title), ctx.getString(R.string.alert_steberla, st.cumulativeMm.fmt(0), st.b.fmt(0)), Tab.OVERVIEW.route)
        }
        val high = listOfNotNull(
            if (risk.peronospora == RiskLevel.HIGH) "peronospora" to ctx.getString(R.string.d_peronospora) else null,
            if (risk.oidium == RiskLevel.HIGH) "oidium" to ctx.getString(R.string.d_oidium) else null,
            if (risk.botrytis == RiskLevel.HIGH) "botrytis" to ctx.getString(R.string.d_botrytis) else null,
        )
        // speak about a disease at most every 3 days
        val fresh = high.filter { (key, _) -> prefs.getLong("risk_$key", 0L) < today - 3 }
        if (fresh.isEmpty()) return null
        val edit = prefs.edit(); fresh.forEach { (key, _) -> edit.putLong("risk_$key", today) }; edit.apply()
        val names = fresh.joinToString(", ") { it.second }
        val title = ctx.getString(R.string.alert_risk_title)
        val text = ctx.getString(R.string.alert_risk_text, names, formatDateShort(risk.date))
        c.reminders.alert(ID_RISK, title, text, Tab.OVERVIEW.route)
        return "$title: $names"
    }

    /** Spring frost: forecast nights at or below 1 °C in the next two days between 20 March and 31 May, once per night. */
    private suspend fun checkFrost(c: AppContainer, s: Settings, prefs: android.content.SharedPreferences, today: Long): List<String> {
        val lat = s.latitude ?: return emptyList(); val lon = s.longitude ?: return emptyList()
        val now = LocalDate.now()
        val from = LocalDate.of(now.year, 3, 20); val to = LocalDate.of(now.year, 5, 31)
        if (now.isBefore(from) || now.isAfter(to)) return emptyList()
        val ctx = c.appContext
        val out = ArrayList<String>()
        val forecast = OpenMeteo.fetchForecast(lat, lon, days = 3)
        forecast.filter { it.date in (today + 1)..(today + 2) }.forEach { d ->
            val t = d.tMin ?: return@forEach
            if (t > 1.0) return@forEach
            if (prefs.getLong("frost_${d.date}", 0L) > 0L) return@forEach
            prefs.edit().putLong("frost_${d.date}", today).apply()
            val title = ctx.getString(R.string.alert_frost_title, formatDate(d.date))
            c.reminders.alert(ID_FROST + (d.date % 100).toInt(), title, ctx.getString(R.string.alert_frost_text, t.fmt(1)), Routes.guide("frost"))
            out += title
        }
        return out
    }

    /** Once a month: plan tasks whose window starts (or ends) this month and are not ticked off. */
    private suspend fun checkPlan(c: AppContainer, prefs: android.content.SharedPreferences): String? {
        val now = LocalDate.now()
        val key = "%d-%02d".format(now.year, now.monthValue)
        if (prefs.getString("plan_month", "") == key) return null
        val tasks = c.taskDao.observeTasks().first()
        val done = c.taskDao.observeDone(now.year).first().map { it.taskId }.toSet()
        val open = tasks.filter { it.id !in done }
        val starting = open.filter { it.monthFrom == now.monthValue }
        val ending = open.filter { it.monthTo == now.monthValue && it.monthFrom != now.monthValue }
        prefs.edit().putString("plan_month", key).apply()
        if (starting.isEmpty() && ending.isEmpty()) return null
        val ctx = c.appContext
        val monthName = now.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        val title = ctx.getString(R.string.alert_plan_title, monthName)
        val text = listOfNotNull(
            starting.takeIf { it.isNotEmpty() }?.let { ctx.getString(R.string.alert_plan_starting, it.joinToString(", ") { t -> t.title }) },
            ending.takeIf { it.isNotEmpty() }?.let { ctx.getString(R.string.alert_plan_ending, it.joinToString(", ") { t -> t.title }) },
        ).joinToString("\n")
        c.reminders.alert(ID_PLAN, title, text, Routes.PLAN)
        return title
    }

    /** Fermenting batches: sugar not dropping over the last readings, or no reading for 4+ days. */
    private suspend fun checkFermentation(c: AppContainer, prefs: android.content.SharedPreferences, today: Long): List<String> {
        val ctx = c.appContext
        val out = ArrayList<String>()
        val batches = c.batchDao.observeAll().first().filter { it.status == BatchStatus.FERMENTING && !it.archived }
        for (b in batches) {
            if (prefs.getLong("ferm_${b.id}", 0L) >= today - 3) continue
            val ms = c.measurementDao.observeForBatch(b.id).first()
            val trend = WineMath.sugarTrend(ms)
            val lastSugar = ms.filter { it.kind == MeasurementKind.NM || it.kind == MeasurementKind.BRIX || it.kind == MeasurementKind.OECHSLE || it.kind == MeasurementKind.SG }.maxByOrNull { it.date }
            val text = when {
                trend != null && trend.points >= 2 && trend.perDay >= -1e-6 &&
                    (if (trend.kind == MeasurementKind.SG) trend.lastValue > 1.0 else trend.lastValue > 2.0) ->
                    ctx.getString(R.string.alert_stuck_text, "${trend.lastValue.fmt(1)} ${trend.kind.unit}".trim())
                lastSugar != null && lastSugar.date <= today - 4 -> ctx.getString(R.string.alert_no_readings_text, (today - lastSugar.date).toInt())
                lastSugar == null && (b.startDate ?: today) <= today - 2 -> ctx.getString(R.string.alert_no_readings_text, (today - (b.startDate ?: today)).toInt())
                else -> null
            } ?: continue
            prefs.edit().putLong("ferm_${b.id}", today).apply()
            val title = ctx.getString(R.string.alert_stuck_title, b.name)
            c.reminders.alert(ID_FERM + (b.id % 1000).toInt(), title, text, Routes.batch(b.id))
            out += title
        }
        return out
    }

    /** Blocks whose sugar trend reaches the target within 10 days: set a sampling reminder two days before and say so. */
    private suspend fun checkSampling(c: AppContainer, s: Settings, prefs: android.content.SharedPreferences, today: Long): List<String> {
        val ctx = c.appContext
        val out = ArrayList<String>()
        val year = LocalDate.now().year
        val blocks = c.blockDao.observeAll().first().filter { !it.archived }
        val entries = c.entryDao.observeAll().first().filter { it.entry.domain == Domain.VINEYARD && yearOf(it.entry.date) == year && it.entry.blockId != null }
        for (b in blocks) {
            val ms = entries.filter { it.entry.blockId == b.id }.flatMap { it.measurements }
            val latest = ms.filter { it.kind == MeasurementKind.NM || it.kind == MeasurementKind.BRIX || it.kind == MeasurementKind.OECHSLE }.maxByOrNull { it.date } ?: continue
            val trend = WineMath.sugarTrend(ms.filter { it.kind == latest.kind }) ?: continue
            if (trend.perDay <= 0) continue
            val targetNm = b.targetNm ?: s.targetSugarNm
            val target = when (trend.kind) { MeasurementKind.BRIX -> targetNm * WineMath.BX_PER_NM; MeasurementKind.OECHSLE -> targetNm * WineMath.OE_PER_NM; else -> targetNm }
            val days = WineMath.daysTo(trend, target) ?: continue
            val estimate = trend.lastDate + days
            if (estimate - today !in 0..10) continue
            val lastEstimate = prefs.getLong("sampling_${b.id}", -1L)
            val lastSaid = prefs.getLong("sampling_said_${b.id}", 0L)
            if (lastEstimate >= 0 && kotlin.math.abs(lastEstimate - estimate) < 2 && lastSaid >= today - 7) continue
            val samplingDay = maxOf(today + 1, estimate - 2)
            val rTitle = ctx.getString(R.string.sampling_reminder_title, b.name, targetNm.fmt(1), formatDateShort(estimate))
            if (c.reminderDao.countAuto(rTitle, samplingDay) == 0) {
                val r = Reminder(
                    title = rTitle, repeat = Repeat.ONCE, hour = 7, minute = 0, startDate = samplingDay, endDate = samplingDay,
                    entryType = EntryType.RIPENESS, blockId = b.id, auto = true,
                )
                val id = c.reminderDao.upsert(r)
                c.reminders.schedule(r.copy(id = id))
            }
            prefs.edit().putLong("sampling_${b.id}", estimate).putLong("sampling_said_${b.id}", today).apply()
            val title = ctx.getString(R.string.alert_sampling_title, b.name)
            val text = ctx.getString(R.string.alert_sampling_text, targetNm.fmt(1), formatDate(estimate), formatDate(samplingDay))
            c.reminders.alert(ID_SAMPLING + (b.id % 1000).toInt(), title, text, Routes.block(b.id))
            out += title
        }
        return out
    }
}

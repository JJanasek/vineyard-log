package cz.janek.vineyardlog.site

import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.guide.GuideData
import cz.janek.vineyardlog.data.guide.GuideLinks
import cz.janek.vineyardlog.data.guide.Phenology
import cz.janek.vineyardlog.data.guide.Sources
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.WineStyle
import cz.janek.vineyardlog.data.templates.CellarTemplates
import cz.janek.vineyardlog.data.templates.SprayProgram
import cz.janek.vineyardlog.data.templates.SprayTarget
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.util.Steberla
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Not a test of behaviour: it writes the app's built-in content (guide, growth stages, varieties, spray
 * programme, cellar templates, enum labels) as JSON into app/build/site-content/, from which
 * web/build_site.py generates the website. Runs with the other unit tests, so the site can never drift
 * from the app.
 */
class ContentExportTest {
    private val out = File("build/site-content").apply { mkdirs() }
    private val json = Json { prettyPrint = true }
    private val stringNames: Map<Int, String> = R.string::class.java.declaredFields
        .filter { it.type == Int::class.javaPrimitiveType }
        .associate { it.getInt(null) to it.name }

    private fun bi(b: Bi): JsonElement = buildJsonObject { put("en", b.en); put("cs", b.cs) }
    private fun res(id: Int): JsonElement = JsonPrimitive(stringNames[id] ?: id.toString())
    private fun strings(list: List<String>): JsonArray = JsonArray(list.map { JsonPrimitive(it) })
    private fun write(name: String, element: JsonElement) = File(out, name).writeText(json.encodeToString(JsonElement.serializer(), element))

    @Test fun exportContentForTheWebsite() {
        write("guide.json", buildJsonArray {
            GuideData.entries.forEach { e ->
                add(buildJsonObject {
                    put("key", e.key); put("kind", e.kind.name); put("name", bi(e.name)); put("latin", e.latin)
                    put("symptoms", bi(e.symptoms)); put("conditions", bi(e.conditions)); put("action", bi(e.action))
                    put("images", strings(e.images)); put("productCategory", e.productCategory?.name ?: "")
                    put("links", buildJsonArray { GuideLinks.byKey[e.key].orEmpty().forEach { l -> add(buildJsonObject { put("title", l.title); put("url", l.url) }) } })
                })
            }
        })
        write("phenology.json", buildJsonArray {
            Phenology.stages.forEach { s ->
                add(buildJsonObject {
                    put("stage", s.stage.name); put("label", res(s.stage.labelRes)); put("bbch", s.stage.bbch)
                    put("look", bi(s.look)); put("todo", bi(s.todo)); put("timing", bi(s.timing))
                    put("sprayWindow", s.sprayWindow ?: ""); put("images", strings(s.images))
                })
            }
        })
        write("varieties.json", buildJsonArray {
            Varieties.all.forEach { v ->
                add(buildJsonObject {
                    put("name", v.name); put("aliases", strings(v.aliases)); put("style", v.style.name)
                    put("ripening", buildJsonObject { put("en", v.ripening.en); put("cs", v.ripening.cs) })
                    put("harvestFrom", v.harvestFrom); put("harvestTo", v.harvestTo); put("targetNm", v.targetNm)
                    put("risk", buildJsonObject { put("peronospora", v.risk.peronospora.name); put("oidium", v.risk.oidium.name); put("botrytis", v.risk.botrytis.name); put("phomopsis", v.risk.phomopsis.name) })
                    put("note", bi(v.note)); put("piwi", v.piwi)
                })
            }
        })
        write("spray_program.json", buildJsonObject {
            put("targets", buildJsonArray { SprayTarget.entries.forEach { t -> add(buildJsonObject { put("key", t.name); put("label", bi(t.label)) }) } })
            put("windows", buildJsonArray {
                SprayProgram.windows.forEach { w ->
                    add(buildJsonObject {
                        put("key", w.key); put("stage", w.stage?.name ?: ""); put("name", bi(w.name)); put("bbch", w.bbch); put("timing", bi(w.timing))
                        put("targets", strings(w.targets.map { it.name })); put("advice", bi(w.advice)); put("month", w.month); put("day", w.day)
                        put("bs2025", buildJsonArray { w.bs2025.forEach { add(bi(it)) } })
                    })
                }
            })
        })
        write("cellar_templates.json", buildJsonArray {
            CellarTemplates.all.forEach { t ->
                add(buildJsonObject {
                    put("key", t.key); put("name", bi(t.name)); put("style", t.style.name); put("summary", bi(t.summary))
                    put("steps", buildJsonArray {
                        t.steps.forEach { s -> add(buildJsonObject { put("day", s.day); put("untilDay", s.untilDay?.let { JsonPrimitive(it) } ?: JsonNull); put("type", s.type.name); put("typeLabel", res(s.type.labelRes)); put("title", bi(s.title)); put("notes", bi(s.notes)) }) }
                    })
                })
            }
        })
        write("enums.json", buildJsonObject {
            put("entryTypes", buildJsonArray { EntryType.entries.forEach { add(buildJsonObject { put("key", it.name); put("domain", it.domain.name); put("label", res(it.labelRes)) }) } })
            put("stages", buildJsonArray { PhenologyStage.entries.forEach { add(buildJsonObject { put("key", it.name); put("label", res(it.labelRes)); put("bbch", it.bbch) }) } })
            put("measurementKinds", buildJsonArray { MeasurementKind.entries.forEach { add(buildJsonObject { put("key", it.name); put("label", res(it.labelRes)); put("unit", it.unit); put("domain", it.domain?.name ?: "") }) } })
            put("productCategories", buildJsonArray { ProductCategory.entries.forEach { add(buildJsonObject { put("key", it.name); put("label", res(it.labelRes)); put("domain", it.domain.name) }) } })
            put("wineStyles", buildJsonArray { WineStyle.entries.forEach { add(buildJsonObject { put("key", it.name); put("label", res(it.labelRes)) }) } })
        })
        write("sources.json", buildJsonArray {
            Sources.all.forEach { t ->
                add(buildJsonObject {
                    put("key", t.key); put("title", bi(t.title)); put("intro", bi(t.intro))
                    put("items", buildJsonArray { t.items.forEach { i -> add(buildJsonObject { put("title", i.title); put("url", i.url); put("note", bi(i.note)) }) } })
                })
            }
        })
        write("steberla.json", buildJsonObject {
            put("firstDay", Steberla.FIRST_DAY); put("lastDay", Steberla.LAST_DAY)
            put("curveA", buildJsonArray { (0..Steberla.LAST_DAY).forEach { add(JsonPrimitive(Steberla.a(it))) } })
            put("curveB", buildJsonArray { (0..Steberla.LAST_DAY).forEach { add(JsonPrimitive(Steberla.b(it))) } })
        })
        assertTrue(File(out, "guide.json").length() > 1000)
    }
}

package dev.androidpowertoys.app

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow

private fun writeAtomic(file: AtomicFile, text: String) {
    val stream = file.startWrite()
    try { stream.write(text.toByteArray()); file.finishWrite(stream) } catch(e: Exception) { file.failWrite(stream); throw e }
}
object BaselineJson {
    fun encode(b: Baseline): String = JSONObject().put("version",1).put("capturedAt",b.capturedAt).put("user",b.user).put("build",b.build).put("values",JSONArray().apply {
        b.values.forEach { (key, cell) -> put(JSONObject().put("table",key.table.name).put("name",key.name).put("state",cell.json())) }
    }).toString()
    fun decode(text: String): Baseline {
        val o=JSONObject(text); require(o.getInt("version")==1)
        val rows=o.getJSONArray("values")
        val values=buildMap { for(i in 0 until rows.length()) { val row=rows.getJSONObject(i); val key=SettingKey(Table.valueOf(row.getString("table")),row.getString("name")); require(!containsKey(key)); put(key,cellFromJson(row.getJSONObject("state"))) } }
        return Baseline(o.getLong("capturedAt"),o.getString("user"),o.getString("build"),values)
    }
}
class BaselineStore(context: Context) {
    private val file=AtomicFile(File(context.noBackupFilesDir,"starting-baseline.json"))
    fun loadOrCapture(backend: AndroidSettings): Baseline {
        if(file.baseFile.exists() || File(file.baseFile.path+".bak").exists()) return BaselineJson.decode(file.openRead().bufferedReader().use { it.readText() })
        val snapshot=backend.snapshot()
        check(snapshot.tables.values.all { it.error == null }) { "Cannot capture starting baseline while a table is unreadable. Editing is disabled; refresh to retry." }
        val values=snapshot.tables.flatMap { (table, read) -> read.rows.map { SettingKey(table,it.name) to it.cell } }.toMap().toMutableMap()
        // Exact reads record absence only where Android actually confirms it.
        explanations.keys.forEach { (table,name) -> val key=SettingKey(table,name); if(key !in values) runCatching { backend.read(table,name) }.getOrNull()?.let { values[key]=it } }
        val baseline=Baseline(snapshot.capturedAt,snapshot.user,Build.FINGERPRINT,values)
        writeAtomic(file,BaselineJson.encode(baseline))
        return baseline
    }
}
class FileTrialStore(context: Context): TrialStore {
    private val file=AtomicFile(File(context.noBackupFilesDir,"pending-trial.json"))
    override fun get(): Trial? {
        if(!file.baseFile.exists() && !File(file.baseFile.path+".bak").exists()) return null
        val o=JSONObject(file.openRead().bufferedReader().use { it.readText() })
        if(o.optBoolean("cleared")) return null
        return Trial(o.getString("id"),Table.valueOf(o.getString("table")),o.getString("name"),cellFromJson(o.getJSONObject("before")),cellFromJson(o.getJSONObject("target")),o.getLong("deadline"),if(o.isNull("problem")) null else o.getString("problem"))
    }
    override fun save(trial: Trial?) {
        val o=if(trial==null) JSONObject().put("cleared",true) else JSONObject().put("id",trial.id).put("table",trial.table.name).put("name",trial.name).put("before",trial.before.json()).put("target",trial.target.json()).put("deadline",trial.deadline).put("problem",trial.problem ?: JSONObject.NULL)
        writeAtomic(file,o.toString())
    }
}
/** Shared by UI and short foreground service. All mutation/storage access is synchronized on this instance. */
class SafetyRepository(context: Context) {
    val backend=AndroidSettings(context)
    val journal=FileJournal(context)
    private val baselineStore=BaselineStore(context)
    var baseline: Baseline?=null
        private set
    val trials=FileTrialStore(context)
    val guard=TrialEngine(backend,journal,trials)
    val revision=MutableStateFlow(0L)
    var message: String?=null
    init {
        // A persisted trial from a prior process never silently becomes a kept change.
        if(trials.get()!=null) message=guard.revert()
    }
    fun ensureBaseline() { if(baseline==null) baseline=baselineStore.loadOrCapture(backend) }
    fun publish(text: String) { message=text; revision.value+=1 }
    fun apply(table: Table,name: String,expected: Cell,target: Cell,undoOf: String?=null): Change {
        ensureBaseline()
        check(trials.get()==null) { "Resolve the pending display trial before making another change." }
        validateEdit(table,name,target)
        check(riskFor(table,name)!=Risk.DISPLAY) { "Display edits must use the timed foreground trial." }
        return ChangeEngine(backend,journal).apply(table,name,expected,target,undoOf)
    }
}
object SafetyRepositories {
    @Volatile private var repository: SafetyRepository?=null
    fun get(context: Context): SafetyRepository = repository ?: synchronized(this) { repository ?: SafetyRepository(context.applicationContext).also { repository=it } }
}

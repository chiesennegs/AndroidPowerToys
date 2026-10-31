package dev.androidpowertoys.app

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun Table.uri(): Uri = when(this) { Table.SYSTEM -> Settings.System.CONTENT_URI; Table.SECURE -> Settings.Secure.CONTENT_URI; Table.GLOBAL -> Settings.Global.CONTENT_URI }
class AndroidSettings(private val context: Context): SettingsBackend {
    val secureGranted get() = context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    val systemGranted get() = Settings.System.canWrite(context) || secureGranted
    fun eligible(table: Table) = if (table == Table.SYSTEM) systemGranted else secureGranted
    override fun read(table: Table, name: String): Cell {
        context.contentResolver.query(table.uri(), arrayOf("name", "value"), "name=?", arrayOf(name), null)?.use { cursor ->
            return if (cursor.moveToFirst()) Cell(true, if(cursor.isNull(1)) null else cursor.getString(1)) else Cell.Absent
        }
        error("Provider returned no cursor; existence is unknown.")
    }
    fun list(table: Table): TableRead = try {
        val rows = mutableListOf<Row>()
        context.contentResolver.query(table.uri(), arrayOf("name", "value"), null, null, null)?.use { c ->
            while(c.moveToNext()) rows += Row(c.getString(0), Cell(true, if(c.isNull(1)) null else c.getString(1)))
        } ?: error("Provider returned no cursor")
        TableRead(rows.sortedBy { it.name }, coverage = "provider-visible subset; secure-grant=$secureGranted; api=${android.os.Build.VERSION.SDK_INT}")
    } catch(e: Exception) { TableRead(emptyList(), e.message ?: "Table is restricted") }
    override fun write(table: Table, name: String, target: Cell): Boolean {
        check(eligible(table)) { "Required write permission is not granted." }
        check(keyWriteSupported(table, name)) { "Android 17 prevents third-party apps from writing non-public System keys, including arbitrary new keys." }
        check(table != Table.SYSTEM || target.exists) { "Android 17 prevents this app from deleting System settings." }
        if (!target.exists) return context.contentResolver.delete(table.uri(), "name=?", arrayOf(name)) > 0
        // Explicit CRUD preserves table identity and existing-null versus absent states.
        val values = ContentValues().apply { put("name", name); if(target.value == null) putNull("value") else put("value", target.value) }
        return context.contentResolver.insert(table.uri(), values) != null
    }
    fun snapshot() = Snapshot("android-user-serial:${context.getSystemService(android.os.UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())}", System.currentTimeMillis(), Table.entries.associateWith { list(it) })
}
fun Cell.json() = JSONObject().put("exists", exists).put("value", value ?: JSONObject.NULL)
fun cellFromJson(o: JSONObject): Cell {
    val exists = o.getBoolean("exists")
    require(o.has("value")) { "Missing value field" }
    require(o.isNull("value") || o.get("value") is String) { "Setting values must be strings or null" }
    return Cell(exists, if(o.isNull("value")) null else o.getString("value"))
}
object SnapshotJson {
    fun encode(s: Snapshot): String = JSONObject().put("format", "android-powertoys").put("version", 1).put("user", s.user).put("capturedAt", s.capturedAt)
        .put("note", "Visible rows only; not a full Android configuration backup. Capture is not atomic. Exported files are controlled by the selected document provider.")
        .put("tables", JSONObject().apply { s.tables.forEach { (t, r) -> put(t.name, JSONObject().put("coverage", r.coverage).put("error", r.error ?: JSONObject.NULL).put("rows", JSONArray().apply { r.rows.forEach { put(JSONObject().put("name", it.name).put("state", it.cell.json())) } })) } }).toString(2)
    fun decode(text: String): Snapshot {
        val o = JSONObject(text)
        require(o.getString("format") == "android-powertoys" && o.getInt("version") == 1) { "Unsupported snapshot format" }
        val tables = o.getJSONObject("tables")
        return Snapshot(o.getString("user"), o.getLong("capturedAt"), Table.entries.associateWith { t ->
            val data = tables.getJSONObject(t.name)
            val rows = data.getJSONArray("rows")
            require(rows.length() <= 50000) { "Too many rows" }
            val parsed = (0 until rows.length()).map { i -> val row = rows.getJSONObject(i); Row(row.getString("name"), cellFromJson(row.getJSONObject("state"))) }
            require(parsed.all { it.cell.exists } && parsed.map { it.name }.distinct().size == parsed.size) { "Invalid or duplicate rows" }
            TableRead(parsed, if(data.isNull("error")) null else data.getString("error"), data.getString("coverage"))
        })
    }
}
class FileJournal(context: Context): Journal {
    private val file = AtomicFile(File(context.noBackupFilesDir, "history.json"))
    private var changes: List<Change> = if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) emptyList() else {
        val arr = JSONArray(file.openRead().bufferedReader().use { it.readText() })
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); Change(o.getString("id"), o.getLong("time"), Table.valueOf(o.getString("table")), o.getString("name"), cellFromJson(o.getJSONObject("before")), cellFromJson(o.getJSONObject("target")), o.getString("status"), o.getString("message"), if(o.isNull("undoOf")) null else o.getString("undoOf")) }
    }
    override fun all() = changes.toList()
    override fun save(change: Change) {
        val updated = changes.filterNot { it.id == change.id } + change
        val arr = JSONArray().apply { updated.forEach { c -> put(JSONObject().put("id", c.id).put("time", c.time).put("table", c.table.name).put("name", c.name).put("before", c.before.json()).put("target", c.target.json()).put("status", c.status).put("message", c.message).put("undoOf", c.undoOf ?: JSONObject.NULL)) } }
        val stream = file.startWrite()
        try { stream.write(arr.toString().toByteArray()); file.finishWrite(stream); changes = updated } catch(e: Exception) { file.failWrite(stream); throw e }
    }
}

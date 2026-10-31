package dev.androidpowertoys.app

import android.app.Application
import android.net.Uri
import android.content.Intent
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppState(
    val tables: Map<Table, TableRead> = emptyMap(), val history: List<Change> = emptyList(),
    val system: Boolean = false, val secure: Boolean = false,
    val busy: Boolean = false, val message: String? = null, val storageError: String? = null,
    val baseline: Baseline? = null, val trial: Trial? = null,
    val first: Snapshot? = null, val second: Snapshot? = null, val diffs: List<Difference>? = null
)
class AppModel(application: Application): AndroidViewModel(application) {
    val backend = AndroidSettings(application)
    private var repo: SafetyRepository? = null
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    init {
        runTask {
            try {
                repo=SafetyRepositories.get(application)
                synchronized(repo!!) { repo!!.ensureBaseline() }
                viewModelScope.launch { repo!!.revision.collect { runTask { load() } } }
            } catch(e: Exception) { mutable.value=mutable.value.copy(storageError="Safety storage unavailable; editing disabled: ${e.message}") }
            load()
        }
    }
    private fun runTask(block: suspend () -> Unit) { viewModelScope.launch { mutex.withLock { mutable.value = mutable.value.copy(busy = true); try { withContext(Dispatchers.IO) { block() } } catch(e: Exception) { mutable.value = mutable.value.copy(message = e.message ?: "Operation failed") } finally { mutable.value = mutable.value.copy(busy = false) } } } }
    private fun load() {
        mutable.value = mutable.value.copy(tables = Table.entries.associateWith { backend.list(it) }, history = repo?.let { synchronized(it) { it.journal.all().reversed() } }.orEmpty(), baseline = repo?.baseline, trial = repo?.let { synchronized(it) { it.trials.get() } }, system = backend.systemGranted, secure = backend.secureGranted,
            message = repo?.message ?: mutable.value.message)
    }
    fun refresh() = runTask { repo?.let { synchronized(it) { it.ensureBaseline() }; mutable.value=mutable.value.copy(storageError=null) }; load() }
    fun clearMessage() { repo?.let { synchronized(it) { it.message=null } }; mutable.value = mutable.value.copy(message = null) }
    fun inspect(table: Table, name: String, done: (Cell) -> Unit) = runTask {
        val cell = backend.read(table, name)
        withContext(Dispatchers.Main) { done(cell) }
    }
    fun change(table: Table, name: String, expected: Cell, target: Cell, undoOf: String? = null) = runTask {
        val repository=repo ?: error("Safety storage unavailable")
        validateEdit(table,name,target)
        if(riskFor(table,name)==Risk.DISPLAY) {
            val intent=Intent(getApplication(),RevertService::class.java).setAction("begin")
                .putExtra("table",table.name).putExtra("name",name).putExtra("before",expected.json().toString()).putExtra("target",target.json().toString()).putExtra("undoOf",undoOf)
            withContext(Dispatchers.Main) { getApplication<Application>().startForegroundService(intent) }
        } else {
            val result=synchronized(repository) { repository.apply(table,name,expected,target,undoOf).also { repository.publish("${it.status}: ${it.message}") } }
            load(); mutable.value=mutable.value.copy(message="${result.status}: ${result.message}")
        }
    }
    fun trialAction(action: String) = runTask {
        val repository=repo ?: error("Safety storage unavailable")
        synchronized(repository) {
            if(action=="keep") { repository.guard.keep(android.os.SystemClock.elapsedRealtime()); repository.publish("Display change kept after your visibility confirmation.") }
            else if(action=="abandon") { repository.guard.abandonRecovery(); repository.publish("Automatic recovery stopped at your request. The original is retained in history as RECOVERY_REQUIRED. Current state has not been restored.") }
            else repository.publish(repository.guard.revert())
        }
        load()
    }
    fun undo(change: Change) {
        val repository=repo ?: return
        synchronized(repository) {
            if(repository.journal.all().any { it.undoOf==change.id && it.status=="VERIFIED" }) { mutable.value=mutable.value.copy(message="Already undone"); return }
        }
        change(change.table,change.name,change.target,change.before,change.id)
    }
    fun export(uri: Uri) = runTask {
        val snapshot = backend.snapshot()
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(SnapshotJson.encode(snapshot).toByteArray()) } ?: error("Cannot open document")
        mutable.value = mutable.value.copy(message = "Snapshot exported. It contains only provider-visible rows; check table errors and coverage in the JSON.")
    }
    fun import(uri: Uri, slot: Int) = runTask {
        val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while(output.size() <= 8 * 1024 * 1024) { val count = input.read(buffer); if(count < 0) break; output.write(buffer, 0, count) }
            output.toByteArray() } ?: error("Cannot open document")
        require(bytes.size <= 8 * 1024 * 1024) { "Snapshot exceeds the 8 MiB limit" }
        val snapshot = SnapshotJson.decode(bytes.toString(Charsets.UTF_8))
        mutable.value = if(slot == 1) mutable.value.copy(first = snapshot, diffs = null) else mutable.value.copy(second = snapshot, diffs = null)
    }
    fun compare() = runTask {
        val s = mutable.value
        val differences = compareSnapshots(s.first ?: error("Choose snapshot A"), s.second ?: error("Choose snapshot B"))
        mutable.value = mutable.value.copy(diffs = differences, message = if(differences.isEmpty()) "No differences in the visible rows." else "${differences.size} visible differences")
    }
}

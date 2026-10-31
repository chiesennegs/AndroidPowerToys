package dev.androidpowertoys.app

import java.util.UUID

enum class Table(val label: String, val scope: String) {
    SYSTEM("System", "Current user"), SECURE("Secure", "Current user"), GLOBAL("Global", "Device-wide")
}
data class Cell(val exists: Boolean, val value: String? = null) {
    init { require(exists || value == null) }
    fun display(): String = when { !exists -> "Absent"; value == null -> "Present · null"; value.isEmpty() -> "Empty string (\"\")"; else -> value }
    companion object { val Absent = Cell(false) }
}
data class Row(val name: String, val cell: Cell)
data class TableRead(val rows: List<Row>, val error: String? = null, val coverage: String = "provider-visible subset")
data class Snapshot(val user: String, val capturedAt: Long, val tables: Map<Table, TableRead>)
data class Difference(val table: Table, val name: String, val before: Cell, val after: Cell) {
    val kind: String get() = if (!before.exists) "Added" else if (!after.exists) "Removed" else "Changed"
}
fun compareSnapshots(a: Snapshot, b: Snapshot): List<Difference> {
    require(a.user == b.user) { "Snapshots belong to different users; comparison would be misleading." }
    require(a.tables.keys == b.tables.keys) { "Snapshots must contain the same tables." }
    return a.tables.flatMap { (table, first) ->
        val second = b.tables.getValue(table)
        require(first.error == null && second.error == null) { "Cannot compare an unreadable or incomplete table: ${table.label}." }
        require(first.coverage == second.coverage) { "Table coverage differs; capture again with the same access." }
        val old = first.rows.associate { it.name to it.cell }
        val new = second.rows.associate { it.name to it.cell }
        (old.keys + new.keys).sorted().mapNotNull { name ->
            val before = old[name] ?: Cell.Absent
            val after = new[name] ?: Cell.Absent
            if (before == after) null else Difference(table, name, before, after)
        }
    }
}
interface SettingsBackend {
    fun read(table: Table, name: String): Cell
    fun write(table: Table, name: String, target: Cell): Boolean
}
data class Change(
    val id: String = UUID.randomUUID().toString(), val time: Long = System.currentTimeMillis(),
    val table: Table, val name: String, val before: Cell, val target: Cell,
    val status: String = "PENDING", val message: String = "Recorded before write; result not yet verified.",
    val undoOf: String? = null
)
interface Journal { fun all(): List<Change>; fun save(change: Change) }
/** Calls are serialized by the owner. Persist the original before touching the provider. */
class ChangeEngine(private val backend: SettingsBackend, private val journal: Journal) {
    fun apply(table: Table, name: String, expected: Cell, target: Cell, undoOf: String? = null, id: String = UUID.randomUUID().toString()): Change {
        require(name.isNotBlank() && name.length <= 512 && !name.any { it.isISOControl() }) { "Use a nonempty setting name without control characters (max 512)." }
        val current = backend.read(table, name) // Failure is not absence.
        check(current == expected) { "The setting changed since you opened it. Refresh and review the new value." }
        check(current != target) { "No change requested." }
        val pending = Change(id = id, table = table, name = name, before = current, target = target, undoOf = undoOf)
        journal.save(pending) // If durability fails, never attempt the write.
        var accepted = false
        var writeError: String? = null
        try { accepted = backend.write(table, name, target) } catch (e: Exception) { writeError = e.message ?: e.javaClass.simpleName }
        val outcome = try {
            val observed = backend.read(table, name)
            when {
                accepted && observed == target -> pending.copy(status = "VERIFIED", message = "Read-back confirmed the requested state.")
                observed == current -> pending.copy(status = "FAILED", message = writeError ?: "Provider rejected the operation or read-back stayed unchanged.")
                else -> pending.copy(status = "UNVERIFIED", message = "Read-back: ${observed.display()}. ${writeError ?: "Requested state was not confirmed with provider acceptance."}")
            }
        } catch (e: Exception) { pending.copy(status = "UNVERIFIED", message = "Could not read back: ${e.message}. ${writeError.orEmpty()}") }
        journal.save(outcome)
        return outcome
    }
    fun undo(change: Change): Change {
        check(change.status == "VERIFIED") { "Only verified changes support undo. Inspect pending or unverified writes manually." }
        check(journal.all().none { it.undoOf == change.id && it.status == "VERIFIED" }) { "This change has already been undone." }
        return apply(change.table, change.name, change.target, change.before, change.id)
    }
}

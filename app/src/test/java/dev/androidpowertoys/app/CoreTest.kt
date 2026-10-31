package dev.androidpowertoys.app

import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    private class MemoryJournal: Journal { val entries = mutableListOf<Change>(); override fun all() = entries.toList(); override fun save(change: Change) { entries.removeAll { it.id == change.id }; entries += change } }
    private class Fake(var cell: Cell): SettingsBackend {
        var behavior = "ok"; var wrote = false; var failReadAfterWrite = false; var beforeWrite: () -> Unit = {}
        override fun read(table: Table, name: String): Cell { if(wrote && failReadAfterWrite) error("Read denied"); return cell }
        override fun write(table: Table, name: String, target: Cell): Boolean { beforeWrite(); wrote = true; when(behavior) { "throw" -> error("Permission denied"); "false" -> return false; "ignore" -> return true; "different" -> { cell = Cell(true, "system override"); return true }; "falseButChanged" -> { cell = target; return false } }; cell = target; return true }
    }
    private fun snapshot(vararg rows: Row) = Snapshot("android-user:0", 1, Table.entries.associateWith { TableRead(if(it == Table.SECURE) rows.toList() else emptyList()) })
    @Test fun absentEmptyZeroAndNullAreDistinct() { val states = setOf(Cell.Absent, Cell(true, ""), Cell(true, "0"), Cell(true, null)); assertEquals(4, states.size); states.forEach { assertEquals(it, cellFromJson(it.json())) } }
    @Test fun diffAddedRemovedChanged() {
        val a = snapshot(Row("removed", Cell(true, "")), Row("changed", Cell(true, "0")), Row("same", Cell(true, "")))
        val b = snapshot(Row("added", Cell(true, "0")), Row("changed", Cell(true, "")), Row("same", Cell(true, "")))
        val diff = compareSnapshots(a,b).associateBy { it.name }
        assertEquals(3, diff.size); assertEquals("Added", diff.getValue("added").kind); assertEquals("Removed", diff.getValue("removed").kind); assertEquals(Cell(true, ""), diff.getValue("changed").after)
    }
    @Test fun snapshotRoundTrip() { val s = snapshot(Row("null",Cell(true,null)), Row("empty",Cell(true,"")), Row("zero",Cell(true,"0"))); assertEquals(s, SnapshotJson.decode(SnapshotJson.encode(s))) }
    @Test fun compareRejectsDifferentUserOrFailedTable() {
        val a = snapshot(); assertThrows(IllegalArgumentException::class.java) { compareSnapshots(a,a.copy(user="android-user:10")) }
        assertThrows(IllegalArgumentException::class.java) { compareSnapshots(a,a.copy(tables=a.tables + (Table.SECURE to TableRead(emptyList(),"Denied")))) }
        assertThrows(IllegalArgumentException::class.java) { compareSnapshots(a,a.copy(tables=a.tables + (Table.SECURE to TableRead(emptyList(),coverage="different permission")))) }
    }
    @Test fun originalIsDurablySavedBeforeWriting() {
        val backend = Fake(Cell(true,"")); val journal = MemoryJournal(); backend.beforeWrite = { assertEquals("PENDING",journal.all().single().status); assertEquals(Cell(true,""),journal.all().single().before) }
        assertEquals("VERIFIED",ChangeEngine(backend,journal).apply(Table.SECURE,"test",backend.cell,Cell(true,"0")).status)
    }
    @Test fun undoRestoresAbsenceEmptyZeroAndNull() { listOf(Cell.Absent, Cell(true,""),Cell(true,"0"),Cell(true,null)).forEach { original ->
        val backend=Fake(original); val journal=MemoryJournal(); val engine=ChangeEngine(backend,journal)
        val change=engine.apply(Table.SECURE,"test",original,Cell(true,"new")); assertEquals("VERIFIED",engine.undo(change).status); assertEquals(original,backend.cell)
        assertThrows(IllegalStateException::class.java) { engine.undo(change) }
    } }
    @Test fun undoRefusesConcurrentChange() { val b=Fake(Cell(true,"1")); val j=MemoryJournal(); val e=ChangeEngine(b,j); val c=e.apply(Table.SECURE,"test",b.cell,Cell(true,"0")); b.cell=Cell(true,"2"); assertThrows(IllegalStateException::class.java) { e.undo(c) }; assertEquals(Cell(true,"2"),b.cell) }
    @Test fun failedOrUnconfirmedWritesNeverSucceed() { listOf("throw","false","ignore","different","falseButChanged").forEach { behavior -> val b=Fake(Cell(true,"1")); b.behavior=behavior; val j=MemoryJournal(); val c=ChangeEngine(b,j).apply(Table.SECURE,"test",b.cell,Cell(true,"0")); assertNotEquals(behavior,"VERIFIED",c.status); assertEquals(Cell(true,"1"),c.before) } }
    @Test fun readBackFailureIsUnverified() { val b=Fake(Cell.Absent); b.failReadAfterWrite=true; assertEquals("UNVERIFIED",ChangeEngine(b,MemoryJournal()).apply(Table.SECURE,"test",b.cell,Cell(true,"")).status) }
    @Test fun journalFailurePreventsWrite() { val b=Fake(Cell.Absent); val j=object: Journal { override fun all()=emptyList<Change>(); override fun save(change: Change) { error("Disk full") } }; assertThrows(IllegalStateException::class.java) { ChangeEngine(b,j).apply(Table.SECURE,"test",b.cell,Cell(true,"0")) }; assertFalse(b.wrote) }
    @Test fun staleOriginalPreventsWrite() { val b=Fake(Cell(true,"2")); assertThrows(IllegalStateException::class.java) { ChangeEngine(b,MemoryJournal()).apply(Table.SECURE,"test",Cell(true,"1"),Cell(true,"0")) }; assertFalse(b.wrote) }
}

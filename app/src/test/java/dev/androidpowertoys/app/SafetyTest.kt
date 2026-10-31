package dev.androidpowertoys.app

import org.junit.Assert.*
import org.junit.Test

class SafetyTest {
    private class Backend(var cell: Cell): SettingsBackend {
        var writes=0; var reject=false; var onWrite: () -> Unit = {}; var unreadable=false
        override fun read(table: Table,name: String): Cell { if(unreadable) error("Access lost"); return cell }
        override fun write(table: Table,name: String,target: Cell): Boolean { writes++; onWrite(); if(reject) return false; cell=target; return true }
    }
    private class History: Journal { val list=mutableListOf<Change>(); override fun all()=list.toList(); override fun save(change: Change) { list.removeAll { it.id==change.id }; list+=change } }
    private class Pending: TrialStore { var value: Trial?=null; override fun get()=value; override fun save(trial: Trial?) { value=trial } }
    private fun fixture()=Triple(Backend(Cell(true,"102")),History(),Pending())
    @Test fun baselinePreservesUnknownVersusAbsentEmptyNullZero() {
        val values=mapOf(SettingKey(Table.SECURE,"absent") to Cell.Absent,SettingKey(Table.SECURE,"empty") to Cell(true,""),SettingKey(Table.SECURE,"null") to Cell(true,null),SettingKey(Table.SECURE,"zero") to Cell(true,"0"))
        val baseline=Baseline(123,"user:0","build",values)
        assertEquals(baseline,BaselineJson.decode(BaselineJson.encode(baseline)))
        assertNull(baselineValue(baseline,Table.SECURE,"unknown"))
        assertEquals(Cell.Absent,baselineValue(baseline,Table.SECURE,"absent"))
    }
    @Test fun referenceDifferenceRequiresEvidenceAndNormalizesNumericFormatting() {
        assertEquals("Comparison unavailable",referenceComparison(null,Cell(true,"0")))
        assertEquals("Comparison unavailable",referenceComparison(Cell(true,"0"),null))
        assertEquals("Starting value matches AOSP reference",referenceComparison(Cell(true,"1"),Cell(true,"1.0")))
        assertEquals("Starting value differs from AOSP reference",referenceComparison(Cell.Absent,Cell(true,"")))
        assertEquals("Starting value differs from AOSP reference",referenceComparison(Cell(true,""),Cell(true,"0")))
    }
    @Test fun timeoutRestoresOriginalAndRecordsUndo() {
        val (b,j,s)=fixture(); val e=TrialEngine(b,j,s)
        b.onWrite={ assertNotNull("Pending recovery must be durable before provider write",s.get()) }
        val c=e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"1"),1000)
        assertEquals("VERIFIED",c.status); assertEquals(16000,s.get()!!.deadline)
        assertNull(e.tick(15999)); assertEquals(Cell(true,"1"),b.cell)
        assertNotNull(e.tick(16000)); assertEquals(Cell(true,"102"),b.cell); assertNull(s.get())
        assertEquals(c.id,j.all().last().undoOf)
    }
    @Test fun confirmationKeepsValueAndDoesNotLaterRevert() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); e.keep(14999); assertNull(s.get()); assertNull(e.tick(20000)); assertEquals(Cell(true,"120"),b.cell) }
    @Test fun lateConfirmationIsRefused() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); assertThrows(IllegalStateException::class.java) { e.keep(15000) }; e.tick(15000); assertEquals(Cell(true,"102"),b.cell) }
    @Test fun newerValueIsNeverOverwrittenByRollback() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); b.cell=Cell(true,"140"); e.tick(15000); assertEquals(Cell(true,"140"),b.cell); assertNotNull(s.get()!!.problem); assertThrows(IllegalStateException::class.java) { e.keep(1000) } }
    @Test fun failedRollbackRetainsRecoverableOriginal() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); b.reject=true; e.tick(15000); assertEquals(Cell(true,"102"),s.get()!!.before); assertNotNull(s.get()!!.problem); b.reject=false; e.revert(); assertEquals(Cell(true,"102"),b.cell); assertNull(s.get()) }
    @Test fun readFailureNeverClaimsReverted() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); b.unreadable=true; e.tick(15000); assertNotNull(s.get()!!.problem); b.unreadable=false; e.revert(); assertNull(s.get()) }
    @Test fun interruptedProcessRecoversFromSameDurableRecord() { val (b,j,s)=fixture(); TrialEngine(b,j,s).begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); TrialEngine(b,j,s).revert(); assertEquals(Cell(true,"102"),b.cell); assertNull(s.get()) }
    @Test fun interruptedBeforeMutationOnlyClearsPending() { val (b,j,s)=fixture(); s.save(Trial("x",Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),15000)); TrialEngine(b,j,s).revert(); assertEquals(0,b.writes); assertNull(s.get()) }
    @Test fun rejectedTrialDoesNotRemainActive() { val (b,j,s)=fixture(); b.reject=true; assertEquals("FAILED",TrialEngine(b,j,s).begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0).status); assertNull(s.get()) }
    @Test fun secondTrialCannotOverwriteOriginal() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); val original=s.get(); assertThrows(IllegalStateException::class.java) { e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"150"),100) }; assertEquals(original,s.get()) }
    @Test fun trialRequiresRestorableOriginalAndBoundedValue() { val (b,j,s)=fixture(); assertThrows(IllegalStateException::class.java) { TrialEngine(b,j,s).begin(Table.SYSTEM,"screen_brightness",Cell.Absent,Cell(true,"120"),0) }; listOf("0","256","NaN","Infinity","1.5","").forEach { value -> assertThrows(value,IllegalArgumentException::class.java) { validateEdit(Table.SYSTEM,"screen_brightness",Cell(true,value)) } }; assertEquals(0,b.writes) }
    @Test fun safetyCategoriesDoNotInventSafeUnknowns() { assertEquals(Risk.UNKNOWN,riskFor(Table.SECURE,"unrecognized_key")); assertEquals(Risk.HIGH,riskFor(Table.SECURE,"browser_content_filters_enabled")); assertEquals(Risk.DISPLAY,riskFor(Table.SYSTEM,"font_scale")); listOf("default_input_method","adb_enabled","accessibility_enabled","display_size_forced").forEach { key -> assertEquals(Risk.BLOCKED,riskFor(Table.SECURE,key)); assertThrows(IllegalStateException::class.java) { validateEdit(Table.SECURE,key,Cell(true,"0")) } } }
    @Test fun noGuessedFilteringFactoryDefault() { assertNull(explanations[Table.SECURE to "browser_content_filters_enabled"]!!.aospDefault); assertNull(explanations[Table.SECURE to "search_content_filters_enabled"]!!.aospDefault) }
    @Test fun durableTrialSaveFailurePreventsWrite() { val (b,j,_)=fixture(); val store=object:TrialStore { override fun get():Trial?=null; override fun save(trial:Trial?) { error("Disk full") } }; assertThrows(IllegalStateException::class.java) { TrialEngine(b,j,store).begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0) }; assertEquals(0,b.writes) }
    @Test fun failedRecoveryCanBeHandedOffWithoutLosingOriginal() { val (b,j,s)=fixture(); val e=TrialEngine(b,j,s); e.begin(Table.SYSTEM,"screen_brightness",b.cell,Cell(true,"120"),0); assertThrows(IllegalStateException::class.java) { e.abandonRecovery() }; b.reject=true; e.tick(15000); val writes=b.writes; e.abandonRecovery(); assertNull(s.get()); assertEquals(writes,b.writes); assertEquals("RECOVERY_REQUIRED",j.all().last().status); assertEquals(Cell(true,"102"),j.all().last().before) }

}

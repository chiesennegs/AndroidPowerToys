package dev.androidpowertoys.app

import java.math.BigDecimal
import java.util.UUID

data class SettingKey(val table: Table, val name: String)
data class Baseline(val capturedAt: Long, val user: String, val build: String, val values: Map<SettingKey, Cell>)
// A missing entry means unknown, never an inferred absent row.
fun baselineValue(baseline: Baseline?, table: Table, name: String): Cell? = baseline?.values?.get(SettingKey(table, name))
fun referenceComparison(start: Cell?, reference: Cell?): String {
    if(start == null || reference == null) return "Comparison unavailable"
    val equivalent = if(start.exists && reference.exists && start.value != null && reference.value != null) {
        try { BigDecimal(start.value).compareTo(BigDecimal(reference.value)) == 0 } catch(_: NumberFormatException) { start == reference }
    } else start == reference
    return if(equivalent) "Starting value matches AOSP reference" else "Starting value differs from AOSP reference"
}
enum class Risk(val label: String) { CAUTION("Caution · system behavior"), HIGH("High impact · review carefully"), DISPLAY("Display / usability · 15-second trial"), BLOCKED("Recovery risk · editing blocked"), UNKNOWN("Unassessed · potential system effects") }
val trialKeys = setOf("screen_brightness", "screen_brightness_mode", "screen_off_timeout", "font_scale", "accelerometer_rotation", "user_rotation")
fun riskFor(table: Table, name: String): Risk {
    if(name in setOf("display_size_forced", "display_density_forced", "default_input_method", "enabled_input_methods", "enabled_accessibility_services", "touch_exploration_enabled", "adb_enabled", "adb_wifi_enabled", "development_settings_enabled", "device_provisioned", "user_setup_complete", "setup_wizard_has_run", "system_locales", "hidden_api_policy", "hidden_api_blacklist_exemptions") || name.startsWith("accessibility_")) return Risk.BLOCKED
    if(table == Table.SYSTEM && name in trialKeys) return Risk.DISPLAY
    return explanations[table to name]?.risk ?: Risk.UNKNOWN
}
fun validateEdit(table: Table, name: String, target: Cell) {
    require(table != Table.SYSTEM || target.exists) { "Android 17 does not permit this app to delete System rows; an absent System baseline cannot be restored by removing a row." }
    check(riskFor(table, name) != Risk.BLOCKED) { "This key can affect input, access, or recovery. Use Android's dedicated Settings screen; an app timer cannot make this change reliably recoverable." }
    if(riskFor(table, name) == Risk.DISPLAY) {
        require(target.exists && target.value != null) { "Display trials require an explicit value, not deletion or null." }
        val value = target.value.toDoubleOrNull()
        require(value != null && value.isFinite()) { "Enter a finite numeric display value." }
        val valid = when(name) {
            "font_scale" -> value in 0.5..2.0
            "screen_brightness" -> value in 1.0..255.0 && value % 1.0 == 0.0
            "screen_off_timeout" -> value in 10000.0..1800000.0 && value % 1.0 == 0.0
            "user_rotation" -> value in 0.0..3.0 && value % 1.0 == 0.0
            else -> value == 0.0 || value == 1.0
        }
        require(valid) { displayBounds(name) }
    }
}
fun displayBounds(name: String): String = when(name) {
    "font_scale" -> "This app limits font scale trials to 0.5–2.0."
    "screen_brightness" -> "This app limits brightness trials to integer values 1–255. Automatic brightness may override the effect."
    "screen_off_timeout" -> "This app limits screen timeout trials to 10000–1800000 milliseconds (10 seconds–30 minutes)."
    "user_rotation" -> "Use 0, 1, 2 or 3 for rotation; device support varies."
    else -> "Use 0 or 1."
}
data class Trial(val id: String, val table: Table, val name: String, val before: Cell, val target: Cell, val deadline: Long, val problem: String? = null)
interface TrialStore { fun get(): Trial?; fun save(trial: Trial?) }
/** One serialized owner; deadline uses a monotonic clock. Durable intent always precedes mutation. */
class TrialEngine(private val backend: SettingsBackend, private val journal: Journal, private val store: TrialStore) {
    fun begin(table: Table, name: String, expected: Cell, target: Cell, now: Long, undoOf: String? = null): Change {
        check(store.get() == null) { "Resolve the previous display trial first." }
        validateEdit(table,name,target)
        check(riskFor(table,name) == Risk.DISPLAY)
        // An absent/null original cannot be reliably restored through System CRUD.
        check(expected.exists && expected.value != null) { "A readable, present original is required for a reversible display trial." }
        val trial = Trial(UUID.randomUUID().toString(),table,name,expected,target,now + 15000)
        store.save(trial)
        try {
            val change = ChangeEngine(backend,journal).apply(table,name,expected,target,undoOf,trial.id)
            if(change.status != "VERIFIED") revert()
            return change
        } catch(e: Exception) { revert(); throw e }
    }
    fun keep(now: Long) {
        val trial = store.get() ?: error("No pending trial")
        check(trial.problem == null && now < trial.deadline) { "Confirmation expired or trial needs recovery. Restore the original value." }
        check(journal.all().any { it.id == trial.id && it.status == "VERIFIED" })
        check(backend.read(trial.table,trial.name) == trial.target) { "The setting changed again; cannot confirm this trial." }
        store.save(null)
    }
    fun revert(): String {
        val trial = store.get() ?: return "No pending trial."
        return try {
            val current = backend.read(trial.table,trial.name)
            when(current) {
                trial.before -> { store.save(null); "Original value confirmed; trial ended." }
                trial.target -> {
                    val result = ChangeEngine(backend,journal).apply(trial.table,trial.name,current,trial.before,trial.id)
                    if(result.status == "VERIFIED") { store.save(null); "Original value restored and read-back verified." }
                    else { store.save(trial.copy(problem = "Rollback ${result.status}: ${result.message}")); "Rollback needs attention: ${result.message}" }
                }
                else -> { val message = "Another writer changed this setting. Automatic rollback did not overwrite it. Inspect the current value and use manual recovery."; store.save(trial.copy(problem=message)); message }
            }
        } catch(e: Exception) { val message = "Rollback failed: ${e.message}. Original: ${trial.before.display()}."; store.save(trial.copy(problem=message)); message }
    }
    fun abandonRecovery() {
        val trial=store.get() ?: error("No pending trial")
        check(trial.problem!=null) { "An active display trial cannot be abandoned without visibility confirmation." }
        journal.save(Change(table=trial.table,name=trial.name,before=trial.before,target=trial.target,status="RECOVERY_REQUIRED",message="User chose manual recovery after: ${trial.problem}. Original value retained here; no restoration is claimed.",undoOf=trial.id))
        store.save(null)
    }
    fun tick(now: Long): String? = store.get()?.let { if(it.problem == null && now >= it.deadline) revert() else null }
}

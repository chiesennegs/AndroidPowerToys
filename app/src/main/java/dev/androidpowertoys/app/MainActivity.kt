package dev.androidpowertoys.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { PowerToysApp() } }
}
private fun date(time: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))
private fun copy(context: Context, text: String) { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Android PowerToys", text)) }
@Composable private fun Selectable(text: String, mono: Boolean = false) { SelectionContainer { Text(text, fontFamily = if(mono) FontFamily.Monospace else MaterialTheme.typography.bodyMedium.fontFamily, style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun Panel(title: String, text: String, content: @Composable ColumnScope.() -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Selectable(text); content() } }
}
private data class Editor(val table: Table, val name: String, val original: Cell, val requested: Cell? = null)
@OptIn(ExperimentalFoundationApi::class)
@Composable fun PowerToysApp(model: AppModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var licenses by remember { mutableStateOf(false) }
    var darkChoice by remember { mutableStateOf<Boolean?>(null) }
    val dark = darkChoice ?: isSystemInDarkTheme()
    val colors = mooseAiColors(dark)
    MaterialTheme(colorScheme = colors,typography=powerToysTypography,shapes=powerToysShapes) {
        val context = LocalContext.current
        SideEffect {
            (context as? android.app.Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window,activity.window.decorView).apply {
                    isAppearanceLightStatusBars=!dark
                    isAppearanceLightNavigationBars=!dark
                }
            }
        }
        val lifecycle = LocalLifecycleOwner.current
        DisposableEffect(lifecycle) { val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) model.refresh() }; lifecycle.lifecycle.addObserver(observer); onDispose { lifecycle.lifecycle.removeObserver(observer) } }
        var screen by remember { mutableStateOf("Tables") }
        var editMode by remember { mutableStateOf(false) }
        var editingSetup by rememberSaveable { mutableStateOf(false) }
        var table by remember { mutableStateOf(Table.SYSTEM) }
        var query by remember { mutableStateOf("") }
        val systemList=rememberLazyListState()
        val secureList=rememberLazyListState()
        val globalList=rememberLazyListState()
        val tableLists=mapOf(Table.SYSTEM to systemList,Table.SECURE to secureList,Table.GLOBAL to globalList)
        var actionsMenu by remember { mutableStateOf(false) }
        var sort by remember { mutableIntStateOf(0) }
        var editor by remember { mutableStateOf<Editor?>(null) }
        val keyboard=LocalSoftwareKeyboardController.current
        val focus=LocalFocusManager.current
        LaunchedEffect(editor) { if(editor!=null) { focus.clearFocus(); keyboard?.hide() } }
        var adding by remember { mutableStateOf(false) }
        var undo by remember { mutableStateOf<Change?>(null) }
        var importSlot by remember { mutableIntStateOf(1) }
        val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.refresh() }
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { if(it != null) model.export(it) }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if(it != null) model.import(it, importSlot) }
        Scaffold(modifier = Modifier.testTag("app-window"), bottomBar = {
            Surface(color=MaterialTheme.colorScheme.surfaceContainer,tonalElevation=3.dp) {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("Tables","History","Snapshots","About").forEach { label ->
                        val active=screen==label || (screen=="Defaults" && label=="Tables")
                        TextButton(onClick={ screen=label },modifier=Modifier.weight(1f).height(48.dp).semantics { selected=active },contentPadding=PaddingValues(horizontal=2.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(active) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                Icon(painterResource(when(label) { "Tables"->R.drawable.ic_tables;"History"->R.drawable.ic_history;"Snapshots"->R.drawable.ic_snapshots;else->R.drawable.ic_about }),null,Modifier.size(18.dp))
                                Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1)
                            }
                        }
                    }
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                ProductHeader(screen,if(screen=="Tables") table else null,editMode,onTable={ table=it }) {
                    if(screen=="Tables") {
                        Box {
                            IconButton(onClick={ actionsMenu=true },modifier=Modifier.semantics { contentDescription="Table actions" }) { Text("⋮",style=MaterialTheme.typography.headlineSmall) }
                            DropdownMenu(expanded=actionsMenu,onDismissRequest={ actionsMenu=false }) {
                                DropdownMenuItem(text={ Text(if(editMode) "Switch to read-only" else "Enable editing") },enabled=!state.busy,onClick={ if(editMode) editMode=false else editingSetup=true; actionsMenu=false })
                                DropdownMenuItem(text={ Text("Defaults & starting-value restore") },onClick={ screen="Defaults"; actionsMenu=false })
                                DropdownMenuItem(text={ Text("Sort: ${listOf("Name ↑","Name ↓","Value ↑")[sort]}") },onClick={ sort=(sort+1)%3; actionsMenu=false })
                                DropdownMenuItem(text={ Text("Look up / Add") },enabled=!state.busy,onClick={ adding=true; actionsMenu=false })
                                DropdownMenuItem(text={ Text("Refresh") },enabled=!state.busy,onClick={ model.refresh(); actionsMenu=false })
                            }
                        }

                    } else IconButton(onClick={ model.refresh() },enabled=!state.busy,modifier=Modifier.semantics { contentDescription="Refresh" }) {
                        Icon(painterResource(R.drawable.ic_refresh),null)
                    }
                }
                if(screen!="Tables") Spacer(Modifier.height(12.dp))
                when(screen) {
                    "Tables" -> {
                        val eligible = if(table == Table.SYSTEM) state.system else state.secure
                        val read = state.tables[table]
                        val rows = read?.rows.orEmpty().filter { it.name.contains(query, true) || it.cell.value.orEmpty().contains(query, true) }.let { when(sort) { 1 -> it.sortedByDescending { r -> r.name }; 2 -> it.sortedWith(compareBy<Row> { r -> r.cell.value.orEmpty() }.thenBy { r -> r.name }); else -> it.sortedBy { r -> r.name } } }
                        key(table) {
                            val listState=tableLists.getValue(table)
                            val scope=rememberCoroutineScope()
                            val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex>1 } }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(Modifier.fillMaxSize().testTag("settings-list"), state=listState, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 76.dp)) {
                                stickyHeader(key="search") {
                                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).testTag("pinned-search").padding(top=12.dp,bottom=8.dp)) {
                                        CompactSearch(query) { query=it;scope.launch { listState.scrollToItem(0) } }
                                    }
                                }
                            item(key="access-summary") {
                                Text("${rows.size} settings · ${table.scope}",style=MaterialTheme.typography.labelMedium)
                                Text(if(eligible) "Editing permission available" else "Read only · editing needs setup",style=MaterialTheme.typography.bodySmall)
                            }
                            if(state.busy) item(key="loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                            state.message?.let { message -> item(key="message") { Panel("Status",message) { TextButton(onClick=model::clearMessage) { Text("Dismiss") } } } }
                            if(read?.error!=null) item(key="read-error") { Panel("Table access limited",read.error+"\nTry an exact key with Look up / Add. Unreadable does not mean absent.") }
                            if(rows.isEmpty()) item { Text(if(state.busy) "Loading…" else "No visible matches. Restricted keys may be omitted by Android.") }
                            items(rows, key = { "setting:${it.name}" }) { row -> Card(onClick = { model.inspect(table, row.name) { editor = Editor(table, row.name, it) } }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(row.name, style = MaterialTheme.typography.titleSmall); Selectable(row.cell.display(), true)
                                    Text(explanations[table to row.name]?.text ?: "Description unavailable", style = MaterialTheme.typography.bodySmall, maxLines=3, overflow=TextOverflow.Ellipsis)
                                    RiskLabel(riskFor(table,row.name))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if(!keyWriteSupported(table, row.name)) "Use Android Settings" else if(editMode && eligible) "Ready to review" else "Read only", style = MaterialTheme.typography.labelSmall); TextButton(onClick = { copy(context, row.cell.value ?: row.cell.display()) }) { Text("Copy value") } }
                                }
                            } }
                            }
                            if(showTop) SmallFloatingActionButton(
                                onClick={ scope.launch { listState.animateScrollToItem(0) } },
                                modifier=Modifier.align(Alignment.BottomEnd).padding(end=8.dp,bottom=12.dp).semantics { contentDescription="Go to top" },
                                containerColor=MaterialTheme.colorScheme.primaryContainer,
                                contentColor=MaterialTheme.colorScheme.onPrimaryContainer
                            ) { Icon(painterResource(R.drawable.ic_top),null,Modifier.size(24.dp)) }
                            }
                        }
                    }
                    "Defaults" -> DefaultsScreen(state, editMode, onInspect = { key, target -> model.inspect(key.table,key.name) { editor=Editor(key.table,key.name,it,target) } })
                    "History" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                        if(state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        state.message?.let { message -> item { OperationFeedback(message,model::clearMessage) } }
                        item { Panel("Change history", "Every edit keeps its original value. Verified changes matched an immediate read-back; pending or unverified changes need review. Undo protects newer values from being overwritten.") }
                        state.storageError?.let { item { Panel("Storage error", it) } }
                        if(state.history.isEmpty()) item { Panel("Your history starts here", "Changes you make will appear here, with their original values and an option to review undo.") }
                        items(state.history, key = { it.id }) { change -> Panel("${change.table.label} · ${change.status}", "${date(change.time)}\n${change.name}\n${change.before.display()} → ${change.target.display()}\n${change.message}") {
                            TextButton(onClick = { copy(context, "${change.name}\n${change.before.display()} → ${change.target.display()}\n${change.status}: ${change.message}") }) { Text("Copy record") }
                            val undone = state.history.any { it.undoOf == change.id && it.status == "VERIFIED" }
                            Button(onClick = { undo = change }, enabled = editMode && state.trial == null && state.baseline != null && riskFor(change.table,change.name) != Risk.BLOCKED && change.status == "VERIFIED" && !undone && !state.busy && (if(change.table == Table.SYSTEM) state.system else state.secure)) { Text(if(undone) "Undone" else "Review undo") }
                        } }
                    }
                    "Snapshots" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                        if(state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        state.message?.let { message -> item { OperationFeedback(message,model::clearMessage) } }
                        item { Panel("Save a point in time", "Keep a JSON snapshot of the settings visible to this app, then compare it with another snapshot. Captures cover all three tables for this Android user, are taken sequentially and may contain sensitive values. They are a partial view of Android configuration; comparison does not restore settings.") {
                            Button(onClick = { export.launch("android-powertoys-${System.currentTimeMillis()}.json") }, enabled = !state.busy) { Text("Export snapshot…") }
                            Text("Choose local storage to keep the export on device. A cloud file provider may upload a file you save there.", style = MaterialTheme.typography.bodySmall)
                        } }
                        item { Panel("Two-file comparison", "A → B. Use snapshots from the same user, device and permission level. Different devices can have the same user number; check your file origins.") {
                            OutlinedButton(onClick = { importSlot = 1; importer.launch(arrayOf("application/json", "text/*")) }, enabled = !state.busy) { Text("Choose A${state.first?.let { " · ${date(it.capturedAt)}" }.orEmpty()}") }
                            OutlinedButton(onClick = { importSlot = 2; importer.launch(arrayOf("application/json", "text/*")) }, enabled = !state.busy) { Text("Choose B${state.second?.let { " · ${date(it.capturedAt)}" }.orEmpty()}") }
                            Button(onClick = model::compare, enabled = state.first != null && state.second != null && !state.busy) { Text("Compare snapshots") }
                        } }
                        state.diffs?.let { diffs -> if(diffs.isEmpty()) item { Text("No visible differences.") }; items(diffs) { d -> Panel("${d.kind} · ${d.table.label}", "${d.name}\nA: ${d.before.display()}\nB: ${d.after.display()}") } }
                    }
                    else -> AboutScreen(Modifier.weight(1f),state,darkChoice,onAppearance={ darkChoice=it },onSetup={ editingSetup=true },onRefresh=model::refresh,onLicenses={ licenses=true },onDismiss=model::clearMessage)
                }
            }
        }
        if(licenses) LicenseDialog(onClose={ licenses=false })
        if(editingSetup) EditingSetupDialog(state,table,onRefresh=model::refresh,onEnable={ selected -> table=selected; editMode=true; screen="Tables"; editingSetup=false },onClose={ editingSetup=false })
        if(adding) { var name by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { adding = false }, title = { Text("Look up / Add · ${table.label}") }, text = { Column { Text("Check an exact key first. An access failure is never treated as absence."); OutlinedTextField(name, { name = it }, label = { Text("Setting name") }, singleLine = true) } }, confirmButton = { TextButton(enabled = name.isNotBlank() && !state.busy, onClick = { adding = false; model.inspect(table, name) { editor = Editor(table, name, it) } }) { Text("Look up") } }, dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }) }
        editor?.let { item -> EditDialog(item, state.baseline, editMode && keyWriteSupported(item.table, item.name) && riskFor(item.table,item.name) != Risk.BLOCKED && state.trial == null && state.baseline != null && state.storageError == null && (if(item.table == Table.SYSTEM) state.system else state.secure) && !state.busy, onNotifications = { if(Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) notificationPermission.launch("android.permission.POST_NOTIFICATIONS") else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) }, onClose = { editor = null }, onChange = { target -> model.change(item.table, item.name, item.original, target); editor = null }) }
        state.trial?.let { trial -> TrialDialog(trial, onKeep={ model.trialAction("keep") }, onRevert={ model.trialAction("revert") }, onAbandon={ model.trialAction("abandon") }) }
        undo?.let { change -> AlertDialog(onDismissRequest = { undo = null }, title = { Text("Undo ${change.name}?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Selectable("${change.table.label} · ${change.table.scope}\nRestore: ${change.before.display()}\nOnly if current value still equals: ${change.target.display()}"); Text(riskExplanation(change.table,change.name)); if(riskFor(change.table,change.name)==Risk.DISPLAY) Text(trialExplanation) } }, confirmButton = { TextButton(onClick = { undo = null; model.undo(change) }) { Text("Restore original") } }, dismissButton = { TextButton(onClick = { undo = null }) { Text("Cancel") } }) }
    }
}
@Composable private fun Command(command: String) { Selectable(command, true); val context = LocalContext.current; TextButton(onClick = { copy(context, command) }) { Text("Copy command") } }
@Composable private fun SourceLink(url: String) { val context = LocalContext.current; TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }) { Text("Open source documentation ↗") } }
private const val trialExplanation = "A foreground service will attempt to restore the original after 15 seconds unless you confirm you can read and use the screen. This is best-effort: force-stop, reboot, system failure or lost permission can prevent rollback. An interrupted trial is recovered when the app next starts. This is not an OS display-mode failsafe."
@Composable private fun EditDialog(item: Editor, baseline: Baseline?, enabled: Boolean, onNotifications: () -> Unit, onClose: () -> Unit, onChange: (Cell) -> Unit) {
    var value by remember(item) { mutableStateOf(item.original.value.orEmpty()) }
    var preset by remember(item) { mutableStateOf(item.requested) }
    var deleting by remember(item) { mutableStateOf(false) }
    var review by remember(item) { mutableStateOf(item.requested != null) }
    var acknowledged by remember(item) { mutableStateOf(false) }
    val info=explanations[item.table to item.name]
    val risk=riskFor(item.table,item.name)
    val start=baselineValue(baseline,item.table,item.name)
    val target=preset ?: if(deleting) Cell.Absent else Cell(true,value)
    val validation=runCatching { validateEdit(item.table,item.name,target) }.exceptionOrNull()?.message
    val context=LocalContext.current
    val notifications=context.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()
    val trial=risk==Risk.DISPLAY
    val needsAcknowledgement=risk in setOf(Risk.HIGH,Risk.UNKNOWN)
    AlertDialog(onDismissRequest=onClose, title={ Text(if(review) "Review ${if(trial) "15-second trial" else "change"}" else item.name) }, text={
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Selectable("${item.table.label} · ${item.table.scope}\n${item.name}\nCurrent: ${item.original.display()}",true) }
            item { Text(info?.text ?: "Description unavailable"); if(info?.documentationOnly==true) Text("Android 17 source documentation · edit effects unassessed",style=MaterialTheme.typography.labelSmall); Text(risk.label,color=MaterialTheme.colorScheme.error); Text(riskExplanation(item.table,item.name)) }
            item { ReferenceValues(start,info?.aospDefault,baseline?.capturedAt,current=item.original) }
            if(baseline != null && baseline.build != Build.FINGERPRINT) item { Text("OS build changed since baseline capture. Starting values are historical and may no longer suit this build.",color=MaterialTheme.colorScheme.error) }
            if(trial) item { Text(trialExplanation); Text(displayBounds(item.name)); if(!notifications) { Text("Enable notifications before starting a trial. No display write is made if the recovery service cannot start."); TextButton(onClick=onNotifications) { Text("Enable trial notifications") } }; TextButton(onClick=onNotifications) { Text("Recovery notification settings") } }
            if(review) {
                item { Selectable("Requested: ${target.display()}"); Text(if(!target.exists) "Remove the row. Consumers may use fallback behavior; deletion is not a factory reset." else "Store this exact value. An empty string, null and an absent row have different meanings.") }
                if(needsAcknowledgement) item { Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(acknowledged,{ acknowledged=it }); Text("I understand these potential effects and want this change.",Modifier.weight(1f)) } }
                item { Text("The original state is recorded before writing. Success requires matching read-back.") }
            } else {
                item { OutlinedTextField(value,{ value=it; preset=null },Modifier.fillMaxWidth(),label={ Text("New value (empty is allowed)") },enabled=enabled && !deleting,minLines=2) }
                item { Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(deleting,{ deleting=it; preset=null },enabled=enabled && item.original.exists && item.table!=Table.SYSTEM); Text("Delete row") } }
                start?.let { original -> item { OutlinedButton(onClick={ preset=original; review=true },enabled=enabled && original!=item.original) { Text("Restore starting value…") } } }
                info?.aospDefault?.let { reference -> item { TextButton(onClick={ preset=reference; review=true },enabled=enabled && reference!=item.original) { Text("Review AOSP reference value…") } } }
                item { TextButton(onClick={ copy(context,item.name) }) { Text("Copy name") }; TextButton(onClick={ copy(context,item.original.value ?: item.original.display()) }) { Text("Copy current value") } }
                info?.let { item { SourceLink(it.source) } }
                if(info?.aospDefault != null) item { SourceLink(defaultsSource); SourceLink(if(item.name=="font_scale") providerSource else initializationSource) }
            }
            if(!enabled) item { Text(if(risk==Risk.BLOCKED) "Editing blocked: use Android's dedicated Settings screen. A countdown cannot reliably protect input or recovery access." else "Editing locked. Enable editing in Tables and grant the required access. A saved starting baseline and no unresolved trial are also required.") }
            if(validation!=null && enabled) item { Text(validation,color=MaterialTheme.colorScheme.error) }
        }
    },confirmButton={ TextButton(enabled=enabled && validation==null && target!=item.original && (!trial || notifications) && (!review || !needsAcknowledgement || acknowledged),onClick={ if(review) onChange(target) else review=true }) { Text(if(review) if(trial) "Start 15-second trial" else "Apply change" else "Review change") } },dismissButton={ TextButton(onClick={ if(review) { review=false; acknowledged=false } else onClose() }) { Text(if(review) "Back" else "Close") } })
}
@Composable private fun ReferenceValues(start: Cell?, aosp: Cell?, capturedAt: Long?, explain: Boolean = true, current: Cell? = null) {
    Selectable("Starting device value: ${start?.display() ?: "Unavailable — not captured for this key"}\n${capturedAt?.let { "Captured ${date(it)}\n" }.orEmpty()}AOSP Android 17 reference default: ${aosp?.display() ?: "Unavailable — no verified universal value"}\n${referenceComparison(start,aosp)}")
    if(current!=null && start!=null) Text(if(current==start) "Current value matches starting value" else "Current value differs from starting value",fontWeight=FontWeight.SemiBold)
    if(current!=null && aosp!=null) Text(referenceComparison(current,aosp).replace("Starting value","Current value"),style=MaterialTheme.typography.bodySmall)
    if(explain) Text("Starting values are a local first-capture baseline, not factory defaults. AOSP references are unoverlaid source values; Pixel, carriers, setup and updates may choose different values.",style=MaterialTheme.typography.bodySmall)
}
@Composable private fun DefaultsScreen(state: AppState, editMode: Boolean, onInspect: (SettingKey, Cell?) -> Unit) {
    var search by remember { mutableStateOf("") }
    val baseline=state.baseline
    var changedOnly by remember { mutableStateOf(false) }
    val keys=(baseline?.values?.keys.orEmpty()+explanations.keys.map { SettingKey(it.first,it.second) }+state.tables.flatMap { (table,read) -> read.rows.map { SettingKey(table,it.name) } }).distinct().filter { key -> key.name.contains(search,true) && (!changedOnly || (baseline?.values?.get(key)?.let { start -> state.tables[key.table]?.rows?.find { it.name==key.name }?.cell != start } == true)) }.sortedWith(compareBy<SettingKey> { it.table.ordinal }.thenBy { it.name })
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)) {
        item { Panel("Defaults & restore", "Starting device values were captured when this safety version first opened. They survive app updates and refreshes; there is no automatic recapture. Earlier changes cannot be reconstructed. AOSP Android 17 reference defaults are unoverlaid source values, not Pixel factory values; OEMs, carriers, setup and updates may differ. Unknown keys and omitted rows have no invented default. Restore one setting at a time with review; no bulk reset is offered.") }
        item { OutlinedTextField(search,{ search=it },Modifier.fillMaxWidth(),label={ Text("Find a default or starting value") },singleLine=true) }
        item { Row(verticalAlignment=Alignment.CenterVertically) { Switch(changedOnly,{ changedOnly=it }); Text("Changed or not currently visible") } }
        if(baseline==null) item { Text("Starting baseline unavailable. Editing is disabled until a baseline can be saved.") }
        if(!editMode) item { Text("Read-only. Enable editing in Tables to restore a value.") }
        items(keys,key={ "${it.table}:${it.name}" }) { key ->
            val start=baseline?.values?.get(key)
            val info=explanations[key.table to key.name]
            val visible=state.tables[key.table]?.rows?.find { it.name==key.name }?.cell
            Panel("${key.table.label} · ${key.name}",info?.text ?: "Description unavailable") {
                Text("Current visible value: ${visible?.display() ?: "Not visible — inspect to check exact access/existence"}")
                ReferenceValues(start,info?.aospDefault,baseline?.capturedAt,explain=false,current=visible)
                Text(riskFor(key.table,key.name).label,color=MaterialTheme.colorScheme.error)
                OutlinedButton(onClick={ onInspect(key,null) },enabled=!state.busy) { Text("Inspect & compare") }
                if(start!=null) Button(onClick={ onInspect(key,start) },enabled=editMode && state.trial==null && state.storageError==null && !state.busy && riskFor(key.table,key.name)!=Risk.BLOCKED && keyWriteSupported(key.table,key.name) && (if(key.table==Table.SYSTEM) state.system else state.secure) && (visible==null || visible!=start)) { Text("Review starting-value restore…") }
            }
        }
    }
}
@Composable private fun TrialDialog(trial: Trial, onKeep: () -> Unit, onRevert: () -> Unit, onAbandon: () -> Unit) {
    var abandon by remember { mutableStateOf(false) }
    var remaining by remember(trial.id) { mutableLongStateOf(0) }
    LaunchedEffect(trial.id) { while(true) { remaining=((trial.deadline-SystemClock.elapsedRealtime()+999)/1000).coerceAtLeast(0); delay(100) } }
    AlertDialog(onDismissRequest={},title={ Text(if(trial.problem==null) "Can you read and use the screen?" else "Rollback needs attention") },text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Selectable("${trial.name}\nTrial value: ${trial.target.display()}\nOriginal: ${trial.before.display()}"); Text(trial.problem ?: "Restore attempt in $remaining seconds. Keep only if the display and controls are usable."); if(trial.problem!=null) TextButton(onClick={ abandon=true }) { Text("Manage recovery manually…") } } },confirmButton={ TextButton(onClick=onKeep,enabled=trial.problem==null && remaining>0) { Text("I can use it — Keep") } },dismissButton={ TextButton(onClick=onRevert) { Text("Restore original now") } })
    if(abandon) AlertDialog(onDismissRequest={ abandon=false },title={ Text("Stop automatic recovery?") },text={ Text("The original has NOT been restored. Its value will be retained in history as RECOVERY_REQUIRED. Use Android settings or authorized ADB to recover manually. This action makes no setting write.") },confirmButton={ TextButton(onClick=onAbandon) { Text("I will recover manually") } },dismissButton={ TextButton(onClick={ abandon=false }) { Text("Back") } })

}

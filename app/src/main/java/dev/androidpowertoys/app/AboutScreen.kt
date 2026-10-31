package dev.androidpowertoys.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable private fun AboutDetails(title: String, summary: String, content: @Composable ColumnScope.()->Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(role=Role.Button) { expanded=!expanded }.semantics { stateDescription=if(expanded) "Expanded" else "Collapsed" }.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.titleMedium);Text(summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(if(expanded) "−" else "+",style=MaterialTheme.typography.titleLarge)
            }
            if(expanded) Column(Modifier.padding(start=18.dp,end=18.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
        }
    }
}
@Composable internal fun AboutScreen(modifier: Modifier, state: AppState, appearance: Boolean?, onAppearance: (Boolean?)->Unit, onSetup: ()->Unit, onRefresh: ()->Unit, onLicenses: ()->Unit, onDismiss: ()->Unit) {
    val context=LocalContext.current
    LazyColumn(modifier.testTag("about-list"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)) {
        if(state.busy) item(key="loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.message?.let { message -> item(key="feedback") { OperationFeedback(message,onDismiss) } }
        state.storageError?.let { error -> item(key="storage-error") { Text(error,color=MaterialTheme.colorScheme.error) } }
        item(key="app") { MooseAiAbout() }
        item(key="appearance") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Appearance",style=MaterialTheme.typography.titleMedium)
            Text("Match your device or choose a theme.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("System" to null,"Light" to false,"Dark" to true).forEach { (label,value) ->
                FilterChip(selected=appearance==value,onClick={ onAppearance(value) },label={ Text(label) })
            } }
        } } }
        item(key="editing") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Editing access",style=MaterialTheme.typography.titleMedium)
            Text("System: ${if(state.system) "permission available" else "needs permission"}\nSecure & Global: ${if(state.secure) "permission available" else "needs setup"}")
            Text("The app starts read-only. Review each change before applying it; Android can still restrict individual settings.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick=onSetup) { Text("Set up editing") }
        } } }
        item(key="privacy") { AboutDetails("Privacy & storage","Your history stays on this phone. No ads, accounts or analytics.") {
            Text("History, starting values and pending recovery records are kept in private storage, excluded from automatic backup and device transfer. Networking is used only for the wireless setup you choose to start on this phone. There are no cloud uploads.")
            Text("Exported files can contain sensitive settings. A cloud storage provider can upload a file you explicitly save there. Uninstalling removes local history and starting values.")
        } }
        item(key="safety") { AboutDetails("Safer changes","Saved originals, verified writes and undo.") {
            Text("Original values are recorded before an edit. A change is verified only after the value reads back correctly. Undo checks for conflicting newer values. Some settings stay blocked to protect input and recovery.")
            Text("Six display/usability settings use a 15-second confirmation with a short foreground recovery service. Force-stop, reboot, lost permission or system failure can prevent rollback; pending originals are retained for recovery. Notifications must be allowed before a screen trial.")
            Text("A documented meaning is not a guarantee of a safe edit. Unknown effects are labelled and require review. AOSP reference values are not Pixel factory defaults.")
        } }
        item(key="permissions") { AboutDetails("Advanced permission details","Current Android user, platform limits and manual setup.") {
            Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nThis app uses its current Android user only. System and Secure are generally per user; Global is device-wide. These tables do not contain all Android configuration.")
            Text("Android may hide or redact values. Supported System edits use Modify system settings access; Secure and Global use WRITE_SECURE_SETTINGS. A permission does not guarantee a successful write. Android 17 still restricts private System keys, arbitrary additions and System deletions.")
            OutlinedButton(onClick={ runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${context.packageName}"))) } }) { Text("Open Modify system settings") }
            Text("The editing guide supports explicit phone-only wireless setup. Alternatively, these one-time computer commands grant or revoke this app's development permission for the foreground user. Copying them does not execute them.")
            for(action in listOf("grant","revoke")) {
                val command="adb shell pm $action --user current ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                SelectionContainer { Text(command,style=MaterialTheme.typography.bodySmall) }
                TextButton(onClick={ context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Android PowerToys",command)) }) { Text("Copy $action command") }
            }
            Text("Use Android's special app access screen to revoke Modify system settings separately. Returning to read-only mode does not revoke permissions or undo values.")
            OutlinedButton(onClick=onRefresh,enabled=!state.busy) { Text("Check access again") }
        } }
        item(key="licenses") { TextButton(onClick=onLicenses) { Text("Open-source licenses") } }
    }
}

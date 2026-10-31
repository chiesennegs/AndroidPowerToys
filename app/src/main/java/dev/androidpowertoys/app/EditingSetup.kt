package dev.androidpowertoys.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Opening this guide never executes a grant. The separate wireless screen requires explicit confirmation. */
@Composable fun EditingSetupDialog(state: AppState, initialTable: Table, onRefresh: () -> Unit, onEnable: (Table) -> Unit, onClose: () -> Unit) {
    val context=LocalContext.current
    var chosen by rememberSaveable { mutableStateOf(initialTable.name) }
    val table=Table.valueOf(chosen)
    var error by remember { mutableStateOf<String?>(null) }
    var wireless by rememberSaveable { mutableStateOf(false) }
    var showRevoke by remember { mutableStateOf(false) }
    val eligible=if(table==Table.SYSTEM) state.system else state.secure
    val ready=eligible && state.baseline!=null && state.storageError==null && state.trial==null && !state.busy
    val grant="adb shell pm grant --user current ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    val revoke="adb shell pm revoke --user current ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    fun openSystemAccess() {
        try { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${context.packageName}"))) }
        catch(_: Exception) { error="Open Android Settings → Apps → Special app access → Modify system settings → Android PowerToys. This device could not open the direct link." }
    }
    if(wireless) { WirelessSetup(onClose={ wireless=false },onAccessChanged=onRefresh); return }
    AlertDialog(onDismissRequest=onClose,title={ Text("Set up editing") },text={
        LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {
                Text("1 · Choose the table",style=MaterialTheme.typography.titleSmall)
                Text("Editing starts only after you enable it here. Opening this guide or copying a command does not grant access or change a setting.")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Table.entries.forEachIndexed { index,t -> SegmentedButton(selected=table==t,onClick={ chosen=t.name; error=null; showRevoke=false },shape=SegmentedButtonDefaults.itemShape(index,3)) { Text(t.label) } }
                }
            }
            item {
                Text("${table.label} · ${if(table==Table.GLOBAL) "Device-wide; changes can affect other users" else "This Android user only"}",style=MaterialTheme.typography.labelLarge)
                Text(if(eligible) "Write permission detected for ${table.label}" else "Write permission not granted for ${table.label}",color=if(eligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            if(table==Table.SYSTEM) {
                item {
                    Text("2 · Allow System changes",style=MaterialTheme.typography.titleSmall)
                    Text("Open Android's Modify system settings screen, allow Android PowerToys, then return here. This authorizes supported public System values; it does not grant Secure or Global access.")
                    OutlinedButton(onClick={ openSystemAccess() }) { Text("Open Modify system settings") }
                    Text("An existing ADB secure-settings grant can also authorize public System writes. Android 17 still restricts private keys, arbitrary additions and System deletions.")
                }
            } else {
                item {
                    Text("2 · Choose a setup method",style=MaterialTheme.typography.titleSmall)
                    Button(onClick={ wireless=true }) { Text("Set up wirelessly on this phone") }
                    Text("Or use a computer:",style=MaterialTheme.typography.labelLarge)
                    Text("On a computer with Android Platform Tools, connect your Pixel using USB or paired wireless debugging. Accept the phone's debugging authorization prompt. Keep this app open in the intended foreground Android user, then run this exact command on the computer:")
                    SetupCommand(grant,"Copy grant command")
                    Text("This grants broad WRITE_SECURE_SETTINGS access to this app, including Secure and Global writes. It persists until revoked or the app is uninstalled. Device policy may refuse it. Computer commands are copied only. The optional wireless flow runs a fixed grant command only after your separate confirmation.")
                }
            }
            item {
                Text("3 · Check access and enable",style=MaterialTheme.typography.titleSmall)
                Text("Access is checked again when you return to the app. You can also check it below. Once permission is detected, enable editing for this session using the button at the bottom.")
                OutlinedButton(onClick=onRefresh,enabled=!state.busy) { Text(if(state.busy) "Checking access…" else "Check access again") }
                state.storageError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                if(state.baseline==null) Text("A saved starting-device baseline is required before editing. Wait for loading to finish, or check access again to retry capture.")
                if(state.trial!=null) Text("Resolve the pending display trial before enabling more edits.")
                error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            }
            item {
                Text("4 · Review each change",style=MaterialTheme.typography.titleSmall)
                Text("Tap a setting, enter the new value, then choose Review change → Apply change. Some settings remain blocked or Android may reject them even with permission. Display-related edits require recovery notifications and a 15-second usability confirmation. Enabling editing itself changes no settings.")
            }
            item {
                TextButton(onClick={ showRevoke=!showRevoke }) { Text(if(showRevoke) "Hide revocation instructions" else "How to turn access off") }
                if(showRevoke) {
                    Text("Use ⋮ → Switch to read-only to stop new edits in this session. That does not revoke Android permissions.")
                    Text("Turn off Modify system settings in Android's special app access screen. To revoke the ADB grant for the intended foreground user, run:")
                    SetupCommand(revoke,"Copy revoke command")
                }
            }
        }
    },confirmButton={ TextButton(onClick={ onEnable(table) },enabled=ready) { Text("Enable ${table.label} editing") } },dismissButton={ TextButton(onClick=onClose) { Text("Stay read-only") } })
}
@Composable private fun SetupCommand(command: String, label: String) {
    val context=LocalContext.current
    SelectionContainer { Text(command,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodySmall) }
    TextButton(onClick={ context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Android PowerToys ADB command",command)) }) { Text(label) }
}

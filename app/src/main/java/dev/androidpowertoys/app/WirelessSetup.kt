package dev.androidpowertoys.app

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable fun WirelessSetup(onClose: ()->Unit,onAccessChanged: ()->Unit,model: WirelessModel=viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var host by rememberSaveable { mutableStateOf("127.0.0.1") }
    var pairingPort by rememberSaveable { mutableStateOf("") }
    var connectionPort by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var networkAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT<37 || context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")==android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { networkAllowed=it; if(!it) error="Local-network access was not allowed. Enable it in Android Settings → Apps → Android PowerToys → Permissions → Nearby devices, then reopen this setup." }
    DisposableEffect(Unit) { onDispose { model.clearCheck() } }
    AlertDialog(onDismissRequest={ if(!state.busy) onClose() },title={ Text("Wireless setup · this phone") },text={
        LazyColumn(Modifier.testTag("wireless-setup-list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("This uses Android's wireless ADB authorization, without a computer. Pairing gives this app a debugging identity, which is broader than settings access. Nothing connects until you tap Pair or Check connection. Granting write access is a separate confirmation.")
                Text("1 · Prepare Android",style=MaterialTheme.typography.titleSmall)
                Text("Enable Developer options → Wireless debugging on this Wi-Fi network. Use split screen with Settings and Android PowerToys so the pairing-code dialog stays open. Turning debugging on may require Android's own network approval.")
                if(Build.VERSION.SDK_INT>=32) OutlinedButton(onClick={
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT))
                    } catch(_: Exception) { error="Could not open split screen. Use the recent-apps steps below, then open Settings → System → Developer options." }
                },enabled=!state.busy) { Text("Open Settings in split screen") }
                Text("In the Settings half, open Wireless debugging → Pair device with pairing code. Keep that dialog open while entering its port and code here.")
                Text("If Settings opens full screen: swipe up from the bottom and hold to show recent apps. Tap the Android PowerToys icon above its preview → Split screen, then choose Settings. With three-button navigation, use the square Overview button instead. Drag the divider to give each app room.")
                OutlinedButton(onClick={ try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) } catch(_: Exception) { error="Open Android Settings → System → Developer options → Wireless debugging manually." } },enabled=!state.busy) { Text("Open Developer options") }
                Text("Network access is used only for this phone's numeric addresses. No discovery scan, internet server, analytics or cloud upload is used.")
                if(Build.VERSION.SDK_INT>=37) OutlinedButton(onClick={ permission.launch("android.permission.ACCESS_LOCAL_NETWORK") },enabled=!state.busy) { Text(if(networkAllowed) "Local-network permission allowed" else "Allow local-network access") }
                if(Build.VERSION.SDK_INT<30) Text("Phone-only pairing is unavailable before Android 11.")
            }
            item {
                OutlinedTextField(host,{ host=it; model.clearCheck() },Modifier.fillMaxWidth(),label={ Text("This phone's IP address") },supportingText={ Text("127.0.0.1 targets this phone. Only loopback or an address assigned to this phone is accepted.") },singleLine=true,enabled=!state.busy)
            }
            item {
                Text("2 · Pair",style=MaterialTheme.typography.titleSmall)
                Text("In Android's Wireless debugging screen, tap Pair device with pairing code. Enter that dialog's pairing port and six-digit code. Keep the dialog open; the code expires. The code is never saved or logged.")
                OutlinedTextField(pairingPort,{ pairingPort=it },Modifier.fillMaxWidth(),label={ Text("Pairing port") },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),enabled=!state.busy)
                OutlinedTextField(code,{ code=it },Modifier.fillMaxWidth(),label={ Text("Six-digit pairing code") },singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),enabled=!state.busy)
                Button(onClick={ val password=code; code=""; model.pair(host,pairingPort,password) },enabled=!state.busy && networkAllowed && Build.VERSION.SDK_INT>=30 && code.matches(Regex("[0-9]{6}")) && runCatching { WirelessPolicy.port(pairingPort) }.isSuccess) { Text("Pair with this phone") }
            }
            item {
                Text("3 · Check the connection",style=MaterialTheme.typography.titleSmall)
                Text("After pairing succeeds, close Android's pairing-code dialog. Use the separate port next to IP address & Port on the main Wireless debugging screen. These two ports are usually different and may change when debugging restarts.")
                OutlinedTextField(connectionPort,{ connectionPort=it; model.clearCheck() },Modifier.fillMaxWidth(),label={ Text("Connection port (not pairing port)") },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),enabled=!state.busy)
                OutlinedButton(onClick={ model.checkConnection(host,connectionPort) },enabled=!state.busy && networkAllowed && Build.VERSION.SDK_INT>=30 && runCatching { WirelessPolicy.port(connectionPort) }.isSuccess) { Text("Check connection only") }
            }
            item { Text(state.message); if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth()); error?.let { Text(it,color=MaterialTheme.colorScheme.error) } }
            item {
                Text("4 · Choose an explicit permission action",style=MaterialTheme.typography.titleSmall)
                Text("The app verifies the shell identity and that its installed UID belongs to the current Android user before running a fixed command. It then checks the actual local permission before reporting success. It never sends setting-value edits through this connection.")
                Button(onClick={ confirmation="grant" },enabled=state.checked && !state.busy) { Text("Review wireless grant…") }
                OutlinedButton(onClick={ confirmation="revoke" },enabled=state.checked && !state.busy) { Text("Review wireless revoke…") }
                Text("A successful grant persists after debugging is turned off. Disable Wireless debugging after setup. An OS policy can reject the grant; no success is assumed from shell output alone.")
            }
            item {
                Text("Remove pairing trust",style=MaterialTheme.typography.titleSmall)
                Text("Forget Android PowerToys under Wireless debugging → Paired devices, then delete the local identity below. This prevents reconnecting with that key; it does not revoke a previously granted settings permission.")
                TextButton(onClick={ confirmation="forget" },enabled=!state.busy) { Text("Delete local pairing identity…") }
                Text("RSA credentials are encrypted with an Android Keystore key in no-backup storage. All connections are closed after each action; there is no background reconnect. Transport uses TLS and the explicitly supplied Android pairing code. The first own-device command connection pins its server key after read-only identity checks (trust on first use); permission actions require that pin. Android's pairing certificate is separate. The third-party protocol libraries have not had an independent security audit here.")
            }
        }
    },confirmButton={ TextButton(onClick={ if(!state.busy) { onAccessChanged(); onClose() } },enabled=!state.busy) { Text("Back to editing setup") } })
    confirmation?.let { action ->
        AlertDialog(onDismissRequest={ confirmation=null },title={ Text(when(action) { "grant"->"Grant settings write access?"; "revoke"->"Revoke settings write access?"; else->"Delete the local pairing identity?" }) },text={ Text(when(action) {
            "grant"->"Run the fixed pm grant command for dev.androidpowertoys.app in the verified current Android user. This grants broad WRITE_SECURE_SETTINGS access to Secure/Global and eligible public System values. It does not change any setting values or enable editing mode. The app will verify the permission afterwards."
            "revoke"->"Run the fixed pm revoke command for this app in the verified current Android user. This removes WRITE_SECURE_SETTINGS access but does not restore previous setting values."
            else->"Delete this app's encrypted ADB identity. You will need to pair again. Also forget the corresponding paired device in Android's Wireless debugging screen. Existing settings permissions are unaffected."
        }) },confirmButton={ TextButton(onClick={ confirmation=null; if(action=="forget") model.forget() else model.changePermission(action=="revoke",onAccessChanged) }) { Text(if(action=="grant") "Grant write access" else if(action=="revoke") "Revoke write access" else "Delete identity") } },dismissButton={ TextButton(onClick={ confirmation=null }) { Text("Cancel") } })
    }
}

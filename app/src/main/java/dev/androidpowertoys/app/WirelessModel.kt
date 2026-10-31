package dev.androidpowertoys.app

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

private class PhoneAdb(identity: Pair<java.security.PrivateKey,java.security.cert.Certificate>): AbsAdbConnectionManager() {
    private val key=identity.first
    private val certificate=identity.second
    init { setApi(Build.VERSION.SDK_INT); setTimeout(12,TimeUnit.SECONDS); setThrowOnUnauthorised(true) }
    override fun getPrivateKey()=key
    override fun getCertificate()=certificate
    override fun getDeviceName()="Android PowerToys"
}
data class WirelessState(val busy: Boolean=false,val message: String="No debugging connection has been opened.",val checked: Boolean=false)
class WirelessModel(application: Application): AndroidViewModel(application) {
    private val mutable=MutableStateFlow(WirelessState())
    val state=mutable.asStateFlow()
    @Volatile private var stage="preparing wireless access"
    private var endpoint: Pair<String,Int>?=null
    private val identity=WirelessIdentity(application)
    private fun access() {
        check(Build.VERSION.SDK_INT>=30) { "Wireless pairing needs Android 11 or newer." }
        if(Build.VERSION.SDK_INT>=37) check(getApplication<Application>().checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")==PackageManager.PERMISSION_GRANTED) { "Allow local-network access before starting wireless setup." }
    }
    private fun task(block: () -> String) {
        if(mutable.value.busy) return
        stage="checking local-network access"
        mutable.value=mutable.value.copy(busy=true)
        viewModelScope.launch {
            try { val result=withContext(Dispatchers.IO) { access(); block() }; mutable.value=WirelessState(message=result,checked=endpoint!=null) }
            catch(e: Exception) { endpoint=null; mutable.value=WirelessState(message="Wireless setup failed while $stage: ${if(e is IllegalArgumentException || e is IllegalStateException) e.message else e.javaClass.simpleName}. ${if(stage=="pairing") "Keep Android’s pairing-code dialog open and check its current port/code." else "After pairing, close the code dialog and use the separate connection port from Wireless debugging. If debugging restarted, check the port again."} No permission change is confirmed.") }
        }
    }
    fun pair(host: String,port: String,code: String) = task {
        val address=WirelessPolicy.ownAddress(host); val number=WirelessPolicy.port(port); val password=WirelessPolicy.code(code)
        endpoint=null
        stage="pairing"
        val keys=identity.loadOrCreate()
        io.github.muntashirakon.adb.PairingConnectionCtx(address,number,password.toByteArray(Charsets.UTF_8),keys.first,keys.second,"Android PowerToys").use { pairing -> pairing.start(); identity.savePairingProof(pairing.peerPublicKeyHash) }
        "Pairing completed. Pairing alone grants no settings permission. Close Android's pairing-code dialog and enter the separate connection port from the main Wireless debugging screen."
    }
    private fun shell(adb: PhoneAdb, command: String): String = adb.openStream("shell:$command").use { stream ->
        val output=java.io.ByteArrayOutputStream()
        stream.openInputStream().use { input -> val buffer=ByteArray(1024); while(true) { val count=input.read(buffer); if(count<0) break; check(output.size()+count<=16384) { "Unexpectedly large debugging response" }; output.write(buffer,0,count) } }
        output.toString("UTF-8").trim()
    }
    private fun verify(adb: PhoneAdb): Int {
        stage="reading the shell identity"
        check(shell(adb,"id -u")=="2000") { "Expected an unrooted Android shell" }
        stage="reading the current Android user"
        val user=shell(adb,"am get-current-user").toIntOrNull()?.takeIf { it>=0 } ?: error("Cannot identify current Android user")
        stage="checking this app’s installed UID"
        check(WirelessPolicy.appUid(shell(adb,"pm list packages -U --user $user dev.androidpowertoys.app"))==Process.myUid()) { "This app is not running in the foreground Android user" }
        return user
    }
    private fun connection(host: String,port: Int, bootstrap: Boolean=false, action: (PhoneAdb,Int)->String): String {
        check(identity.paired()) { "Pair with this phone first" }
        val pin=identity.pin()
        check(bootstrap || pin!=null) { "Perform a read-only connection check before granting access" }
        io.github.muntashirakon.adb.SslUtils.setExpectedServerKeyHash(pin)
        return PhoneAdb(identity.loadOrCreate()).use { adb ->
        stage="establishing the TLS connection"
        check(adb.connect(host,port)) { "TLS connection was not established" }
        val user=verify(adb)
        if(pin==null) identity.savePin(io.github.muntashirakon.adb.SslUtils.getObservedServerKeyHash() ?: error("Missing TLS server key"))
        action(adb,user)
        }
    }
    fun checkConnection(host: String,port: String) = task {
        endpoint=null
        val address=WirelessPolicy.ownAddress(host); val number=WirelessPolicy.port(port)
        val result=connection(address,number,bootstrap=true) { _,user -> "Connection and current-user app identity checked (user $user). No permission was granted. Review the separate grant confirmation when ready." }
        endpoint=address to number; result
    }
    fun changePermission(revoke: Boolean, done: () -> Unit) = task {
        val target=endpoint ?: error("Check the connection first")
        val result=connection(target.first,target.second) { adb,user ->
            stage=if(revoke) "revoking permission" else "granting permission"
            shell(adb,WirelessPolicy.grant(user,revoke))
            val granted=getApplication<Application>().checkSelfPermission(WirelessPolicy.permission)==PackageManager.PERMISSION_GRANTED
            check(granted != revoke) { "Android did not confirm the requested permission state" }
            if(revoke) "Write permission revocation verified. Existing setting values were not restored." else "WRITE_SECURE_SETTINGS grant verified using this app's permission check. No setting values were changed. You can turn Wireless debugging off, then enable editing."
        }
        endpoint=null
        viewModelScope.launch { done() }
        result
    }
    fun forget() {
        if(mutable.value.busy) return
        endpoint=null
        viewModelScope.launch { withContext(Dispatchers.IO) { identity.forget() }; mutable.value=WirelessState(message="Local pairing identity deleted. Also forget Android PowerToys in Wireless debugging → Paired devices. This does not revoke an existing WRITE_SECURE_SETTINGS grant.") }
    }
    fun clearCheck() { endpoint=null; if(!mutable.value.busy) mutable.value=mutable.value.copy(checked=false) }
}

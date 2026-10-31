package dev.androidpowertoys.app

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import kotlinx.coroutines.*

/** Starts from an explicit foreground edit; shortService is limited to a brief rollback trial. */
class RevertService: Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var timer: Job?=null
    private var wakeLock: PowerManager.WakeLock?=null
    @Volatile private var latestStartId=0
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId=startId
        try {
            val notifications=getSystemService(NotificationManager::class.java)
            notifications.createNotificationChannel(NotificationChannel("display-trial","Display change recovery",NotificationManager.IMPORTANCE_HIGH))
            check(intent?.action != "begin" || (notifications.areNotificationsEnabled() && notifications.getNotificationChannel("display-trial").importance != NotificationManager.IMPORTANCE_NONE)) { "Enable display-trial notifications before changing display settings." }
            val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val revert=PendingIntent.getService(this,1,Intent(this,RevertService::class.java).setAction("revert"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification=Notification.Builder(this,"display-trial").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Display trial · 15 seconds").setContentText("Return to confirm visibility, or restore the original now.").setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"Restore original",revert).build()).build()
            if(Build.VERSION.SDK_INT>=34) startForeground(15,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE) else startForeground(15,notification)
        } catch(e: Exception) {
            scope.launch { val repo=SafetyRepositories.get(this@RevertService); synchronized(repo) { repo.publish("Display trial did not start: ${e.message}. No new setting write was attempted.") }; stopSelf() }
            return START_NOT_STICKY
        }
        wakeLock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"AndroidPowerToys:DisplayRecovery").apply { acquire(30000) }
        scope.launch {
            val repo=SafetyRepositories.get(this@RevertService)
            try {
                synchronized(repo) {
                    when(intent?.action) {
                        "begin" -> {
                            repo.ensureBaseline()
                            val table=Table.valueOf(intent.getStringExtra("table")!!)
                            val name=intent.getStringExtra("name")!!
                            val before=cellFromJson(org.json.JSONObject(intent.getStringExtra("before")!!))
                            val target=cellFromJson(org.json.JSONObject(intent.getStringExtra("target")!!))
                            val result=repo.guard.begin(table,name,before,target,SystemClock.elapsedRealtime(),intent.getStringExtra("undoOf"))
                            repo.publish("Display trial ${result.status}: ${result.message}")
                        }
                        "keep" -> { repo.guard.keep(SystemClock.elapsedRealtime()); repo.publish("Display change kept after your confirmation.") }
                        else -> repo.publish(repo.guard.revert())
                    }
                }
            } catch(e: Exception) { synchronized(repo) { repo.publish("Display trial: ${e.message}") } }
            timer?.cancel()
            timer=scope.launch {
                while(isActive) {
                    val running=synchronized(repo) {
                        val result=repo.guard.tick(SystemClock.elapsedRealtime())
                        if(result!=null) repo.publish(result)
                        repo.trials.get()?.problem == null && repo.trials.get()!=null
                    }
                    if(!running) break
                    delay(100)
                }
                if(latestStartId==startId) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(startId) }
            }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() { wakeLock?.let { if(it.isHeld) it.release() }; timer?.cancel(); scope.cancel(); super.onDestroy() }
    override fun onTimeout(startId: Int) {
        scope.launch { val repo=SafetyRepositories.get(this@RevertService); synchronized(repo) { repo.publish(repo.guard.revert()) }; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    }
}

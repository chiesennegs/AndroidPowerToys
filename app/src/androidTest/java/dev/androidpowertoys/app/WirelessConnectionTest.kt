package dev.androidpowertoys.app

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in: requires existing phone pairing and a current connection port.
 * Sends only the model's fixed read-only identity probes; never grants or edits. */
@RunWith(AndroidJUnit4::class)
class WirelessConnectionTest {
    @Test fun pairedOwnPhoneCheckAndPinnedReconnect() {
        val args=InstrumentationRegistry.getArguments()
        val port=args.getString("wirelessConnectionPort")
        assumeTrue("Supply wirelessConnectionPort only for an authorized live connection check",port!=null)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as Application
        val originalPermission=app.checkSelfPermission(WirelessPolicy.permission)
        lateinit var model: WirelessModel
        instrumentation.runOnMainSync { model=WirelessModel(app) }
        repeat(3) {
            instrumentation.runOnMainSync { model.checkConnection("127.0.0.1",port!!) }
            val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20)
            while(model.state.value.busy && System.nanoTime()<deadline) Thread.sleep(50)
            assertFalse("Connection check timed out",model.state.value.busy)
            assertTrue(model.state.value.message,model.state.value.checked)
            assertEquals(originalPermission,app.checkSelfPermission(WirelessPolicy.permission))
        }
    }
}

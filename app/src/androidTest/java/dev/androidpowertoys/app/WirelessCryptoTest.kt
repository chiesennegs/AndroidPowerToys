package dev.androidpowertoys.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.Signature
import javax.net.ssl.SSLContext

@RunWith(AndroidJUnit4::class)
class WirelessCryptoTest {
    @Test fun isolatedEncryptedIdentitySurvivesReloadAndTlsProviderLoads() {
        val identity=WirelessIdentity(InstrumentationRegistry.getInstrumentation().targetContext,"wireless-test-${java.util.UUID.randomUUID()}")
        try {
            val first=identity.loadOrCreate(); val second=identity.loadOrCreate()
            assertFalse(identity.paired()); assertNull(identity.pin())
            identity.savePairingProof(ByteArray(32) { 2 })
            assertTrue(identity.paired()); assertNull(identity.pin())
            identity.savePin(ByteArray(32) { 3 })
            assertArrayEquals(ByteArray(32) { 3 },identity.pin())
            assertArrayEquals(first.first.encoded,second.first.encoded)
            assertArrayEquals(first.second.encoded,second.second.encoded)
            val challenge="local cryptography test".toByteArray()
            val signature=Signature.getInstance("SHA256withRSA").apply { initSign(first.first); update(challenge) }.sign()
            assertTrue(Signature.getInstance("SHA256withRSA").apply { initVerify(first.second); update(challenge) }.verify(signature))
            assertTrue(org.conscrypt.Conscrypt.isAvailable())
            assertTrue(SSLContext.getInstance("TLSv1.3",org.conscrypt.Conscrypt.newProvider()).protocol.contains("1.3"))
        } finally { identity.forget() }
    }
    @Test fun nativeSpakeMatchingAndWrongPassword() {
        val client="adb pair client\u0000".toByteArray()
        val server="adb pair server\u0000".toByteArray()
        for(wrong in listOf(false,true)) {
            val alice=io.github.muntashirakon.crypto.spake2.Spake2Context(io.github.muntashirakon.crypto.spake2.Spake2Role.Alice,client,server)
            val bob=io.github.muntashirakon.crypto.spake2.Spake2Context(io.github.muntashirakon.crypto.spake2.Spake2Role.Bob,server,client)
            try {
                val a=alice.generateMessage("fixed test password".toByteArray())
                val b=bob.generateMessage((if(wrong) "other test password" else "fixed test password").toByteArray())
                assertEquals(!wrong,alice.processMessage(b).contentEquals(bob.processMessage(a)))
            } finally { alice.destroy(); bob.destroy() }
        }
    }

}

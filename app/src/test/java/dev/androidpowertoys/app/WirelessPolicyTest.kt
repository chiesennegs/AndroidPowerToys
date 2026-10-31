package dev.androidpowertoys.app

import org.junit.Assert.*
import org.junit.Test

class WirelessPolicyTest {
    @Test fun boundedNumericPortsOnly() { assertEquals(1,WirelessPolicy.port("1")); assertEquals(65535,WirelessPolicy.port("65535")); listOf("0","65536","-1","123; id","abc","").forEach { assertThrows(IllegalArgumentException::class.java) { WirelessPolicy.port(it) } } }
    @Test fun pairingCodeIsExactlySixDigits() { assertEquals("012345",WirelessPolicy.code("012345")); listOf("12345","1234567","123 45","12345x").forEach { assertThrows(IllegalArgumentException::class.java) { WirelessPolicy.code(it) } } }
    @Test fun onlyOwnNumericAddressesAccepted() { assertEquals("127.0.0.1",WirelessPolicy.ownAddress("127.0.0.1")); listOf("example.com","localhost","0.0.0.0","224.0.0.1","192.0.2.123","127.0.0.1;id").forEach { assertThrows(IllegalArgumentException::class.java) { WirelessPolicy.ownAddress(it) } } }
    @Test fun fixedOwnPackageGrantAndRevokeOnly() { assertEquals("pm grant --user 0 dev.androidpowertoys.app android.permission.WRITE_SECURE_SETTINGS",WirelessPolicy.grant(0)); assertEquals("pm revoke --user 10 dev.androidpowertoys.app android.permission.WRITE_SECURE_SETTINGS",WirelessPolicy.grant(10,true)); assertThrows(IllegalArgumentException::class.java) { WirelessPolicy.grant(-1) } }
    @Test fun exactAppIdentityRequired() { assertEquals(12345,WirelessPolicy.appUid("package:dev.androidpowertoys.app uid:12345\n")); assertNull(WirelessPolicy.appUid("package:dev.androidpowertoys.app.fake uid:12345")); assertNull(WirelessPolicy.appUid("package:dev.androidpowertoys.app uid:12345;id")); assertNull(WirelessPolicy.appUid("package:dev.androidpowertoys.app uid:1\npackage:dev.androidpowertoys.app uid:2")) }
}

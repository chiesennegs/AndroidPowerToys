package dev.androidpowertoys.app

import java.net.InetAddress
import java.net.NetworkInterface

object WirelessPolicy {
    const val permission = "android.permission.WRITE_SECURE_SETTINGS"
    fun port(text: String): Int = text.toIntOrNull()?.takeIf { it in 1..65535 } ?: throw IllegalArgumentException("Enter a port from 1 to 65535.")
    fun code(text: String): String = text.also { require(it.matches(Regex("[0-9]{6}"))) { "Enter the six-digit pairing code." } }
    fun ownAddress(text: String): String {
        require(text.matches(Regex("[0-9.]+|[0-9a-fA-F:]+(%[a-zA-Z0-9]+)?"))) { "Enter a numeric address belonging to this phone; hostnames are not accepted." }
        val address=InetAddress.getByName(text)
        require(!address.isAnyLocalAddress && !address.isMulticastAddress && (address.isLoopbackAddress || NetworkInterface.getByInetAddress(address)!=null)) { "Only this phone's own addresses are allowed. Other devices are not supported." }
        return address.hostAddress!!
    }
    fun grant(user: Int, revoke: Boolean = false): String { require(user>=0); return "pm ${if(revoke) "revoke" else "grant"} --user $user dev.androidpowertoys.app $permission" }
    fun appUid(output: String): Int? = output.lineSequence().map { it.trim() }.mapNotNull { Regex("package:dev\\.androidpowertoys\\.app uid:([0-9]+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.singleOrNull()
}

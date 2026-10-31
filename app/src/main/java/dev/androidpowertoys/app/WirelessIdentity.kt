package dev.androidpowertoys.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Software RSA identity encrypted at rest by a non-exportable Android Keystore AES key. */
class WirelessIdentity(private val context: Context, private val identityName: String="wireless-identity") {
    private val file=AtomicFile(File(context.noBackupFilesDir,"$identityName.bin"))
    private val pinFile=AtomicFile(File(context.noBackupFilesDir,"$identityName-peer.bin"))
    private fun peer(): ByteArray? = if(pinFile.baseFile.exists()) pinFile.openRead().use { it.readBytes() }.also { require(it.size==32 || it.size==33) } else null
    fun paired(): Boolean = peer()!=null
    fun pin(): ByteArray? = peer()?.let { if(it.size==33 && it[0]==1.toByte()) it.copyOfRange(1,33) else null }
    private fun savePeer(bytes: ByteArray) { val out=pinFile.startWrite(); try { out.write(bytes); pinFile.finishWrite(out) } catch(e: Exception) { pinFile.failWrite(out); throw e } }
    fun savePairingProof(hash: ByteArray) { require(hash.size==32); savePeer(byteArrayOf(0)+hash) }
    fun savePin(pin: ByteArray) { require(pin.size==32); savePeer(byteArrayOf(1)+pin) }
    private val alias="AndroidPowerToys.$identityName.v1"
    private fun wrappingKey(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun loadOrCreate(): Pair<PrivateKey,java.security.cert.Certificate> {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        val data: ByteArray
        if(file.baseFile.exists()) {
            val encrypted=file.openRead().use { it.readBytes() }
            require(encrypted.size>12)
            cipher.init(Cipher.DECRYPT_MODE,wrappingKey(),GCMParameterSpec(128,encrypted.copyOfRange(0,12)))
            data=cipher.doFinal(encrypted.copyOfRange(12,encrypted.size))
        } else {
            val pair=KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val provider=BouncyCastleProvider()
            val name=X500Name("CN=Android PowerToys Wireless Setup")
            val certificate=JcaX509CertificateConverter().setProvider(provider).getCertificate(JcaX509v3CertificateBuilder(name,BigInteger(160,SecureRandom()).abs(),Date(System.currentTimeMillis()-86400000),Date(System.currentTimeMillis()+315360000000L),name,pair.public).build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.private)))
            data=java.io.ByteArrayOutputStream().also { bytes -> java.io.DataOutputStream(bytes).use { out -> out.writeInt(pair.private.encoded.size); out.write(pair.private.encoded); out.write(certificate.encoded) } }.toByteArray()
            cipher.init(Cipher.ENCRYPT_MODE,wrappingKey())
            val encrypted=cipher.iv+cipher.doFinal(data)
            val stream=file.startWrite()
            try { stream.write(encrypted); file.finishWrite(stream) } catch(e: Exception) { file.failWrite(stream); throw e }
        }
        try {
            java.io.DataInputStream(data.inputStream()).use { input ->
                val length=input.readInt(); require(length in 1..16384 && length<input.available())
                val encoded=ByteArray(length); input.readFully(encoded)
                val key=try { KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(encoded)) } finally { encoded.fill(0) }
                return key to CertificateFactory.getInstance("X.509").generateCertificate(input)
            }
        } finally { data.fill(0) }
    }
    fun forget() { file.delete(); pinFile.delete(); KeyStore.getInstance("AndroidKeyStore").apply { load(null); if(containsAlias(alias)) deleteEntry(alias) } }
}

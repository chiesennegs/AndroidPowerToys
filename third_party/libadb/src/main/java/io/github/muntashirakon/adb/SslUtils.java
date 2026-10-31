// SPDX-License-Identifier: GPL-3.0-or-later OR Apache-2.0

package io.github.muntashirakon.adb;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;

import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

public final class SslUtils {
    private static boolean customConscrypt = false;
    private static byte[] expectedServerKeyHash;
    private static volatile byte[] observedServerKeyHash;
    public static byte[] getObservedServerKeyHash() { return observedServerKeyHash == null ? null : observedServerKeyHash.clone(); }
    public static synchronized void setExpectedServerKeyHash(byte[] hash) { expectedServerKeyHash = hash == null ? null : hash.clone(); observedServerKeyHash = null; }

    public static boolean isCustomConscrypt() {
        return customConscrypt;
    }

    @SuppressLint("TrulyRandom") // The users are already instructed to fix this issue
    @NonNull
    public static SSLContext getSslContext(KeyPair keyPair) throws NoSuchAlgorithmException, KeyManagementException { return createContext(keyPair, false); }
    public static SSLContext getPairingSslContext(KeyPair keyPair) throws NoSuchAlgorithmException, KeyManagementException { return createContext(keyPair, true); }
    private static SSLContext createContext(KeyPair keyPair, boolean pairing) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3", org.conscrypt.Conscrypt.newProvider());
        customConscrypt = true;
        sslContext.init(new KeyManager[]{getKeyManager(keyPair)},
                new X509TrustManager[]{getServerTrustManager(pairing)},
                new SecureRandom());
        return sslContext;
    }

    @NonNull
    private static KeyManager getKeyManager(KeyPair keyPair) {
        return new X509ExtendedKeyManager() {
            private final String mAlias = "key";

            @Override
            public String[] getClientAliases(String keyType, Principal[] issuers) {
                return null;
            }

            @Override
            public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
                for (String keyType : keyTypes) {
                    if (keyType.equals("RSA")) return mAlias;
                }
                return null;
            }

            @Override
            public String[] getServerAliases(String keyType, Principal[] issuers) {
                return null;
            }

            @Override
            public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                return null;
            }

            @Override
            public X509Certificate[] getCertificateChain(String alias) {
                if (this.mAlias.equals(alias)) {
                    return new X509Certificate[]{(X509Certificate) keyPair.getCertificate()};
                }
                return null;
            }

            @Override
            public PrivateKey getPrivateKey(String alias) {
                if (this.mAlias.equals(alias)) {
                    return keyPair.getPrivateKey();
                }
                return null;
            }
        };
    }

    // Pairing authenticity is established by SPAKE2 with the explicit six-digit code.
    // First own-device read-only check captures a command-server pin (TOFU); permission commands require that saved pin. Android uses a separate, ephemeral pairing certificate.
    private static X509TrustManager getServerTrustManager(boolean pairing) {
        final byte[] pin = expectedServerKeyHash == null ? null : expectedServerKeyHash.clone();
        return new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException { throw new java.security.cert.CertificateException("Client authentication server mode is disabled"); }
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                if (chain == null || chain.length == 0) throw new java.security.cert.CertificateException("Missing peer certificate");
                if (pairing) return; // No commands: peer must subsequently prove possession of the pairing code.
                try {
                    byte[] actual = java.security.MessageDigest.getInstance("SHA-256").digest(chain[0].getPublicKey().getEncoded());
                    if (pin != null && !java.security.MessageDigest.isEqual(pin, actual)) throw new java.security.cert.CertificateException("Wireless server identity changed; pair again");
                    observedServerKeyHash = actual;
                } catch (java.security.NoSuchAlgorithmException e) { throw new java.security.cert.CertificateException(e); }
            }
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
    }
}

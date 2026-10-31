package io.github.muntashirakon.adb;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.ServerSocket;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.lang.reflect.Field;

/** Real stream buffering against a local socket; no Android daemon or permissions. */
public class StreamEofTest {
    private interface Check { void run(AdbStream stream) throws Exception; }
    private void withStream(Check check) throws Exception {
        java.security.KeyPair keys=KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Certificate cert=new Certificate("test") {
            public byte[] getEncoded() { return new byte[0]; }
            public void verify(PublicKey key) {}
            public void verify(PublicKey key,String provider) {}
            public PublicKey getPublicKey() { return keys.getPublic(); }
            public String toString() { return "test"; }
        };
        try(ServerSocket server=new ServerSocket(0);
            AdbConnection connection=AdbConnection.create("127.0.0.1",server.getLocalPort(),keys.getPrivate(),cert,37);
            java.net.Socket peer=server.accept()) {
            for(String name:new String[]{"mConnectAttempted","mConnectionEstablished"}) {
                Field field=AdbConnection.class.getDeclaredField(name); field.setAccessible(true); field.setBoolean(connection,true);
            }
            check.run(new AdbStream(connection,1));
        }
    }
    @Test(timeout=3000) public void remoteCloseAfterPayloadDrainsThenEnds() throws Exception {
        withStream(stream -> {
            stream.addPayload(new byte[]{1,2,3}); stream.notifyClose(true);
            byte[] result=new byte[2]; AdbInputStream input=stream.openInputStream();
            assertEquals(2,input.read(result)); assertArrayEquals(new byte[]{1,2},result);
            assertEquals(1,input.read(result)); assertEquals(3,result[0]);
            assertEquals(-1,input.read(result));
        });
    }
    @Test(timeout=3000) public void emptyRemoteCloseIsNormalEof() throws Exception {
        withStream(stream -> { stream.notifyClose(true); assertEquals(-1,stream.read(new byte[8],0,8)); });
    }
    @Test(timeout=3000) public void remoteClosePreservesPartiallyReadBuffer() throws Exception {
        withStream(stream -> {
            stream.addPayload(new byte[]{1,2,3});
            AdbInputStream input=stream.openInputStream(); assertEquals(1,input.read());
            stream.notifyClose(true);
            assertEquals(2,input.read()); assertEquals(3,input.read()); assertEquals(-1,input.read());
        });
    }
    @Test(timeout=3000) public void singleByteReadsAreUnsignedAndZeroLengthReadsAreZero() throws Exception {
        withStream(stream -> {
            stream.addPayload(new byte[]{(byte)255});
            AdbInputStream input=stream.openInputStream(); assertEquals(255,input.read());
            stream.notifyClose(true); assertEquals(0,input.read(new byte[0])); assertEquals(-1,input.read());
        });
    }
}

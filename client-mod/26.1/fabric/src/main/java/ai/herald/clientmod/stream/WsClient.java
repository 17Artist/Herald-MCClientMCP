package ai.herald.clientmod.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal WebSocket client connection. Handles binary/text frame encoding/decoding.
 * Only supports frames up to 64KB for text input, unlimited for binary output.
 */
public final class WsClient {

    private final InputStream in;
    private final OutputStream out;
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final Object writeLock = new Object();

    WsClient(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public boolean isAlive() {
        return alive.get();
    }

    /**
     * Send a binary frame (opcode 0x02).
     */
    public void sendBinary(byte[] data) {
        if (!alive.get()) return;
        try {
            synchronized (writeLock) {
                writeFrame(0x82, data); // FIN + opcode binary
            }
        } catch (IOException e) {
            close();
        }
    }

    /**
     * Read a text frame from the client. Returns null on close/error.
     * Client frames are always masked per RFC 6455.
     */
    public String readTextFrame() {
        try {
            while (alive.get()) {
                int b0 = in.read();
                if (b0 == -1) return null;
                int b1 = in.read();
                if (b1 == -1) return null;

                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long payloadLen = b1 & 0x7F;

                if (payloadLen == 126) {
                    payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
                } else if (payloadLen == 127) {
                    payloadLen = 0;
                    for (int i = 0; i < 8; i++) {
                        payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
                    }
                }

                byte[] maskKey = new byte[4];
                if (masked) {
                    in.readNBytes(maskKey, 0, 4);
                }

                byte[] payload = in.readNBytes((int) payloadLen);
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] ^= maskKey[i % 4];
                    }
                }

                // Handle opcodes
                if (opcode == 0x08) { // Close
                    return null;
                } else if (opcode == 0x09) { // Ping → send Pong
                    synchronized (writeLock) {
                        writeFrame(0x8A, payload);
                    }
                } else if (opcode == 0x01) { // Text
                    return new String(payload, StandardCharsets.UTF_8);
                }
                // Ignore other opcodes (binary from client, continuation, etc.)
            }
        } catch (IOException e) {
            // Connection lost
        }
        return null;
    }

    private void writeFrame(int firstByte, byte[] payload) throws IOException {
        out.write(firstByte);
        if (payload.length < 126) {
            out.write(payload.length);
        } else if (payload.length <= 65535) {
            out.write(126);
            out.write((payload.length >> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        } else {
            out.write(127);
            long len = payload.length;
            for (int i = 7; i >= 0; i--) {
                out.write((int) ((len >> (8 * i)) & 0xFF));
            }
        }
        out.write(payload);
        out.flush();
    }

    public void close() {
        if (alive.compareAndSet(true, false)) {
            try { in.close(); } catch (IOException ignored) {}
            try { out.close(); } catch (IOException ignored) {}
        }
    }
}

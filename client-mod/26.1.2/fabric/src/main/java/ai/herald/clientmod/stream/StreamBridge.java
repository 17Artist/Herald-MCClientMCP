package ai.herald.clientmod.stream;

import ai.herald.clientmod.util.HeraldLogger;
import org.slf4j.Logger;

/**
 * Bridges frame capture (render thread) with the stream WebSocket server.
 * Call {@link #onFrameRendered()} at the end of each frame on the render thread.
 */
public final class StreamBridge {

    private static final Logger LOG = HeraldLogger.of(StreamBridge.class);

    private final StreamServer streamServer;
    private volatile boolean enabled;

    public StreamBridge(StreamServer streamServer) {
        this.streamServer = streamServer;
        // Always enabled: stream frames whenever clients are connected
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Called at the end of each rendered frame. Captures and broadcasts if
     * headless mode is active and clients are connected.
     */
    public void onFrameRendered() {
        if (!enabled) return;
        if (!streamServer.hasClients()) return;
        if (!FrameCapture.shouldCapture()) return;

        byte[] jpeg = FrameCapture.captureFrame();
        if (jpeg != null) {
            streamServer.broadcast(jpeg);
        }
    }
}

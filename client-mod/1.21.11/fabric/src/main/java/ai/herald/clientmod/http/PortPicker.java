package ai.herald.clientmod.http;

import ai.herald.clientmod.HeraldConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Probes a small contiguous port range for the first one we can bind to on
 * {@code 127.0.0.1}. Range defined by {@link HeraldConstants#PORT_RANGE_START}
 * / {@link HeraldConstants#PORT_RANGE_END} (inclusive on both ends).
 */
public final class PortPicker {

    private PortPicker() {}

    public static int pickDefault() {
        return pick(HeraldConstants.PORT_RANGE_START, HeraldConstants.PORT_RANGE_END);
    }

    public static int pick(int startInclusive, int endInclusive) {
        for (int port = startInclusive; port <= endInclusive; port++) {
            if (canBind(port)) {
                return port;
            }
        }
        throw new IllegalStateException(
            "No free port in range " + startInclusive + ".." + endInclusive + " on 127.0.0.1");
    }

    private static boolean canBind(int port) {
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(false);
            s.bind(new InetSocketAddress("127.0.0.1", port), 1);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

package ai.herald.clientmod.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class PortPickerTest {

    @Test
    void picksFirstAvailablePortInRange() {
        // Use a narrow test range to avoid clashes.
        int port = PortPicker.pick(39900, 39910);
        assertTrue(port >= 39900 && port <= 39910);
    }

    @Test
    void skipsBoundPort() throws IOException {
        try (ServerSocket occupied = new ServerSocket()) {
            occupied.setReuseAddress(false);
            occupied.bind(new InetSocketAddress("127.0.0.1", 39920), 1);
            int picked = PortPicker.pick(39920, 39921);
            assertEquals(39921, picked);
        }
    }

    @Test
    void throwsWhenRangeExhausted() throws IOException {
        try (ServerSocket s1 = new ServerSocket();
             ServerSocket s2 = new ServerSocket()) {
            s1.setReuseAddress(false);
            s2.setReuseAddress(false);
            s1.bind(new InetSocketAddress("127.0.0.1", 39930), 1);
            s2.bind(new InetSocketAddress("127.0.0.1", 39931), 1);
            assertThrows(IllegalStateException.class, () -> PortPicker.pick(39930, 39931));
        }
    }
}

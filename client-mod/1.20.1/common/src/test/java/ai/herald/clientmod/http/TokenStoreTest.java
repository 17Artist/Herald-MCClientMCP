package ai.herald.clientmod.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenStoreTest {

    @Test
    void bootWritesSingleLineUuid(@TempDir Path tmp) throws IOException {
        Path heraldDir = tmp.resolve(".herald");
        TokenStore ts = TokenStore.boot(heraldDir);

        assertNotNull(ts.token());
        assertDoesNotThrow(() -> UUID.fromString(ts.token()));

        String content = Files.readString(ts.tokenFile(), StandardCharsets.UTF_8);
        // Contract: file holds UUID followed by a single newline.
        assertEquals(ts.token() + "\n", content);
    }

    @Test
    void cleanupDeletesTokenFile(@TempDir Path tmp) throws IOException {
        TokenStore ts = TokenStore.boot(tmp.resolve(".herald"));
        assertTrue(Files.exists(ts.tokenFile()));
        ts.cleanup();
        assertFalse(Files.exists(ts.tokenFile()));
    }

    @Test
    void cleanupIsIdempotent(@TempDir Path tmp) throws IOException {
        TokenStore ts = TokenStore.boot(tmp.resolve(".herald"));
        ts.cleanup();
        assertDoesNotThrow(ts::cleanup);
    }

    @Test
    void tokenDiffersEachBoot(@TempDir Path tmp) throws IOException {
        TokenStore a = TokenStore.boot(tmp.resolve("a"));
        TokenStore b = TokenStore.boot(tmp.resolve("b"));
        assertNotEquals(a.token(), b.token());
    }
}

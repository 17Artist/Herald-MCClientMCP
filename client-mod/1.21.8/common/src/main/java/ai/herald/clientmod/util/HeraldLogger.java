package ai.herald.clientmod.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thin SLF4J wrapper; all Herald components log through here. */
public final class HeraldLogger {

    private HeraldLogger() {}

    public static Logger of(String name) {
        return LoggerFactory.getLogger("Herald-" + name);
    }

    public static Logger of(Class<?> cls) {
        return LoggerFactory.getLogger("Herald-" + cls.getSimpleName());
    }
}

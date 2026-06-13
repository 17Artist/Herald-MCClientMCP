package ai.herald.clientmod.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded ring buffer of recent incoming chat / system messages.
 * Populated by {@link GameEventBridge} via the loader's chat-receive event.
 * Read by {@code QueryChatHistoryAction}.
 */
public final class ChatHistoryBuffer {

    public static final int CAPACITY = 256;

    public static final class Entry {
        public final long timestampMs;
        public final String text;
        Entry(long timestampMs, String text) {
            this.timestampMs = timestampMs;
            this.text = text;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();

    public void append(String text) {
        if (text == null) return;
        long now = System.currentTimeMillis();
        lock.lock();
        try {
            if (entries.size() >= CAPACITY) entries.pollFirst();
            entries.addLast(new Entry(now, text));
        } finally {
            lock.unlock();
        }
    }

    /** Return up to {@code limit} most recent entries (oldest first). */
    public List<Entry> snapshot(int limit) {
        lock.lock();
        try {
            int n = Math.min(limit <= 0 ? CAPACITY : limit, entries.size());
            List<Entry> out = new ArrayList<>(n);
            int skip = entries.size() - n;
            int i = 0;
            for (Entry e : entries) {
                if (i++ < skip) continue;
                out.add(e);
            }
            return out;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try { return entries.size(); }
        finally { lock.unlock(); }
    }

    public void clear() {
        lock.lock();
        try { entries.clear(); }
        finally { lock.unlock(); }
    }
}

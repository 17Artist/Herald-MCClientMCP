package ai.herald.clientmod.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Minimal tick-driven scheduler: {@link #schedule(int, Runnable)} runs the
 * task after N client ticks. {@link #tick()} must be invoked from the
 * client thread every client-tick.
 */
public final class TickScheduler {

    private static final class Entry {
        int remaining;
        final Runnable task;
        Entry(int remaining, Runnable task) {
            this.remaining = remaining;
            this.task = task;
        }
    }

    /** Thread-safe inbox for schedules that happen on non-client threads. */
    private final ConcurrentLinkedQueue<Entry> inbox = new ConcurrentLinkedQueue<>();
    /** Only mutated on the client thread inside tick(). */
    private final List<Entry> active = new ArrayList<>();

    public void schedule(int delayTicks, Runnable task) {
        if (task == null) throw new IllegalArgumentException("task must not be null");
        if (delayTicks < 0) delayTicks = 0;
        inbox.add(new Entry(delayTicks, task));
    }

    /** Must be called on the client thread. */
    public void tick() {
        // drain inbox
        Entry e;
        while ((e = inbox.poll()) != null) {
            active.add(e);
        }
        // decrement + fire
        int i = 0;
        while (i < active.size()) {
            Entry entry = active.get(i);
            if (entry.remaining <= 0) {
                active.remove(i);
                try {
                    entry.task.run();
                } catch (Throwable t) {
                    // Scheduled tasks are fire-and-forget; swallow to keep tick loop alive.
                    t.printStackTrace();
                }
            } else {
                entry.remaining--;
                i++;
            }
        }
    }

    public int clear() {
        int drained = inbox.size() + active.size();
        inbox.clear();
        active.clear();
        return drained;
    }

    public int pendingCount() {
        return inbox.size() + active.size();
    }
}

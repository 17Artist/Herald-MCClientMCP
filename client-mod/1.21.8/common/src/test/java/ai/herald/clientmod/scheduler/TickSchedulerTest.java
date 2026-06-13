package ai.herald.clientmod.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TickSchedulerTest {

    @Test
    void zeroDelayRunsOnFirstTick() {
        TickScheduler s = new TickScheduler();
        AtomicInteger hits = new AtomicInteger();
        s.schedule(0, hits::incrementAndGet);
        s.tick();
        assertEquals(1, hits.get());
    }

    @Test
    void delayCountsDown() {
        TickScheduler s = new TickScheduler();
        AtomicInteger hits = new AtomicInteger();
        s.schedule(3, hits::incrementAndGet);
        s.tick(); assertEquals(0, hits.get());
        s.tick(); assertEquals(0, hits.get());
        s.tick(); assertEquals(0, hits.get());
        s.tick(); assertEquals(1, hits.get());
    }

    @Test
    void clearCancelsAllPending() {
        TickScheduler s = new TickScheduler();
        AtomicInteger hits = new AtomicInteger();
        s.schedule(5, hits::incrementAndGet);
        s.schedule(5, hits::incrementAndGet);
        assertEquals(2, s.clear());
        s.tick();
        assertEquals(0, hits.get());
    }

    @Test
    void taskExceptionDoesNotKillLoop() {
        TickScheduler s = new TickScheduler();
        AtomicInteger hits = new AtomicInteger();
        s.schedule(0, () -> { throw new RuntimeException("boom"); });
        s.schedule(0, hits::incrementAndGet);
        s.tick();
        assertEquals(1, hits.get());
    }
}

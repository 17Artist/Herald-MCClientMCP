package ai.herald.clientmod.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Fan-out event publisher. Each SSE subscriber gets its own bounded queue
 * — slow consumers drop oldest events rather than blocking publishers.
 */
public final class EventBus {

    public static final int SUBSCRIBER_QUEUE_CAPACITY = 256;

    private final CopyOnWriteArrayList<BlockingQueue<HeraldEvent>> subscribers = new CopyOnWriteArrayList<>();

    public BlockingQueue<HeraldEvent> subscribe() {
        BlockingQueue<HeraldEvent> q = new LinkedBlockingQueue<>(SUBSCRIBER_QUEUE_CAPACITY);
        subscribers.add(q);
        return q;
    }

    public void unsubscribe(BlockingQueue<HeraldEvent> queue) {
        subscribers.remove(queue);
    }

    public void publish(HeraldEvent event) {
        if (event == null) return;
        for (BlockingQueue<HeraldEvent> q : subscribers) {
            // Non-blocking offer; drop oldest if full.
            if (!q.offer(event)) {
                q.poll();
                q.offer(event);
            }
        }
    }

    public int subscriberCount() {
        return subscribers.size();
    }
}

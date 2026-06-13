package ai.herald.clientmod.dispatcher;

import ai.herald.clientmod.util.HeraldLogger;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe map of {@code actionId -> ActionExecutor}.
 *
 * <p>Lifecycle: {@link #register(String, ActionExecutor)} repeatedly during
 * mod init, then {@link #freeze(Set)} once. After freeze, registrations
 * throw and lookups become lock-free.
 */
public final class ActionRegistry {

    private static final Logger LOG = HeraldLogger.of(ActionRegistry.class);

    private final Map<String, ActionExecutor> executors = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean frozen = false;

    public void register(String actionId, ActionExecutor executor) {
        if (actionId == null || actionId.isEmpty()) {
            throw new IllegalArgumentException("actionId must not be empty");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        lock.writeLock().lock();
        try {
            if (frozen) {
                throw new IllegalStateException("ActionRegistry is frozen, cannot register: " + actionId);
            }
            if (executors.containsKey(actionId)) {
                throw new IllegalStateException("Duplicate action id: " + actionId);
            }
            executors.put(actionId, executor);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionExecutor find(String actionId) {
        if (frozen) {
            // Post-freeze: snapshot is immutable, no lock needed.
            return executors.get(actionId);
        }
        lock.readLock().lock();
        try {
            return executors.get(actionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(String actionId) {
        return find(actionId) != null;
    }

    public Set<String> registeredIds() {
        if (frozen) {
            return Collections.unmodifiableSet(executors.keySet());
        }
        lock.readLock().lock();
        try {
            return Set.copyOf(executors.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        return executors.size();
    }

    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Freeze the registry against the canonical action catalog. IDs in the
     * catalog but not registered are logged at WARN (acceptable: phased rollout).
     * IDs registered but not in the catalog are logged at WARN too (suspicious).
     */
    public void freeze(Set<String> catalogIds) {
        lock.writeLock().lock();
        try {
            if (frozen) return;
            if (catalogIds != null) {
                for (String id : catalogIds) {
                    if (!executors.containsKey(id)) {
                        LOG.warn("Action declared in catalog but not registered: {}", id);
                    }
                }
                for (String id : executors.keySet()) {
                    if (!catalogIds.contains(id)) {
                        LOG.warn("Action registered but not declared in catalog: {}", id);
                    }
                }
            }
            frozen = true;
            LOG.info("ActionRegistry frozen with {} actions", executors.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
}

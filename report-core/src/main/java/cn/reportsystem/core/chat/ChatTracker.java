package cn.reportsystem.core.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 环形缓存每个玩家的最近发言，供举报时的聊天快照使用。
 */
public final class ChatTracker {

    private final Map<UUID, Deque<String>> history = new ConcurrentHashMap<UUID, Deque<String>>();
    private final int capacity;

    public ChatTracker(int capacity) {
        this.capacity = Math.max(capacity, 1);
    }

    public void record(UUID player, String message) {
        Deque<String> queue = history.get(player);
        if (queue == null) {
            Deque<String> created = new ArrayDeque<String>();
            Deque<String> existing = history.putIfAbsent(player, created);
            queue = existing == null ? created : existing;
        }
        synchronized (queue) {
            queue.addLast(message);
            while (queue.size() > capacity) {
                queue.removeFirst();
            }
        }
    }

    public List<String> recent(UUID player) {
        Deque<String> queue = history.get(player);
        if (queue == null) {
            return Collections.emptyList();
        }
        synchronized (queue) {
            return new ArrayList<String>(queue);
        }
    }

    public void forget(UUID player) {
        history.remove(player);
    }
}

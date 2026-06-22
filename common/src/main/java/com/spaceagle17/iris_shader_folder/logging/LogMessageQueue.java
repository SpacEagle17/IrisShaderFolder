package com.spaceagle17.iris_shader_folder.logging;

import java.util.LinkedHashMap;

public class LogMessageQueue {
    private final LinkedHashMap<String, LogMessage> map;
    private final int maxSize;
    private static final long SPAM_TIME_WINDOW_MS = 300;

    public LogMessageQueue(int maxSize) {
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(maxSize, 0.75f, false);
    }

    public synchronized int getOccurrenceCount(LogMessage message) {
        long now = System.currentTimeMillis();
        LogMessage existing = map.get(message.getMessage());

        if (existing != null) {
            if ((now - existing.getTimestamp()) <= SPAM_TIME_WINDOW_MS) {
                existing.incrementOccurrenceCount();
                existing.refreshTimestamp();
                return existing.getOccurrenceCount();
            } else {
                map.remove(message.getMessage());
            }
        }
        return -1;
    }

    public synchronized void add(LogMessage message) {
        evictExpired();
        if (map.size() >= maxSize) {
            map.remove(map.keySet().iterator().next());
        }
        map.put(message.getMessage(), message);
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(entry -> (now - entry.getValue().getTimestamp()) > SPAM_TIME_WINDOW_MS);
    }
}

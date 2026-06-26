package net.portaltiertagger.cache;

import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.network.RankingEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankingCache {

    private final int maxSize;
    private final long expireMs;
    private final ConcurrentHashMap<String, RankingEntry> cache = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, RankingEntry> lruOrder = new LinkedHashMap<>(16, 0.75f, true);

    public RankingCache(int maxSize, long expireMs) {
        this.maxSize = maxSize;
        this.expireMs = expireMs;
    }

    public void putAll(List<RankingEntry> entries) {
        for (RankingEntry entry : entries) {
            put(entry);
        }
    }

    public void put(RankingEntry entry) {
        if (entry == null) return;
        synchronized (lruOrder) {
            cache.put(entry.getUsername(), entry);
            lruOrder.put(entry.getUsername(), entry);
            if (lruOrder.size() > maxSize) {
                String oldest = lruOrder.keySet().iterator().next();
                lruOrder.remove(oldest);
                cache.remove(oldest);
            }
        }
    }

    public RankingEntry get(String username) {
        if (username == null) return null;
        String key = username.toLowerCase();
        RankingEntry entry = cache.get(key);
        if (entry == null) return null;
        if (entry.isExpired(expireMs)) {
            cache.remove(key);
            synchronized (lruOrder) { lruOrder.remove(key); }
            return null;
        }
        return entry;
    }

    public int size() { return cache.size(); }

    public void clear() {
        cache.clear();
        synchronized (lruOrder) { lruOrder.clear(); }
        PortalTierTagger.LOGGER.info("Cache cleared");
    }
}

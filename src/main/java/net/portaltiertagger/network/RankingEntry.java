package net.portaltiertagger.network;

import java.util.HashMap;
import java.util.Map;

public class RankingEntry {
    private final String username;
    private final Map<String, String> tiers = new HashMap<>();
    private final long fetchedAt;

    public RankingEntry(String username) {
        this.username = username.toLowerCase();
        this.fetchedAt = System.currentTimeMillis();
    }

    public void addTier(String gamemode, String tier) {
        if (gamemode != null && tier != null) {
            tiers.put(gamemode.toLowerCase(), tier.toUpperCase());
        }
    }

    public String getUsername() { return username; }
    public Map<String, String> getAllTiers() { return tiers; }

    public HighestTierResult getHighestTier() {
        if (tiers.isEmpty()) return null;
        HighestTierResult best = null;
        int bestPriority = 999;
        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            int priority = getTierPriority(entry.getValue());
            if (priority < bestPriority) {
                bestPriority = priority;
                best = new HighestTierResult(entry.getKey(), entry.getValue());
            }
        }
        return best;
    }

    // NEW METHOD: Find the lowest tier
    public HighestTierResult getLowestTier() {
        if (tiers.isEmpty()) return null;
        HighestTierResult worst = null;
        int worstPriority = -1; // Start at the bottom
        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            int priority = getTierPriority(entry.getValue());
            if (priority > worstPriority) { // Note: > instead of <
                worstPriority = priority;
                worst = new HighestTierResult(entry.getKey(), entry.getValue());
            }
        }
        return worst;
    }

    private int getTierPriority(String tier) {
        if (tier == null) return 0;
        switch (tier.toUpperCase()) {
            case "HT1": return 10; // 10 is best
            case "LT1": return 9;
            case "HT2": return 8;
            case "LT2": return 7;
            case "HT3": return 6;
            case "LT3": return 5;
            case "HT4": return 4;
            case "LT4": return 3;
            case "HT5": return 2;
            case "LT5": return 1; // 1 is worst
            default: return 0;
        }
    }

    public boolean isExpired(long expireMs) {
        return (System.currentTimeMillis() - fetchedAt) > expireMs;
    }

    public static class HighestTierResult {
        public final String gamemode;
        public final String tier;
        public HighestTierResult(String gamemode, String tier) {
            this.gamemode = gamemode;
            this.tier = tier;
        }
    }
}

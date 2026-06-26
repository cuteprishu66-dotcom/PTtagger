package net.portaltiertagger.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.portaltiertagger.PortalTierTagger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RankingScraper {

    private static final String DATA_URL = "https://portal-production-5ec6.up.railway.app/api/rankings"; 

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PortalTier-Fetcher");
        t.setDaemon(true);
        return t;
    });

    public CompletableFuture<List<RankingEntry>> fetchAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PortalTierTagger.LOGGER.info("Fetching live tier data from API...");
                URL url = URI.create(DATA_URL).toURL(); // Fixed Java 21 deprecation
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "PortalTierTagger/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    JsonArray array = new Gson().fromJson(new InputStreamReader(is), JsonArray.class);
                    return parseJsonArray(array);
                } else {
                    PortalTierTagger.LOGGER.error("Failed to fetch. HTTP Code: {}", conn.getResponseCode());
                }
            } catch (Exception e) {
                PortalTierTagger.LOGGER.error("Failed to fetch data: {}", e.getMessage());
            }
            return new ArrayList<>();
        }, executor);
    }

    private List<RankingEntry> parseJsonArray(JsonArray array) {
        List<RankingEntry> entries = new ArrayList<>();
        
        for (JsonElement element : array) {
            try {
                JsonObject playerObj = element.getAsJsonObject();
                
                if (!playerObj.has("minecraftUsername") || playerObj.get("minecraftUsername").isJsonNull()) continue;
                String username = playerObj.get("minecraftUsername").getAsString();
                
                if (username == null || username.trim().isEmpty()) continue;

                RankingEntry entry = new RankingEntry(username);

                if (playerObj.has("ranks") && !playerObj.get("ranks").isJsonNull()) {
                    JsonObject ranks = playerObj.getAsJsonObject("ranks");
                    for (Map.Entry<String, JsonElement> rankEntry : ranks.entrySet()) {
                        entry.addTier(rankEntry.getKey(), rankEntry.getValue().getAsString());
                    }
                }
                entries.add(entry);
                
            } catch (Exception e) {
                PortalTierTagger.LOGGER.warn("Failed to parse a player entry");
            }
        }
        return entries;
    }
}

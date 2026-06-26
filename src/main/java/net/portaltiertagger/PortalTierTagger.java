package net.portaltiertagger;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.portaltiertagger.cache.RankingCache;
import net.portaltiertagger.command.TierTaggerCommand;
import net.portaltiertagger.config.ModConfig;
import net.portaltiertagger.keybind.ModKeybinds;
import net.portaltiertagger.network.RankingScraper;
import net.portaltiertagger.render.PlayerNameRenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class PortalTierTagger implements ClientModInitializer {

    public static final String MOD_ID = "portal_tier_tagger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;
    private static ConfigHolder<ModConfig> configHolder;
    private static RankingCache cache;
    private static RankingScraper scraper;
    private static CompletableFuture<Void> currentFetch;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Portal Tier Tagger...");

        configHolder = AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = configHolder.getConfig();

        cache = new RankingCache(config.cacheMaxSize, config.cacheExpireMinutes * 60_000L);
        scraper = new RankingScraper();

        ModKeybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ModKeybinds.tick());

        // Register the nametag renderer
        WorldRenderEvents.LAST.register(PlayerNameRenderHelper::renderTierTags);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            TierTaggerCommand.register(dispatcher, registryAccess));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.info("Joined server — fetching all player tiers...");
            // Reset font availability check so it re-verifies with the new resource manager
            PlayerNameRenderHelper.resetFontCache();
            triggerRefresh();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Disconnected — clearing tier cache.");
            cache.clear();
            PlayerNameRenderHelper.resetFontCache();
        });

        LOGGER.info("Portal Tier Tagger Ready.");
    }

    public static void triggerRefresh() {
        if (currentFetch != null && !currentFetch.isDone()) return;

        currentFetch = scraper.fetchAllPlayers().thenAccept(entries -> {
            if (!entries.isEmpty()) {
                cache.putAll(entries);
                LOGGER.info("Tier cache updated. Tracking {} players.", cache.size());
            }
        }).exceptionally(ex -> {
            LOGGER.error("Failed to fetch tiers: {}", ex.getMessage());
            return null;
        });
    }

    public static ModConfig getConfig() { return config; }
    public static ConfigHolder<ModConfig> getConfigManager() { return configHolder; }
    public static RankingCache getCache() { return cache; }
    public static MinecraftClient getClient() { return MinecraftClient.getInstance(); }
}

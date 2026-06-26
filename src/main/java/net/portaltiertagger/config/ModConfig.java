package net.portaltiertagger.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.Map;

@Config(name = "portal_tier_tagger")
public class ModConfig implements ConfigData {

    // --- NEW ENUMS FOR CONFIG ---
    public enum DisplayMode { HIGHEST, LOWEST }
    public enum DisplaySide { LEFT, CENTER, RIGHT, DISABLED }

    @ConfigEntry.Category("display")
    public boolean enabled = true; // This acts as "Disable Tiers"

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DisplayMode displayMode = DisplayMode.HIGHEST; // Highest or Lowest

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DisplaySide displaySide = DisplaySide.CENTER; // Left, Right, or Disabled

    @ConfigEntry.Category("display")
    public double offsetY = 0.3;
    
    @ConfigEntry.Category("display")
    @ConfigEntry.BoundedDiscrete(min = 4, max = 128)
    public int distanceCutoff = 32;

    @ConfigEntry.Category("api")
    public String apiUrl = "https://portal-production-5ec6.up.railway.app/api/rankings";
    
    @ConfigEntry.Category("api")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 60)
    public int refreshIntervalMinutes = 5;

    @ConfigEntry.Category("cache")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int cacheExpireMinutes = 5;
    
    @ConfigEntry.Category("cache")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 5000)
    public int cacheMaxSize = 500;

    @ConfigEntry.Gui.Excluded
    public Map<String, String> rankColors = new HashMap<>();

    public ModConfig() {
        rankColors.put("HT1", "#FFD700");
        rankColors.put("LT1", "#E0E0E0");
        rankColors.put("HT2", "#FF5555");
        rankColors.put("LT2", "#5555FF");
        rankColors.put("HT3", "#CC55FF");
        rankColors.put("LT3", "#0000AA");
        rankColors.put("HT4", "#55FFFF");
        rankColors.put("LT4", "#AAAAAA");
        rankColors.put("HT5", "#55FF55");
        rankColors.put("LT5", "#555555");
        rankColors.put("default", "#FFFFFF"); 
    }

    public String getRankColor(String rank) {
        return rankColors.getOrDefault(rank, rankColors.getOrDefault("default", "#FFFFFF"));
    }
}

package net.portaltiertagger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.config.ModConfig;
import net.portaltiertagger.network.RankingEntry;

import java.awt.Color;
import java.util.Map;

public class TierTaggerCommand {

    // ── FIX 1: Reuse the same font ID constant everywhere ──────────────────────
    private static final Identifier FONT_ID = Identifier.of("portal_tier_tagger", "default");

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("tiertagger")
            .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                ModConfig config = PortalTierTagger.getConfig();
                if (config != null) {
                    config.enabled = !config.enabled;
                    if (PortalTierTagger.getConfigManager() != null) PortalTierTagger.getConfigManager().save();
                    ctx.getSource().sendFeedback(Text.literal("Tier tags: ")
                        .append(Text.literal(config.enabled ? "ENABLED" : "DISABLED")
                            .formatted(config.enabled ? Formatting.GREEN : Formatting.RED)));
                }
                return 1;
            }))

            .then(ClientCommandManager.literal("refresh").executes(ctx -> {
                PortalTierTagger.triggerRefresh();
                ctx.getSource().sendFeedback(Text.literal("Refreshing rankings...").formatted(Formatting.YELLOW));
                return 1;
            }))

            .then(ClientCommandManager.literal("cache")
                .then(ClientCommandManager.literal("clear").executes(ctx -> {
                    PortalTierTagger.getCache().clear();
                    ctx.getSource().sendFeedback(Text.literal("Cache cleared.").formatted(Formatting.GREEN));
                    return 1;
                }))
                .then(ClientCommandManager.literal("size").executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("Cache size: " + PortalTierTagger.getCache().size() + " players").formatted(Formatting.AQUA));
                    return 1;
                }))
            )

            // ── FIX 2: Use word() instead of greedyString() ──────────────────
            // greedyString() was consuming extra characters from tab-completion
            // suggestions, causing the argument to be parsed as a partial string
            // (e.g. "4" instead of "NightRyze_").  word() captures exactly one
            // whitespace-delimited token, which is all we need for a player name.
            .then(ClientCommandManager.literal("rank")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "player");
                        RankingEntry entry = PortalTierTagger.getCache().get(name);

                        if (entry != null && entry.getAllTiers() != null && !entry.getAllTiers().isEmpty()) {
                            ModConfig config = PortalTierTagger.getConfig();

                            // Header
                            ctx.getSource().sendFeedback(Text.literal("")
                                .append(Text.literal(name + "'s Tiers:").formatted(Formatting.GOLD, Formatting.BOLD)));

                            // One line per gamemode
                            for (Map.Entry<String, String> tierData : entry.getAllTiers().entrySet()) {
                                String gamemode = tierData.getKey();
                                String tier     = tierData.getValue();

                                // ── FIX 3: Build emoji text with explicit font ──────────────────────
                                // The emoji characters (\uE001-\uE008) only exist in our custom bitmap
                                // font.  Without withFont(FONT_ID) they render as □ (tofu/plain box)
                                // in chat.  We must set the Style on the literal that contains them.
                                Style emojiStyle   = Style.EMPTY.withFont(FONT_ID);
                                MutableText emojiText = Text.literal(getGamemodeEmoji(gamemode) + " ")
                                        .setStyle(emojiStyle);

                                // Tier colour
                                int colorInt = 0xFFFFFF;
                                if (config != null) {
                                    String colorHex = config.getRankColor(tier);
                                    Color color = parseHex(colorHex);
                                    // ── FIX 4: Use 0xFF alpha, not color.getAlpha() ────────────────
                                    // parseHex() creates Color(int) which sets alpha=255 correctly,
                                    // but being explicit avoids surprises if the helper ever changes.
                                    colorInt = (0xFF << 24)
                                             | (color.getRed()   << 16)
                                             | (color.getGreen() << 8)
                                             |  color.getBlue();
                                }
                                MutableText tierText     = Text.literal("[" + tier + "] ").withColor(colorInt);
                                MutableText gamemodeText = Text.literal(gamemode).formatted(Formatting.GRAY);

                                MutableText line = emojiText.append(tierText).append(gamemodeText);
                                ctx.getSource().sendFeedback(line);
                            }
                        } else {
                            ctx.getSource().sendFeedback(
                                Text.literal(name + " is not ranked or not in cache.")
                                    .formatted(Formatting.RED));
                        }
                        return 1;
                    })
                )
            )
        );
    }

    // ── FIX 5: switch cases must match the keys stored in RankingEntry ─────────
    // RankingEntry.addTier() calls gamemode.toLowerCase() before storing, so the
    // map always has lowercase keys.  These cases were already lowercase — kept
    // as-is; confirmed correct.
    private static String getGamemodeEmoji(String gamemode) {
        if (gamemode == null) return "";
        switch (gamemode.toLowerCase()) {
            case "mace":    return "\uE001";
            case "sword":   return "\uE002";
            case "axe":     return "\uE003";
            case "smp":     return "\uE004";
            case "uhc":     return "\uE005";
            case "pot":     return "\uE006";
            case "nethop":  return "\uE007";
            case "vanilla": return "\uE008";
            default:        return "";
        }
    }

    private static Color parseHex(String hex) {
        try {
            if (hex == null) return new Color(0xFFFFFF);
            hex = hex.replace("#", "");
            if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException ignored) {}
        return new Color(0xFFFFFF);
    }
}

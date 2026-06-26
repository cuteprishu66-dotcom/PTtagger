package net.portaltiertagger.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.config.ModConfig;
import net.portaltiertagger.network.RankingEntry;

import java.awt.Color;

public class PlayerNameRenderHelper {

    // Font identifier — resolves to:
    //   assets/portal_tier_tagger/font/default.json
    private static final Identifier FONT_ID =
            Identifier.of("portal_tier_tagger", "default");

    // Resource-manager path used only to check that the font file is present
    private static final Identifier FONT_RESOURCE_PATH =
            Identifier.of("portal_tier_tagger", "font/default.json");

    // Cached once per session so we only log once
    private static Boolean fontAvailable = null;

    public static void renderTierTags(WorldRenderContext context) {
        ModConfig config = PortalTierTagger.getConfig();
        if (config == null) return;
        if (!config.enabled) return;
        if (config.displaySide == ModConfig.DisplaySide.DISABLED) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.world  == null) return;

        // Check font availability once and log result
        if (fontAvailable == null) {
            fontAvailable = client.getResourceManager()
                    .getResource(FONT_RESOURCE_PATH)
                    .isPresent();
            if (fontAvailable) {
                PortalTierTagger.LOGGER.info("[PTT] Custom font loaded: {}", FONT_ID);
            } else {
                PortalTierTagger.LOGGER.warn(
                        "[PTT] Custom font NOT found at {}. Falling back to plain text.",
                        FONT_RESOURCE_PATH);
            }
        }

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vcp = context.consumers();
        if (vcp == null) return;

        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player && !client.gameRenderer.getCamera().isThirdPerson()) continue;

            double distSq = player.squaredDistanceTo(cameraPos);
            if (distSq > (double) config.distanceCutoff * config.distanceCutoff) continue;

            if (!player.isAlive()) continue;
            if (player.isInvisibleTo(client.player)) continue;

            String name = player.getName().getString();
            RankingEntry entry = PortalTierTagger.getCache().get(name);

            PortalTierTagger.LOGGER.debug(
                    "[PTT] Player={} cacheHit={} dist={}",
                    name, (entry != null), (int) Math.sqrt(distSq));

            if (entry == null) continue;

            RankingEntry.HighestTierResult targetTier = null;
            if (config.displayMode == ModConfig.DisplayMode.HIGHEST) {
                targetTier = entry.getHighestTier();
            } else if (config.displayMode == ModConfig.DisplayMode.LOWEST) {
                targetTier = entry.getLowestTier();
            }

            if (targetTier == null) continue;

            PortalTierTagger.LOGGER.debug(
                    "[PTT]   -> gamemode={} tier={}", targetTier.gamemode, targetTier.tier);

            renderTierTag(matrices, vcp, player, cameraPos,
                    targetTier.gamemode, targetTier.tier, config, tickDelta);
        }
    }

    private static void renderTierTag(MatrixStack matrices, VertexConsumerProvider vcp,
                                      PlayerEntity player, Vec3d cameraPos,
                                      String gamemode, String tier, ModConfig config,
                                      float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // ── Build the display text ──────────────────────────────────────────────
        MutableText finalText;

        if (Boolean.TRUE.equals(fontAvailable)) {

            // FIX A: The emoji character and the space that follows it must BOTH
            // be inside the custom-font span.  If we split them into separate
            // Text.literal() calls with different styles, the space reverts to the
            // default font and the glyph advance is wrong, pushing the tier text
            // too far to the right.  Keep them in one literal with emojiStyle.
            Style emojiStyle  = Style.EMPTY.withFont(FONT_ID);
            MutableText emojiText = Text.literal(getGamemodeEmoji(gamemode) + " ")
                    .setStyle(emojiStyle);

            // FIX B: Always use 0xFF for the alpha channel.  withColor(int) treats
            // the int as ARGB; if alpha is 0 the text is invisible even though the
            // RGB values look right in a debugger.
            String colorHex = config.getRankColor(tier);
            Color  color    = parseHex(colorHex);
            int    colorInt = (0xFF << 24)
                            | (color.getRed()   << 16)
                            | (color.getGreen() << 8)
                            |  color.getBlue();

            MutableText tierText = Text.literal("[" + tier + "]").withColor(colorInt);
            finalText = emojiText.append(tierText);

        } else {
            // Plain-text fallback when font is unavailable
            String code = gamemode.substring(0, Math.min(3, gamemode.length())).toUpperCase();
            finalText = Text.literal("[" + code + "][" + tier + "]").withColor(0xAAAAAA);
        }

        // ── Camera-relative interpolated position ──────────────────────────────
        double interpX = player.prevX + (player.getX() - player.prevX) * tickDelta;
        double interpY = player.prevY + (player.getY() - player.prevY) * tickDelta;
        double interpZ = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;

        double relX = interpX - cameraPos.x;
        double relY = interpY - cameraPos.y;
        double relZ = interpZ - cameraPos.z;

        float yOffset   = player.getHeight() + 0.25f + (float) config.offsetY;
        float scale     = 0.025f;
        float textWidth = textRenderer.getWidth(finalText) * scale;

        float xOffset = 0f;
        if (config.displaySide == ModConfig.DisplaySide.LEFT) {
            xOffset = -textWidth - 0.15f;
        } else if (config.displaySide == ModConfig.DisplaySide.RIGHT) {
            xOffset = 0.15f;
        }

        matrices.push();
        matrices.translate(relX, relY + yOffset, relZ);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        // Negative X flips the text right-side-up (MC text renders upside-down)
        matrices.scale(-scale, -scale, scale);

        float drawX = -(textRenderer.getWidth(finalText) / 2.0f) + (xOffset / scale);

        textRenderer.draw(
                finalText,
                drawX,
                0.0f,
                0xFFFFFF,
                false,
                matrices.peek().getPositionMatrix(),
                vcp,
                TextRenderer.TextLayerType.SEE_THROUGH,  // visible through blocks
                0,
                15728880   // full brightness
        );

        matrices.pop();
    }

    // ── Emoji map — must match the chars defined in default.json ──────────────
    // FIX C: The gamemode keys in RankingEntry are stored lowercase (see
    // addTier()), so we compare against lowercase here.  All cases already
    // matched — verified against default.json.
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
            if (hex == null) return new Color(0xAAAAAA);
            hex = hex.replace("#", "");
            if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException ignored) {}
        return new Color(0xAAAAAA);
    }

    /** Call this on world disconnect to re-check font on next join */
    public static void resetFontCache() {
        fontAvailable = null;
    }
}

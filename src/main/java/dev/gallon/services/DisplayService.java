package dev.gallon.services;

import dev.gallon.domain.DisplayMinMax;
import dev.gallon.domain.GroupedKind;
import dev.gallon.domain.HorseStats;
import dev.gallon.domain.I18nKeys;
import dev.gallon.domain.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DisplayService {
    /**
     * One statistic of a mount, already formatted according to the config.
     *
     * @param labelKey   the translation key of the stat's name
     * @param valueStr   the value, or the percentage when the config asks for percentages
     * @param rawStr     the value, whatever the config says about percentages
     * @param percentStr the percentage, whatever the config says about percentages
     * @param minStr     the min possible value to display
     * @param maxStr     the max possible value to display
     */
    record Stat(
            @NotNull String labelKey,
            double value,
            double min,
            double max,
            @NotNull String valueStr,
            @NotNull String rawStr,
            @NotNull String percentStr,
            @NotNull String minStr,
            @NotNull String maxStr
    ) {
    }

    /**
     * The stats and the comparison are displayed next to the inventory by {@link InventoryPanels}. When they are turned
     * off, the stats are only displayed in a tooltip when hovering the mount's name.
     */
    public static void displayContainerStats(
            @NotNull GuiGraphicsExtractor guiGraphics,
            @NotNull ModConfig config,
            @NotNull HorseStats stats,
            int containerWidth,
            int containerMouseX,
            int containerMouseY
    ) {
        if (!config.getDisplayStatsInInventory()) {
            displayStatsInHoveringText(
                    guiGraphics, config, stats, containerWidth, containerMouseX, containerMouseY
            );
        }
    }

    private static void displayStatsInHoveringText(
            @NotNull GuiGraphicsExtractor guiGraphics,
            @NotNull ModConfig config,
            @NotNull HorseStats stats,
            int containerWidth,
            int containerMouseX,
            int containerMouseY
    ) {
        // This represents a rectangle that contains all the header of the container (horse name)
        final int RX = 3;
        final int RY = 3;
        final int RW = containerWidth - 6;
        final int RH = 14;

        if (posInRect(containerMouseX, containerMouseY, RX, RY, RW, RH)) {
            List<Component> textLines = new ArrayList<>();

            for (Stat stat : buildStats(config, stats, true)) {
                textLines.add(Component.literal(buildStatText(config, stat)));
            }

            // Owner
            if (stats.owner().isPresent()) {
                textLines.add(
                        Component.literal(
                                I18n.get(I18nKeys.OWNER) + ": " + stats.owner().get() +
                                        reset(config)
                        )
                );
            }

            drawHoveringText(
                    guiGraphics,
                    containerMouseX,
                    containerMouseY,
                    textLines
            );
        }
    }

    public static Component buildOverlayMessage(@NotNull ModConfig config, @NotNull HorseStats stats) {
        return buildOverlayMessage(config, stats, Optional.empty());
    }

    /**
     * @param breeding the breeding status to add at the end, when there is something to say about it
     */
    public static Component buildOverlayMessage(
            @NotNull ModConfig config,
            @NotNull HorseStats stats,
            @NotNull Optional<String> breeding
    ) {
        final String reset = reset(config);
        final String overall = I18n.get(I18nKeys.OVERALL) + ": " +
                getColorTextFormat(config, stats.getGroupedStats(), 0, 100) + stats.getGroupedStatsStr(config);
        final Optional<String> owner = stats.owner().map(name -> I18n.get(I18nKeys.OWNER) + ": " + name);

        List<String> parts = new ArrayList<>();
        switch (config.getGroupedStats()) {
            case GroupedKind.GROUPED :
            default:
                return Component.literal(
                I18n.get(I18nKeys.STATS) + ": " +
                        getColorTextFormat(config, stats.getGroupedStats(),0, 100) + stats.getGroupedStatsStr(config) +
                        reset + " " + owner.orElse("") + breeding.map(text -> " " + text).orElse("")
                );
            case GroupedKind.INDIVIDUAL :
                buildStats(config, stats, true).forEach(stat -> parts.add(buildStatText(config, stat)));
                owner.ifPresent(parts::add);
                breeding.ifPresent(parts::add);
                break;
            case GroupedKind.GROUPED_AND_INDIVIDUAL :
                buildStats(config, stats, true).forEach(stat -> parts.add(buildStatText(config, stat)));
                owner.ifPresent(parts::add);
                parts.add(overall);
                breeding.ifPresent(parts::add);
                break;
        }

        return Component.literal(String.join(reset + " ", parts));
    }

    /**
     * @return one line per statistic, from top to bottom, to be displayed above the mount's head
     */
    public static List<Component> buildAboveHeadLines(@NotNull ModConfig config, @NotNull HorseStats stats) {
        List<Component> lines = new ArrayList<>();

        if (config.getGroupedStats() == GroupedKind.GROUPED) {
            lines.add(Component.literal(
                    I18n.get(I18nKeys.STATS) + ": " +
                            getColorTextFormat(config, stats.getGroupedStats(), 0, 100) + stats.getGroupedStatsStr(config)
            ));
            return lines;
        }

        for (Stat stat : buildStats(config, stats, true)) {
            lines.add(Component.literal(buildStatText(config, stat)));
        }

        if (config.getGroupedStats() == GroupedKind.GROUPED_AND_INDIVIDUAL) {
            lines.add(Component.literal(
                    I18n.get(I18nKeys.OVERALL) + ": " +
                            getColorTextFormat(config, stats.getGroupedStats(), 0, 100) + stats.getGroupedStatsStr(config)
            ));
        }

        return lines;
    }

    /**
     * @param includeSlots whether to add the slots, when the mount has some
     * @return the health, jump height, speed and slots of the mount, formatted according to the config
     */
    static List<Stat> buildStats(@NotNull ModConfig config, @NotNull HorseStats stats, boolean includeSlots) {
        List<Stat> result = new ArrayList<>();
        result.add(new Stat(
                I18nKeys.HEALTH,
                stats.health(), stats.minHealth(), stats.maxHealth(),
                stats.getHealthStr(config),
                stats.getHealthValueStr(config),
                stats.getHealthPercentageStr(config),
                HorseStats.formatHealth(stats.minHealth(), config),
                HorseStats.formatHealth(stats.maxHealth(), config)
        ));
        result.add(new Stat(
                I18nKeys.JUMP_HEIGHT,
                stats.jumpHeight(), stats.minJumpHeight(), stats.maxJumpHeight(),
                stats.getJumpHeightStr(config),
                stats.getJumpHeightValueStr(config),
                stats.getJumpHeightPercentageStr(config),
                HorseStats.formatJumpHeight(stats.minJumpHeight(), config),
                HorseStats.formatJumpHeight(stats.maxJumpHeight(), config)
        ));
        result.add(new Stat(
                I18nKeys.SPEED,
                stats.speed(), stats.minSpeed(), stats.maxSpeed(),
                stats.getSpeedStr(config),
                stats.getSpeedValueStr(config),
                stats.getSpeedPercentageStr(config),
                HorseStats.formatSpeed(stats.minSpeed(), config),
                HorseStats.formatSpeed(stats.maxSpeed(), config)
        ));
        if (includeSlots) {
            stats.slots().ifPresent(slots -> result.add(new Stat(
                    I18nKeys.SLOTS,
                    slots, stats.minSlots(), stats.maxSlots(),
                    stats.getSlotsStr(config),
                    stats.getSlotsValueStr(),
                    stats.getSlotsPercentageStr(config),
                    stats.minSlots().toString(),
                    stats.maxSlots().toString()
            )));
        }
        return result;
    }

    /**
     * @return the stat as "Name: value", with its min and max according to the config
     */
    private static String buildStatText(@NotNull ModConfig config, @NotNull Stat stat) {
        // no min max when using percentage, it makes no sense
        final DisplayMinMax displayMinMax = config.getDisplayStatsInPercentage() ? DisplayMinMax.DISABLED : config.getDisplayMinMax();
        final boolean displayMin = displayMinMax == DisplayMinMax.MIN_AND_MAX || displayMinMax == DisplayMinMax.MIN_ONLY;
        final boolean displayMax = displayMinMax == DisplayMinMax.MIN_AND_MAX || displayMinMax == DisplayMinMax.MAX_ONLY;
        final String red = config.getColoredStats() ? ChatFormatting.RED.toString() : "";
        final String green = config.getColoredStats() ? ChatFormatting.GREEN.toString() : "";
        final String reset = reset(config);

        return I18n.get(stat.labelKey()) + ": " +
                (displayMin ? (red + stat.minStr() + reset + "/") : "") +
                getColorTextFormat(config, stat.value(), stat.min(), stat.max()) + stat.valueStr() +
                (displayMax ? (reset + "/" + green + stat.maxStr()) : "");
    }

    static String reset(@NotNull ModConfig config) {
        return config.getColoredStats() ? ChatFormatting.RESET.toString() : "";
    }

    public static void renderOverlayMessage(
            @NotNull GuiGraphicsExtractor guiGraphics,
            @NotNull Component message,
            int remainingTicks,
            @NotNull DeltaTracker deltaTracker
    ) {
        float time = remainingTicks - deltaTracker.getGameTimeDeltaPartialTick(false);
        int alpha = Math.min(255, (int) (time * 255.0F / 20.0F));
        if (alpha <= 0) {
            return;
        }

        var font = Minecraft.getInstance().font;
        int width = font.width(message);

        guiGraphics.nextStratum();
        guiGraphics.pose().pushMatrix();
        try {
            guiGraphics.pose().translate(guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - 68);
            guiGraphics.textWithBackdrop(
                    font,
                    message,
                    -width / 2,
                    -4,
                    width,
                    alpha << 24 | 0xFFFFFF
            );
        } finally {
            guiGraphics.pose().popMatrix();
        }
    }

    private static void drawHoveringText(GuiGraphicsExtractor guiGraphics, int x, int y, List<Component> textLines) {
        // Deferred tooltips are extracted after the container pose has been restored.
        // Convert the local hover position to scaled screen coordinates while it is still available.
        Vector2f screenPosition = guiGraphics.pose().transformPosition(x, y, new Vector2f());
        guiGraphics.setTooltipForNextFrame(
                Minecraft.getInstance().font,
                textLines,
                Optional.empty(),
                Math.round(screenPosition.x),
                Math.round(screenPosition.y)
        );
    }

    /**
     * @param val the value
     * @param min the min possible value
     * @param max the max possible value
     * @return a hex color according to the percentage of val in min/max
     */
    static int getColorHex(double val, double min, double max) {
        double p = getPercentage(val, min, max);

        if (p <= 25) {
            return 0xffd12300;
        } else if (p > 25 && p <= 50) {
            return 0xffd18800;
        } else if (p > 50 && p <= 75) {
            return 0xfffae314;
        } else {
            return 0xff77bf04;
        }
    }

    /**
     * @param val the value
     * @param min the min possible value
     * @param max the max possible value
     * @return a ChatFormatting color according to the percentage of val in min/max
     */
    private static ChatFormatting getColorTextFormat(double val, double min, double max) {
        double p = getPercentage(val, min, max);

        if (p <= 25) {
            return ChatFormatting.RED;
        } else if (p > 25 && p <= 50) {
            return ChatFormatting.GOLD;
        } else if (p > 50 && p <= 75) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.GREEN;
        }
    }

    static String getColorTextFormat(ModConfig config, double val, double min, double max) {
        return config.getColoredStats() ? getColorTextFormat(val, min, max).toString() : "";
    }

    /**
     * Basic percentage function
     *
     * @param val the value
     * @param min the min possible value
     * @param max the max possible value
     * @return the percentage (0<=x<=100)
     */
    private static double getPercentage(double val, double min, double max) {
        if (min == max) {
            return 100;
        }
        return 100d * (val - min) / (max - min);
    }

    /**
     * @param px x position to test
     * @param py y position to test
     * @param rx x position of rect
     * @param ry y position of rect
     * @param rw width of rect
     * @param rh height of rect
     * @return true if the given position is in the given rectangle
     */
    private static boolean posInRect(int px, int py, int rx, int ry, int rw, int rh) {
        // These multiplications are here to match the rectangle drawn by calling AbstractGui.fillRect(a, b, c, d, col)
        // The rectangle given is multiplied by the gui scale to match the rectangle that would be drawn with the
        // same parameters.
        // When creating a rectangle, I use the fillRect function to visualize the rectangle. Then I use this function
        // to check if the mouse is inside or not. This multiplication prevents doing a lot more when calling this
        // function. Just ignore that, act like if it worked as intended. It's a little hack.
        return (px >= rx && px <= rx + rw) && (py >= ry && py <= ry + rh);
    }
}

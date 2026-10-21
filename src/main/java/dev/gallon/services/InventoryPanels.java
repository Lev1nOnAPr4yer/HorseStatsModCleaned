package dev.gallon.services;

import dev.gallon.CompareState;
import dev.gallon.domain.BreedingOdds;
import dev.gallon.domain.BreedingParent;
import dev.gallon.domain.BreedingStatus;
import dev.gallon.domain.DisplayMinMax;
import dev.gallon.domain.HorseStats;
import dev.gallon.domain.I18nKeys;
import dev.gallon.domain.InventoryLayout;
import dev.gallon.domain.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The two panels displayed next to a mount's inventory: all its stats, and the comparison of two mounts.
 */
public final class InventoryPanels {
    public static final int COMPARE_WIDTH = 120;

    private static final int PADDING = 5;
    private static final int ROW_HEIGHT = 11;
    private static final int COLUMN_GAP = 8;
    private static final int TITLE_HEIGHT = 12;
    private static final int LINE_HEIGHT = 9;

    // Compare panel: where the title, the selected mounts, the buttons and the results are, from the top of the panel
    private static final int SLOT_Y = PADDING + TITLE_HEIGHT;
    private static final int BUTTONS_Y = SLOT_Y + 2 * ROW_HEIGHT + 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_STEP = 22;
    private static final int RESULTS_Y = BUTTONS_Y + 3 * BUTTON_STEP + 4;
    private static final int CHANCE_ROWS = 4;

    // The breeding status takes a title, its state, and up to two lines of details. It always takes the same room, so
    // that the panel doesn't change size when the status does.
    private static final int BREEDING_STEP = LINE_HEIGHT + 1;
    private static final int BREEDING_DETAIL_LINES = 3;
    private static final int BREEDING_HEIGHT = 4 + (2 + BREEDING_DETAIL_LINES) * BREEDING_STEP;

    private static final int BORDER = 0xFF5B3A9E;
    private static final int BACKGROUND = 0xFF100010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA0A0A0;
    private static final int ERROR = 0xFFFF5555;

    private InventoryPanels() {
    }

    private record Row(@NotNull String label, @NotNull String value, @NotNull String percent, @NotNull String range, int color) {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Stats panel
    // ------------------------------------------------------------------------------------------------------------

    public static int statsWidth(@NotNull Font font, @NotNull ModConfig config, @NotNull HorseStats stats) {
        List<Row> rows = statsRows(config, stats);

        int label = 0;
        int value = 0;
        int percent = 0;
        int range = 0;
        for (Row row : rows) {
            label = Math.max(label, font.width(row.label()));
            value = Math.max(value, font.width(row.value()));
            percent = Math.max(percent, font.width(row.percent()));
            range = Math.max(range, font.width(row.range()));
        }

        int table = label + COLUMN_GAP + value + COLUMN_GAP + percent + (range > 0 ? COLUMN_GAP + range : 0);
        int width = Math.max(table, font.width(I18n.get(I18nKeys.STATS)));
        for (String unit : units()) {
            width = Math.max(width, font.width(unit));
        }
        return width + 2 * PADDING;
    }

    public static int statsHeight(@NotNull ModConfig config, @NotNull HorseStats stats) {
        int rows = statsRows(config, stats).size();
        return PADDING + TITLE_HEIGHT + rows * ROW_HEIGHT + 4 + (stats.owner().isPresent() ? ROW_HEIGHT : 0)
                + (config.getDisplayBreedingStatus() ? BREEDING_HEIGHT : 0)
                + units().size() * LINE_HEIGHT + PADDING;
    }

    public static void drawStats(
            @NotNull GuiGraphicsExtractor graphics,
            @NotNull Font font,
            @NotNull ModConfig config,
            @NotNull HorseStats stats,
            @Nullable BreedingStatus breeding,
            @NotNull InventoryLayout.Rect rect,
            int originX,
            int originY
    ) {
        int x = originX + rect.x();
        int y = originY + rect.y();
        drawPanel(graphics, x, y, rect.width(), rect.height());

        List<Row> rows = statsRows(config, stats);
        int percentWidth = 0;
        int valueWidth = 0;
        int rangeWidth = 0;
        for (Row row : rows) {
            percentWidth = Math.max(percentWidth, font.width(row.percent()));
            valueWidth = Math.max(valueWidth, font.width(row.value()));
            rangeWidth = Math.max(rangeWidth, font.width(row.range()));
        }

        // label | value | percentage | range, the numbers are aligned on their right
        int rangeX = x + rect.width() - PADDING - rangeWidth;
        int percentRight = rangeWidth > 0 ? rangeX - COLUMN_GAP : x + rect.width() - PADDING;
        int valueRight = percentRight - percentWidth - COLUMN_GAP;

        drawText(graphics, font, I18n.get(I18nKeys.STATS), x + PADDING, y + PADDING, TEXT);

        int rowY = y + PADDING + TITLE_HEIGHT;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i == rows.size() - 1) {
                // A little space before the overall percentage, which isn't a stat of the mount
                rowY += 4;
            }
            drawText(graphics, font, row.label(), x + PADDING, rowY, TEXT);
            drawText(graphics, font, row.value(), valueRight - font.width(row.value()), rowY, row.color());
            drawText(graphics, font, row.percent(), percentRight - font.width(row.percent()), rowY, row.color());
            if (!row.range().isEmpty()) {
                drawText(graphics, font, row.range(), rangeX, rowY, MUTED);
            }
            rowY += ROW_HEIGHT;
        }

        if (stats.owner().isPresent()) {
            String owner = font.plainSubstrByWidth(ownerLine(stats), rect.width() - 2 * PADDING);
            drawText(graphics, font, owner, x + PADDING, rowY, MUTED);
            rowY += ROW_HEIGHT;
        }
        if (breeding != null) {
            drawBreeding(graphics, font, breeding, x + PADDING, rowY + 4, rect.width() - 2 * PADDING);
            rowY += BREEDING_HEIGHT;
        }
        for (String unit : units()) {
            drawText(graphics, font, unit, x + PADDING, rowY, MUTED);
            rowY += LINE_HEIGHT;
        }
    }

    private static void drawBreeding(
            GuiGraphicsExtractor graphics, Font font, BreedingStatus breeding, int x, int y, int width
    ) {
        drawText(graphics, font, I18n.get(I18nKeys.BREEDING), x, y, TEXT);
        drawText(graphics, font, font.plainSubstrByWidth(BreedingText.label(breeding), width), x, y + BREEDING_STEP, BreedingText.color(breeding));

        List<FormattedCharSequence> detail = BreedingText.detail(breeding)
                .map(text -> font.split(Component.literal(text), width))
                .orElse(List.of());
        for (int i = 0; i < Math.min(detail.size(), BREEDING_DETAIL_LINES); i++) {
            graphics.text(font, detail.get(i), x, y + (2 + i) * BREEDING_STEP, MUTED, true);
        }
    }

    /**
     * @return the health, jump, speed and slots of the mount, then its overall percentage
     */
    private static List<Row> statsRows(ModConfig config, HorseStats stats) {
        List<Row> rows = new ArrayList<>();
        for (DisplayService.Stat stat : DisplayService.buildStats(config, stats, true)) {
            rows.add(new Row(
                    I18n.get(stat.labelKey()),
                    stat.rawStr(),
                    stat.percentStr(),
                    rangeText(config, stat),
                    statColor(config, stat.value(), stat.min(), stat.max())
            ));
        }
        rows.add(new Row(
                I18n.get(I18nKeys.OVERALL),
                "",
                stats.getGroupedStatsStr(config),
                "",
                statColor(config, stats.getGroupedStats(), 0, 100)
        ));
        return rows;
    }

    private static String rangeText(ModConfig config, DisplayService.Stat stat) {
        DisplayMinMax displayMinMax = config.getDisplayMinMax();
        return switch (displayMinMax) {
            case MIN_AND_MAX -> stat.minStr() + "-" + stat.maxStr();
            case MIN_ONLY -> I18n.get(I18nKeys.MIN) + " " + stat.minStr();
            case MAX_ONLY -> I18n.get(I18nKeys.MAX) + " " + stat.maxStr();
            case DISABLED -> "";
        };
    }

    private static String ownerLine(HorseStats stats) {
        return I18n.get(I18nKeys.OWNER) + ": " + stats.owner().orElse("");
    }

    /**
     * @return what the jump height and the speed are measured in
     */
    private static List<String> units() {
        return List.of(
                I18n.get(I18nKeys.JUMP_HEIGHT) + ": " + I18n.get(I18nKeys.BLOCKS),
                I18n.get(I18nKeys.SPEED) + ": " + I18n.get(I18nKeys.METERS_PER_SECONDS)
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // Compare panel
    // ------------------------------------------------------------------------------------------------------------

    /**
     * @return the height of the compare panel when it displays the biggest content, to leave room for it
     */
    public static int compareMaxHeight() {
        return RESULTS_Y + 3 * LINE_HEIGHT + 3 + CHANCE_ROWS * ROW_HEIGHT + PADDING;
    }

    public static @NotNull List<Button> createCompareButtons(
            @NotNull InventoryLayout.Rect rect,
            int originX,
            int originY,
            @NotNull Runnable addFirst,
            @NotNull Runnable addSecond,
            @NotNull Runnable run
    ) {
        int x = originX + rect.x() + PADDING;
        int y = originY + rect.y() + BUTTONS_Y;
        int width = rect.width() - 2 * PADDING;

        return List.of(
                compareButton("horsestatsmod.compare.add_first", addFirst, x, y, width),
                compareButton("horsestatsmod.compare.add_second", addSecond, x, y + BUTTON_STEP, width),
                compareButton("horsestatsmod.compare.run", run, x, y + 2 * BUTTON_STEP, width)
        );
    }

    private static Button compareButton(String translationKey, Runnable action, int x, int y, int width) {
        return Button.builder(Component.translatable(translationKey), button -> action.run())
                .bounds(x, y, width, BUTTON_HEIGHT)
                .build();
    }

    public static void drawCompare(
            @NotNull GuiGraphicsExtractor graphics,
            @NotNull Font font,
            @NotNull ModConfig config,
            @NotNull CompareState state,
            @NotNull InventoryLayout.Rect rect,
            int originX,
            int originY
    ) {
        int x = originX + rect.x();
        int y = originY + rect.y();
        int contentWidth = rect.width() - 2 * PADDING;

        List<FormattedCharSequence> message = compareMessage(font, state, contentWidth);
        drawPanel(graphics, x, y, rect.width(), compareHeight(state, message));

        drawText(graphics, font, I18n.get("horsestatsmod.compare.title"), x + PADDING, y + PADDING, TEXT);
        drawSlot(graphics, font, config, state, CompareState.FIRST, x + PADDING, y + SLOT_Y, contentWidth);
        drawSlot(graphics, font, config, state, CompareState.SECOND, x + PADDING, y + SLOT_Y + ROW_HEIGHT, contentWidth);

        int resultY = y + RESULTS_Y;
        CompareState.Outcome outcome = state.outcome().orElse(null);
        int messageColor = outcome != null && outcome.problem() != null ? ERROR : MUTED;
        for (FormattedCharSequence line : message) {
            graphics.text(font, line, x + PADDING, resultY, messageColor, true);
            resultY += LINE_HEIGHT;
        }

        if (outcome != null && outcome.odds() != null) {
            BreedingOdds.Odds odds = outcome.odds();
            resultY += 3;
            drawChance(graphics, font, config, I18n.get(I18nKeys.HEALTH), odds.health(), x + PADDING, resultY, contentWidth);
            drawChance(graphics, font, config, I18n.get(I18nKeys.JUMP_HEIGHT), odds.jump(), x + PADDING, resultY + ROW_HEIGHT, contentWidth);
            drawChance(graphics, font, config, I18n.get(I18nKeys.SPEED), odds.speed(), x + PADDING, resultY + 2 * ROW_HEIGHT, contentWidth);
            drawChance(graphics, font, config, I18n.get(I18nKeys.OVERALL), odds.overall(), x + PADDING, resultY + 3 * ROW_HEIGHT, contentWidth);
        }
    }

    private static int compareHeight(CompareState state, List<FormattedCharSequence> message) {
        boolean hasOdds = state.outcome().map(outcome -> outcome.odds() != null).orElse(false);
        return RESULTS_Y + message.size() * LINE_HEIGHT + (hasOdds ? 3 + CHANCE_ROWS * ROW_HEIGHT : 0) + PADDING;
    }

    /**
     * @return the text above the results: what to do, why there are no results, or what the results are
     */
    private static List<FormattedCharSequence> compareMessage(Font font, CompareState state, int width) {
        String text = state.outcome()
                .map(outcome -> outcome.problem() != null
                        ? I18n.get(outcome.problem().translationKey())
                        : I18n.get("horsestatsmod.compare.results"))
                .orElseGet(() -> I18n.get("horsestatsmod.compare.hint"));
        return font.split(Component.literal(text), width);
    }

    private static void drawSlot(
            GuiGraphicsExtractor graphics, Font font, ModConfig config, CompareState state, int slot, int x, int y, int width
    ) {
        String number = slot + ": ";
        drawText(graphics, font, number, x, y, MUTED);

        BreedingParent parent = state.get(slot).orElse(null);
        if (parent == null) {
            drawText(graphics, font, "-", x + font.width(number), y, MUTED);
            return;
        }

        HorseStats stats = parent.toStats();
        String percent = stats.getGroupedStatsStr(config);
        drawText(graphics, font, percent, x + width - font.width(percent), y, statColor(config, stats.getGroupedStats(), 0, 100));

        int nameWidth = width - font.width(number) - font.width(percent) - COLUMN_GAP / 2;
        drawText(graphics, font, font.plainSubstrByWidth(parent.name(), nameWidth), x + font.width(number), y, TEXT);
    }

    private static void drawChance(
            GuiGraphicsExtractor graphics, Font font, ModConfig config, String label, double chance, int x, int y, int width
    ) {
        String text = BreedingOdds.formatChance(chance);
        drawText(graphics, font, label, x, y, TEXT);
        drawText(graphics, font, text, x + width - font.width(text), y, chanceColor(config, chance));
    }

    private static int chanceColor(ModConfig config, double chance) {
        if (!config.getColoredStats()) {
            return TEXT;
        }
        if (chance >= 0.5) {
            return 0xFF77BF04;
        } else if (chance >= 0.25) {
            return 0xFFFAE314;
        } else if (chance >= 0.1) {
            return 0xFFD18800;
        }
        return 0xFFD12300;
    }

    // ------------------------------------------------------------------------------------------------------------

    private static int statColor(ModConfig config, double value, double min, double max) {
        return config.getColoredStats() ? DisplayService.getColorHex(value, min, max) : TEXT;
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND);
    }

    private static void drawText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
    }
}

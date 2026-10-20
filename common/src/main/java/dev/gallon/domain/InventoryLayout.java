package dev.gallon.domain;

/**
 * Where the stats panel and the compare panel are drawn around a mount's inventory. The positions are relative to the
 * top left corner of the inventory window.
 * <p>
 * The stats go on the right of the window and the compare panel on its left. When one of them doesn't fit on its
 * side, it goes on the other side, and the two stack when they end up on the same one. On a window too small for any
 * of that, the panels overlap the inventory instead of leaving the screen.
 */
public record InventoryLayout(Rect stats, Rect compare) {
    /**
     * Space between the inventory window and a panel
     */
    public static final int GAP = 4;

    public record Rect(int x, int y, int width, int height) {
    }

    /**
     * @param leftPos        x of the inventory window on the screen
     * @param imageWidth     width of the inventory window
     * @param screenWidth    width of the screen, in GUI units
     * @param compareHeight  the tallest the compare panel gets, used when it has to stack under the stats
     */
    public static InventoryLayout compute(
            int leftPos,
            int imageWidth,
            int screenWidth,
            int statsWidth,
            int statsHeight,
            int compareWidth,
            int compareHeight
    ) {
        int roomOnTheRight = screenWidth - (leftPos + imageWidth) - GAP;
        int roomOnTheLeft = leftPos - GAP;

        boolean statsOnTheRight = statsWidth <= roomOnTheRight || statsWidth > roomOnTheLeft;
        Rect stats = new Rect(
                clampX(sideX(statsOnTheRight, imageWidth, statsWidth), leftPos, screenWidth, statsWidth),
                0,
                statsWidth,
                statsHeight
        );

        int roomOnTheOtherSide = statsOnTheRight ? roomOnTheLeft : roomOnTheRight;
        boolean compareFitsOnTheOtherSide = compareWidth <= roomOnTheOtherSide;
        boolean compareOnTheRight = compareFitsOnTheOtherSide != statsOnTheRight;
        Rect compare = new Rect(
                clampX(sideX(compareOnTheRight, imageWidth, compareWidth), leftPos, screenWidth, compareWidth),
                compareFitsOnTheOtherSide ? 0 : statsHeight + GAP,
                compareWidth,
                compareHeight
        );

        return new InventoryLayout(stats, compare);
    }

    private static int sideX(boolean onTheRight, int imageWidth, int width) {
        return onTheRight ? imageWidth + GAP : -(width + GAP);
    }

    /**
     * Keeps a panel inside of the screen
     */
    private static int clampX(int x, int leftPos, int screenWidth, int width) {
        int maxX = screenWidth - leftPos - width;
        return Math.max(-leftPos, Math.min(maxX, x));
    }
}

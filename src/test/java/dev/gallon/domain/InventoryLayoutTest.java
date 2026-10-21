package dev.gallon.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLayoutTest {
    private static final int IMAGE_WIDTH = 176;

    @Test
    void statsGoOnTheRightAndCompareOnTheLeftWhenThereIsRoom() {
        // 1920x1080 at GUI scale 4: 480 GUI units wide
        InventoryLayout layout = InventoryLayout.compute(152, IMAGE_WIDTH, 480, 120, 90, 120, 190);

        assertEquals(IMAGE_WIDTH + InventoryLayout.GAP, layout.stats().x());
        assertEquals(-(120 + InventoryLayout.GAP), layout.compare().x());
        assertEquals(0, layout.stats().y());
        assertEquals(0, layout.compare().y());
    }

    @Test
    void panelsNeverOverlapTheInventoryWhenTheyFit() {
        // From the width where the 120 wide compare panel fits next to the window: (width - 176) / 2 - 4 >= 120
        for (int screenWidth = 424; screenWidth <= 960; screenWidth += 8) {
            int leftPos = (screenWidth - IMAGE_WIDTH) / 2;
            InventoryLayout layout = InventoryLayout.compute(leftPos, IMAGE_WIDTH, screenWidth, 120, 90, 120, 190);

            assertTrue(layout.stats().x() >= IMAGE_WIDTH, "stats on " + screenWidth);
            assertTrue(layout.compare().x() + layout.compare().width() <= 0, "compare on " + screenWidth);
        }
    }

    @Test
    void aPanelThatDoesNotFitOnItsSideGoesOnTheOtherOne() {
        // 120 units of room on the right (stats need 130) and 150 on the left
        InventoryLayout layout = InventoryLayout.compute(154, IMAGE_WIDTH, 176 + 154 + 124, 130, 90, 100, 185);

        assertEquals(-(130 + InventoryLayout.GAP), layout.stats().x());
        // The compare panel fits on the right, which is now the other side
        assertEquals(IMAGE_WIDTH + InventoryLayout.GAP, layout.compare().x());
        assertEquals(0, layout.compare().y());
    }

    @Test
    void panelsStackWhenBothEndUpOnTheSameSide() {
        // The window is too narrow for both sides: 100 units of room on each
        InventoryLayout layout = InventoryLayout.compute(104, IMAGE_WIDTH, 384, 120, 90, 120, 190);

        assertEquals(layout.stats().y() + 90 + InventoryLayout.GAP, layout.compare().y());
    }

    @Test
    void panelsStayOnTheScreenOnATinyWindow() {
        InventoryLayout layout = InventoryLayout.compute(40, IMAGE_WIDTH, 256, 120, 90, 120, 190);

        for (InventoryLayout.Rect rect : new InventoryLayout.Rect[]{layout.stats(), layout.compare()}) {
            assertTrue(40 + rect.x() >= 0, "left edge " + rect);
            assertTrue(40 + rect.x() + rect.width() <= 256, "right edge " + rect);
        }
    }
}

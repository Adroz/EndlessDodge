package com.nikmoores.android.materialmove;

import android.graphics.RectF;

import java.util.Random;

import static com.nikmoores.android.materialmove.Utilities.FAB_RADIUS;
import static com.nikmoores.android.materialmove.Utilities.MAX_ELEVATION;
import static com.nikmoores.android.materialmove.Utilities.MAX_HEIGHT;
import static com.nikmoores.android.materialmove.Utilities.MAX_WIDTH;
import static com.nikmoores.android.materialmove.Utilities.MIN_ELEVATION;
import static com.nikmoores.android.materialmove.Utilities.MIN_HEIGHT;
import static com.nikmoores.android.materialmove.Utilities.screenWidth;

/**
 * Created by Nik on 24/09/2015.
 */
public class WallPair {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final int LEFT_WALL = 0;
    public static final int RIGHT_WALL = 1;
    public static final int PERCENT_CHANCE_SAME = 80;

    private int height;
    private int width;
    private int xOffset;
    private int yOffset;
    private int elevation;
    private boolean direction;

    /**
     * Default constructor. Should only be used for initial call.
     */
    public WallPair() {
        initialiseWallPair(new Random().nextBoolean(), screenWidth / 2, -MAX_HEIGHT);
    }

    /**
     * Constructor supporting previous wall pair data input, used to place newly constructed wall
     * pair.
     *
     * @param direction Direction of previous wall pair's offset to it's previous wall pair.
     * @param leftEdge  Position of previous wall pair's left wall inner edge. Used to guide
     *                  placement of new wall pair, based on direction variable.
     * @param topEdge   Position of top edge of previous wall pair. Used to guide placement off new
     *                  wall pair's initial Y offset.
     */
    public WallPair(boolean direction, int leftEdge, int topEdge) {
        initialiseWallPair(direction, leftEdge, topEdge);
    }

    /**
     * Initialise a pair of boundary walls. Wall pair placement based on previously generated wall
     * pair.
     *
     * @param direction The direction of the previously created wall pair, as compared to its
     *                  predecessor.
     * @param leftEdge  The X coordinate of the previously created wall pair's left wall outer edge.
     * @param topEdge   The Y coordinate of the previous wall pair's top edge.
     */
    public void initialiseWallPair(boolean direction, int leftEdge, int topEdge) {
        // Set random width.
        int minWidth = FAB_RADIUS * 9;
        width = (FAB_RADIUS * 11 - minWidth) + minWidth;

        // Set xOffset direction based on input direction.
        if (new Random().nextInt(100) < PERCENT_CHANCE_SAME) {
            this.direction = direction;
        } else {
            this.direction = !direction;
        }

        // Left wall inner edge calculations.
        int offset = screenWidth / 6;
        if (direction) { // Heading left.
            xOffset = leftEdge - offset;
        } else {
            xOffset = leftEdge + offset;
        }

        // Set height.
        height = MAX_HEIGHT;

        // Set elevation.
        elevation = new Random().nextInt(MAX_ELEVATION - MIN_ELEVATION) + MIN_ELEVATION;

        yOffset = topEdge - height;
    }

    /**
     * Original randomised setup. Currently unused in favour of a more regular import.
     *
     * @param direction The direction of the previously created wall pair, as compared to its
     *                  predecessor.
     * @param leftEdge  The X coordinate of the previously created wall pair's left wall outer edge.
     * @param topEdge   The Y coordinate of the previous wall pair's top edge.
     */
    public void initialiseRandomisedWallPair(boolean direction, int leftEdge, int topEdge) {
        // Set random width.
        int minWidth = FAB_RADIUS * 8;
        width = new Random().nextInt(FAB_RADIUS * 11 - minWidth) + minWidth;

        // Set xOffset direction based on input direction.
        if (new Random().nextInt(100) < PERCENT_CHANCE_SAME) {
            this.direction = direction;
        } else {
            this.direction = !direction;
        }

        // Left wall inner edge calculations.
        int offset = new Random().nextInt(screenWidth / 6);
        if (direction) { // Heading left.
            xOffset = leftEdge - offset;
        } else {
            xOffset = leftEdge + offset;
        }
        int avgWidth = (MAX_WIDTH - minWidth) / 2 + minWidth;
        xOffset += (avgWidth - width) / 2;

        // Set height.
        height = new Random().nextInt(MAX_HEIGHT - MIN_HEIGHT + 1) + MIN_HEIGHT;

        // Set elevation.
        elevation = new Random().nextInt(MAX_ELEVATION - MIN_ELEVATION) + MIN_ELEVATION;

        yOffset = -height + topEdge;
    }

    /**
     * Updates the wall pair's current y position and x offset.
     *
     * @param dX The X coordinate change.
     * @param dY The Y coordinate change.
     */
    public void updatePosition(int dX, int dY) {
        xOffset += dX;
        yOffset += dY;
    }

    /**
     * Returns a rectangle of either the left or right wall bounding coordinates.
     *
     * @param wall An int value, representing either the left or right wall.
     * @return a RectF containing a the chosen wall's bounding coordinates.
     */
    public RectF getRect(int wall) {
        RectF scratchRect = new RectF(0, 0, 0, 0);
        if (wall == LEFT_WALL && xOffset > 0) {
            // If left wall, and is on-screen.
            scratchRect.set(0, yOffset, xOffset, yOffset + height);
        } else if (wall == RIGHT_WALL && xOffset + width < screenWidth) {
            // If right wall, and is on-screen.
            scratchRect.set(xOffset + width, yOffset, screenWidth, yOffset + height);
        }
        return scratchRect;
    }

    public int[] getLeftWallDimensions() {
        return new int[]{0, yOffset, xOffset, yOffset + height};
    }

    public int[] getRightWallDimensions() {
        return new int[]{xOffset + width, yOffset, screenWidth, yOffset + height};
    }

    public int getTop() {
        return yOffset;
    }

    public int getBottom() {
        return yOffset + height;
    }

    public int getLeftEdge() {
        return xOffset;
    }

    public int getRightEdge() {
        return xOffset + width;
    }

    public int getElevation() {
        return elevation;
    }

    /**
     * Get wall pair's X offset direction in relation to the previously built wall pair.
     *
     * @return The direction (left = true, right = false).
     */
    public boolean getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "Top= " + yOffset +
                ", Bottom= " + (yOffset + height) +
                ", LeftWallX= " + xOffset +
                ", RightWallX= " + (xOffset + width) +
                ", Elevation= " + elevation;
    }
}

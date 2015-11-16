package com.nikmoores.android.endlessdodge;

import android.graphics.RectF;

import java.util.Random;

import static com.nikmoores.android.endlessdodge.Utilities.WALL_HEIGHT;
import static com.nikmoores.android.endlessdodge.Utilities.WALL_OFFSET;
import static com.nikmoores.android.endlessdodge.Utilities.WALL_WIDTH;
import static com.nikmoores.android.endlessdodge.Utilities.screenWidth;

/**
 * Class for generating and managing wall pairs. Each generated wall pair relies of the one built
 * previously. Each wall pair is offset to the left or right of the previous with a weighted chance.
 *
 * @author Nicholas Moores (Workshop Orange)
 * @since 1.0
 */
public class WallPair {

    final String LOG_TAG = WallPair.class.getSimpleName();

    public static final int LEFT_WALL = 0;
    public static final int RIGHT_WALL = 1;
    public static final int PERCENT_CHANCE_SAME = 77;

    private int height;
    private int width;
    private int xOffset;
    private int yOffset;
    private int elevation;
    private boolean direction;

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
     * @param directionLeft The direction of the previously created wall pair, as compared to its
     *                      predecessor.
     * @param leftEdge      The X coordinate of the previously created wall pair's left wall outer edge.
     * @param topEdge       The Y coordinate of the previous wall pair's top edge.
     */
    public void initialiseWallPair(boolean directionLeft, int leftEdge, int topEdge) {
        // Set xOffset direction based on input direction.
        if (new Random().nextInt(100) < PERCENT_CHANCE_SAME) {
            this.direction = directionLeft;
        } else {
            this.direction = !directionLeft;
        }

        // Left wall inner edge calculations.
        if (directionLeft) { // Heading left.
            xOffset = leftEdge - WALL_OFFSET;
        } else {
            xOffset = leftEdge + WALL_OFFSET;
        }

        // Set height, width, top.
        height = WALL_HEIGHT;
        width = WALL_WIDTH;
        yOffset = topEdge - height;

        // Set elevation. // TODO: Currently unused, will I want this?
//        elevation = new Random().nextInt(MAX_ELEVATION - MIN_ELEVATION) + MIN_ELEVATION;
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

    /**
     * Returns the wall pair's top edge (Y axis), which due to inverse Y axis, is less than
     * the bottom edge.
     *
     * @return The top edge of the wall pair.
     */
    public int getTop() {
        return yOffset;
    }

    /**
     * Returns the wall pair's bottom edge (Y axis), which due to inverse Y axis, is greater than
     * the top edge.
     *
     * @return The bottom edge of the wall pair.
     */
    public int getBottom() {
        return yOffset + height;
    }

    /**
     * Returns the wall pair's left wall edge.
     *
     * @return The inner edge of the left wall.
     */
    public int getLeftEdge() {
        return xOffset;
    }

    /**
     * Returns the wall pair's right wall edge.
     * @return The inner edge of the right wall.
     */
    public int getRightEdge() {
        return xOffset + width;
    }

    /**
     * The wall pair's elevation (Z height).
     * @return Wall pair elevation.
     */
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

    /**
     * Sets the wall pair's X offset.
     * @param offset The new X offset.
     */
    public void setXOffset(int offset) {
        xOffset = offset;
    }

    @Override
    public String toString() {
        return "Top: " + yOffset +
                ", Bottom: " + (yOffset + height) +
                ", LeftWallX: " + xOffset +
                ", RightWallX: " + (xOffset + width) +
                ", Elevation: " + elevation;
    }
}

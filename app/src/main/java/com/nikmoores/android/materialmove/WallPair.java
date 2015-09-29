package com.nikmoores.android.materialmove;

import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.Display;
import android.view.WindowManager;

import java.util.Random;

/**
 * Created by Nik on 24/09/2015.
 */
public class WallPair {

    private static final int MAX_HEIGHT = 200;
    private static final int MIN_HEIGHT = 150;

    private static final int MAX_ELEVATION = 10;
    private static final int MIN_ELEVATION = 0;

    private int height;
    private int width;
    private int xOffset;
    private int yOffset;
    private int elevation;
    private int screenWidth;
    private int screenHeight;

    private RectF mScratchRect;


    public WallPair(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // TODO: make height based on screen size.
        height = new Random().nextInt(MAX_HEIGHT - MIN_HEIGHT + 1) + MIN_HEIGHT;
        elevation = new Random().nextInt(MAX_ELEVATION - MIN_ELEVATION + 1) + MIN_ELEVATION;
        // TODO: make width based on screen size
        width = new Random().nextInt(screenWidth / 2 - screenWidth / 3) + screenWidth / 3;
        yOffset = -height;
        xOffset = new Random().nextInt(screenWidth / 2);

        mScratchRect = new RectF(0, 0, 0, 0);
    }

    public void updatePosition(int x, int y) {
        xOffset += x;
        yOffset += y;
    }

    public RectF getRect(int wall) {
        if (wall == 0) {
            mScratchRect.set(0, yOffset, xOffset, yOffset + height);
        } else {
            mScratchRect.set(xOffset + width, yOffset, screenWidth, yOffset + height);
        }
        return mScratchRect;
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

    @Override
    public String toString() {
        return "Top= " + yOffset +
                ", Bottom= " + (yOffset + height) +
                ", LeftWallX= " + xOffset +
                ", RightWallX= " + (xOffset + width) +
                ", Elevation= " + elevation;
    }
}

package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class used to calculate globally desired values. Serves as a single location to store
 * game physics and dimension related variables, as well as definitions.
 *
 * @author Nicholas Moores (Workshop Orange).
 * @since 1.0
 */
public class Utilities {

    static final String LOG_TAG = Utilities.class.getSimpleName();

    /**
     * Debug Mode Flag: Only set to true when debugging application
     **/
    public static final boolean DEBUG_MODE = false;

    /* Intent Constants */
    public static final String INTENT_FILTER = "com.nikmoores.android.endlessdodge.INTENT_FILTER";
    public static final String STATE_KEY = "state";

    /* Colour location constants */
    public final static int COLOUR_LOCATION_LIGHT = 1;
    public final static int COLOUR_LOCATION_MAIN = 3;
    public final static int COLOUR_LOCATION_SCORE = 4;
    public final static int COLOUR_LOCATION_DARK = 5;

    /* Screen values */
    public static int screenWidth = 500;
    public static int screenHeight = 500;
    public static int WALL_HEIGHT = 200;

    /* Physics values */
    public static int PHYS_X_ACCEL_SEC = 2500;
    public static int PHYS_X_MAX_SPEED = 700;
    public static int SCROLLING_Y_SPEED = 400;

    /* FAB values */
    public static int FAB_X = 100;
    public static int FAB_Y = 100;
    public static int FAB_RADIUS = 10;

    public static int WALL_WIDTH;
    public static int WALL_OFFSET;
    public static final int NUMBER_OF_WALLS = 8;

    /**
     * Sets the game screen dimensions to a global variable. Uses the screen height to calculate
     * WallPair heights.
     *
     * @param context The application's context.
     */
    public static void setScreenDimensions(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        WALL_HEIGHT = screenHeight / NUMBER_OF_WALLS;
        if (DEBUG_MODE)
            Log.d("Utilities", "screen: " + screenWidth + "/" + screenHeight
                    + ", wall height: " + WALL_HEIGHT);
    }

    /**
     * Sets the location of the FAB (Floating Action Button) to a global variable. The X, Y location of the FAB are pixel
     * coordinates (where Y is inverted). The FAB radius is used to calculate many WallPair
     * variables and physics variables.
     *
     * @param fabX      The X axis value at the centre of the FAB, in pixels.
     * @param fabY      The Y axis value at the centre of the FAB, in pixels.
     * @param fabRadius The radius of the FAB, in pixels.
     */
    public static void setFabMeasurements(int fabX, int fabY, int fabRadius) {
        FAB_X = fabX;
        FAB_Y = fabY;
        FAB_RADIUS = fabRadius;

        // Set wall width and offset
        WALL_WIDTH = FAB_RADIUS * 12;
        WALL_OFFSET = FAB_RADIUS * 2;

        // Set physics
        SCROLLING_Y_SPEED = (int) (FAB_RADIUS * 6.5);
        //noinspection SuspiciousNameCombination
        PHYS_X_MAX_SPEED = SCROLLING_Y_SPEED;
        PHYS_X_ACCEL_SEC = PHYS_X_MAX_SPEED * 4;

        if (DEBUG_MODE) {
            Log.v(LOG_TAG, "FAB location: " + FAB_X + "," + FAB_Y + " & FAB radius = " + FAB_RADIUS);
            Log.v(LOG_TAG, "WALL WIDTH: " + WALL_WIDTH + ", WALL OFFSET: " + WALL_OFFSET);
            Log.v(LOG_TAG, "SCROLL Y: " + SCROLLING_Y_SPEED
                    + ", MAX X: " + PHYS_X_MAX_SPEED
                    + ", ACCEL X: " + PHYS_X_ACCEL_SEC);
        }
    }

    /**
     * This method converts a 2D XML integer array into a list of integer arrays. The expected input 2D
     * array must be in the form <code><integer-array name="key_X"></code>, where <code>key</code>
     * is the name of the arrays, and X represents number order of the second dimension of the
     * array.
     *
     * @param context The application's context.
     * @param key     The name of the 2D array to be converted.
     * @return The XML array converted into a List of integer arrays.
     */
    public static List<int[]> get2dResourceArray(Context context, String key) {
        List<int[]> array = new ArrayList<>();
        Resources resources = context.getResources();
        //noinspection finally
        try {
            Class<R.array> res = R.array.class;
            Field field;
            int counter = 0;
            //noinspection ConstantConditions
            do {
                field = res.getField(key + "_" + counter);
                field.getInt(field);
                int[] intArray = resources.getIntArray(field.getInt(field));
                for (int i = 0; i < intArray.length; i++) {
                    intArray[i] += 0xFF000000;
                }
                array.add(intArray);
                counter++;
            } while (field != null);
        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            //noinspection ReturnInsideFinallyBlock
            return array;
        }
    }

    /**
     * A method to find height of the status bar, in pixels.
     *
     * @param context The application context.
     * @return The status bar height, in px.
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * A method to find height of the action bar, in pixels.
     *
     * @param context The application context.
     * @return The action bar height, in px.
     */
    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    @SuppressWarnings("unused")
    public static int interpolate(double ratio, int valueTo, int valueFrom) {
        return (int) Math.abs((ratio * valueTo) + ((1 - ratio) * valueFrom));
    }

}

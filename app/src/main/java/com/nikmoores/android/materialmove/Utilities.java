package com.nikmoores.android.materialmove;

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
 * Created by Nik on 26/09/2015.
 */
public class Utilities {

    /* Intent Constants */
    public static final String INTENT_FILTER = "com.nikmoores.android.materialmove.INTENT_FILTER";
    public static final String STATE_KEY = "state";

    /* Colour location constants */
    public final static int COLOUR_LOCATION_MAIN = 3;
    public final static int COLOUR_LOCATION_DARK = 5;

    /* Screen values */
    public static int screenWidth = 500;
    public static int screenHeight = 500;
    public static int MAX_WIDTH = 200;
    public static int MIN_WIDTH = 100;
    public static int MAX_HEIGHT = 200;
    public static int MIN_HEIGHT = 100;

    /* Physics values */
    public static int PHYS_X_ACCEL_SEC = 2500;     // TODO: Will need to be calculated based on screen width.
    public static int PHYS_X_MAX_SPEED = 700;     // TODO: Will need to be calculated based on screen width.
    public static int SCROLLING_Y_SPEED = 400;      // TODO: Will need to be calculated based on screen height.

    /* FAB values */
    public static int FAB_X = 100;
    public static int FAB_Y = 100;
    public static int FAB_RADIUS = 10;

    public static final int MAX_ELEVATION = 9;
    public static final int MIN_ELEVATION = 0;

    public static void setScreenDimensions(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        MAX_WIDTH = screenWidth - screenWidth / 10;
        MIN_WIDTH = (int) Math.floor(screenWidth / 2.5);

        MAX_HEIGHT = screenHeight / 8;
        MIN_HEIGHT = screenHeight / 10;
        Log.d("Utilities", "screen: " + screenWidth + "/" + screenHeight);

        PHYS_X_MAX_SPEED = screenWidth / 2 - screenWidth / 25;
        PHYS_X_ACCEL_SEC = PHYS_X_MAX_SPEED * 4;
        //noinspection SuspiciousNameCombination
        SCROLLING_Y_SPEED = screenWidth / 2;
    }


    public static void setFabMeasurements(int fabX, int fabY, int fabRadius) {
        FAB_X = fabX;
        FAB_Y = fabY;
        FAB_RADIUS = fabRadius;
    }

    public static List<int[]> get2dResourceArray(Context context, String key) {
        List<int[]> array = new ArrayList<>();
        Resources resources = context.getResources();
        try {
            Class<R.array> res = R.array.class;
            Field field;
            int counter = 0;
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
            return array;
        }
    }

    // A method to find height of the status bar
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

}

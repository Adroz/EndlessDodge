package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.nikmoores.android.endlessdodge.Utilities.DEBUG_MODE;
import static com.nikmoores.android.endlessdodge.Utilities.FAB_RADIUS;
import static com.nikmoores.android.endlessdodge.Utilities.FAB_X;
import static com.nikmoores.android.endlessdodge.Utilities.FAB_Y;
import static com.nikmoores.android.endlessdodge.Utilities.NUMBER_OF_WALLS;
import static com.nikmoores.android.endlessdodge.Utilities.PHYS_X_ACCEL_SEC;
import static com.nikmoores.android.endlessdodge.Utilities.PHYS_X_MAX_SPEED;
import static com.nikmoores.android.endlessdodge.Utilities.SCROLLING_Y_SPEED;
import static com.nikmoores.android.endlessdodge.Utilities.WALL_HEIGHT;
import static com.nikmoores.android.endlessdodge.Utilities.WALL_WIDTH;
import static com.nikmoores.android.endlessdodge.Utilities.screenHeight;
import static com.nikmoores.android.endlessdodge.Utilities.screenWidth;
import static com.nikmoores.android.endlessdodge.WallPairComparator.ELEVATION_SORT;
import static com.nikmoores.android.endlessdodge.WallPairComparator.TOP_SORT;
import static com.nikmoores.android.endlessdodge.WallPairComparator.descending;
import static com.nikmoores.android.endlessdodge.WallPairComparator.getComparator;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameLoop extends Thread {

    final String LOG_TAG = GameLoop.class.getSimpleName();

    /*
     * Game state constants.
     */
    public static final int STATE_END = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_STARTING = 4;
    public static final int STATE_RUNNING = 5;

    /*
     * State key constants.
     */
    private static final String KEY_SCORE = "mCurrentScore";
    private static final String KEY_COLOUR_SET = "mColourSet";

    /*
     * Drawing constants
     */
    private static final int BASE_SHADOW_LENGTH = (int) (Resources.getSystem().getDisplayMetrics().density);
    private static final int START_COLOR = Color.parseColor("#33000000");
    private static final int START_COLOR_MINOR = Color.parseColor("#22000000");
    private static final int END_COLOR = Color.parseColor("#00000000");

    /**
     * Handle to the surface manager object that's interacted with.
     */
    private final SurfaceHolder mSurfaceHolder;

    /**
     * Message handler used by thread to interact with Views in Fragment.
     */
    private Handler mHandler;

    /**
     * Handler for the application's context.
     */
    private Context mContext;

    /**
     * Current height of the surface/canvas.
     *
     * @see #setSurfaceSize
     */
//    private int mCanvasHeight = 1;
//
//    /**
//     * Current width of the surface/canvas.
//     *
//     * @see #setSurfaceSize
//     */
//    private int mCanvasWidth = 1;

    /**
     * The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN
     */
    private int mMode;

    /**
     * Indicate whether the surface has been created & is ready to draw
     */
    private boolean mRun = false;

    /**
     * FAB location and radius.
     */
    private int[] mFabData = new int[3];

    /**
     * X axis change each frame.
     */
    int mX = 0;

    /**
     * Y axis change each frame.
     */
    int mY = 0;

    /**
     * X axis difference due to acceleration.
     */
    int mDX = 0;

    /**
     * Variable to calculate elapsed time between frames
     */
    private long mLastTime;

    /**
     * Scratch rectangle object.
     */
    private RectF mScratchRect;

    /**
     * Fade-in state.
     */
    private static double alphaFadeState = 0;

    /**
     * Temporary bitmap used with secondary canvas.
     */
    Bitmap tempBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    /**
     * Secondary canvas.
     */
    Canvas tempCanvas = new Canvas();

    /**
     * The game's background colour. Colour changes on each play through.
     */
    private int mColour = 0xFFFFFFFF;

    /**
     * The game's wall colour. Colour changes each play through.
     */
    private int mWallColour = 0xFFFFFFFF;

    /**
     * The current game's colour set. Colour changes on each play through.
     */
    private int[] mColourSet;

    /**
     * Gradient use to paint rectangle edge shadows.
     */
    private GradientDrawable linearGradient;

    /**
     * Gradient used to paint rectangle corner shadows.
     */
    private GradientDrawable radialGradient;

    /**
     * Paint to draw the lines on screen.
     */
    private Paint mLinePaint;

    /**
     * Current direction of rotation of screen (left or right).
     */
    private int mDirection;

    /**
     * List container for all wall pairs.
     */
    private List<WallPair> mWallPairs = new ArrayList<>();

    /**
     * Current game score.
     */
    private int mCurrentScore;

    /**
     * Running lock.
     */
    private final Object mRunLock = new Object();


    public GameLoop(SurfaceHolder holder, Context context) {
        mSurfaceHolder = holder;
        mContext = context;

        // Initialise variables.
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mScratchRect = new RectF(0, 0, 0, 0);

        int[] colors = new int[]{START_COLOR, END_COLOR};
        linearGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        radialGradient = new GradientDrawable();
        radialGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        radialGradient.setColors(colors);
        radialGradient.setGradientRadius(BASE_SHADOW_LENGTH);
    }

    @Override
    public void run() {
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null);
                synchronized (mSurfaceHolder) {
                    if (mMode == STATE_RUNNING) updatePhysics();
                    // Critical section. Do not allow mRun to be set false until we are sure all
                    // canvas draw operations are complete.
                    //
                    // If mRun has been toggled false, inhibit canvas operations.
                    synchronized (mRunLock) {
                        if (mRun)
                            doDraw(c);            // TODO: Don't draw frames if the screen is off.
                    }
                }
            } finally {
                // Do this in a finally so that if an exception is thrown during the above, the
                // SurfaceView is not left in an inconsistent state.
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }

    /**
     * Restores game state from the indicated Bundle. Typically called when
     * the Activity is being restored after having been previously
     * destroyed.
     *
     * @param savedState Bundle containing the game state
     */
    public synchronized void restoreState(Bundle savedState) {
        synchronized (mSurfaceHolder) {
            setState(STATE_PAUSE);
            mCurrentScore = savedState.getInt(KEY_SCORE);
            mColourSet = savedState.getIntArray(KEY_COLOUR_SET);
            setColour(mColourSet);
        }
    }

    /**
     * Dump game's state to a Bundle. Typically called when the Activity is being suspended.
     *
     * @return Bundle with the game's state
     */
    public Bundle saveState(Bundle bundle) {
        synchronized (mSurfaceHolder) {
            if (bundle != null) {
                bundle.putInt(KEY_SCORE, mCurrentScore);
                bundle.putIntArray(KEY_COLOUR_SET, mColourSet);
                // TODO: Save/load mWallPairs list.
            }
        }
        return bundle;
    }

    /**
     * Resets the game variables (obstacles, score, direction, speed, etc).
     */
    public void doReset() {
        synchronized (mSurfaceHolder) {
            Log.v(LOG_TAG, "doReset called.");
            mCurrentScore = 0;
            mX = 0;
            mY = 0;
            mDX = 0;
            alphaFadeState = 0;
            if (mWallPairs.size() == 0) {
                // If no walls, create wall set.
                int i;
                for (i = 0; i < NUMBER_OF_WALLS - 1; i++) {
                    int top = screenHeight - WALL_HEIGHT * i;
                    // Create wall pairs
                    mWallPairs.add(new WallPair(new Random().nextBoolean(), 0, top));
                    // Get wall that would be at FAB height.
                    int wallPairTop = mWallPairs.get(i).getTop();
                    if (wallPairTop < FAB_Y && (wallPairTop + WALL_HEIGHT) > FAB_Y) {
                        // Set that wall pair to horizontal centre.
                        mWallPairs.get(i).setXOffset((screenWidth - WALL_WIDTH) / 2);
                        for (int j = 0; j < i; j++) {
                            // Reset previously generated walls to be offset to centre pair.
                            WallPair wallPair = mWallPairs.get(i - j - 1);
                            wallPair.initialiseWallPair(
                                    mWallPairs.get(i - j).getDirection(),
                                    mWallPairs.get(i - j).getLeftEdge(),
                                    wallPair.getTop() + WALL_HEIGHT);
                        }
                        break;
                    }
                }
                // All new walls are offset according to centre wall pair.
                for (int k = i; k < NUMBER_OF_WALLS - 1; k++) {
                    mWallPairs.add(new WallPair(mWallPairs.get(k).getDirection(),
                            mWallPairs.get(k).getLeftEdge(),
                            mWallPairs.get(k).getTop()));
                }
            } else {
                int offset = 0;
                // Get pair inline with FAB, and calculate the offset needed to align to centre.
                for (WallPair wallPair : mWallPairs) {
                    if (wallPair.getTop() < FAB_Y && (wallPair.getBottom()) > FAB_Y) {
                        offset = (screenWidth - WALL_WIDTH) / 2 - wallPair.getLeftEdge();
                        break;
                    }
                }
                // Offset all wall pairs. Game is now centred and ready to start again.
                for (WallPair wallPair : mWallPairs) {
                    wallPair.updatePosition(offset, 0);
                }
            }
        }
    }

    /**
     * Starts the game, setting parameters for the current difficulty.
     */
    public void doStart() {
        doReset();
        synchronized (mSurfaceHolder) {
            mLastTime = System.currentTimeMillis() + 100;
            setState(STATE_STARTING);
        }
    }

    /**
     * Pauses the physics update & animation.
     */
    public void pause() {
        synchronized (mSurfaceHolder) {
            if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
        }
    }

    /**
     * Resumes from a pause.
     */
    public void unpause() {
        // Move the real time clock up to now
        synchronized (mSurfaceHolder) {
            mLastTime = System.currentTimeMillis() + 100;
        }
        setState(STATE_RUNNING);
    }

    /**
     * Used to signal the thread whether it should be running or not. Passing true allows the
     * thread to run; passing false will shut it down if it's already running. Calling start()
     * after this was most recently called with false will result in an immediate shutdown.
     *
     * @param running true to run, false to shut down
     */
    public void setRunning(boolean running) {
        // Do not allow mRun to be modified while any canvas operations
        // are potentially in-flight. See doDraw().
        synchronized (mRunLock) {
            mRun = running;
        }
    }

    /**
     * Sets the game mode. That is, whether we are running, paused, in the
     * failure state, in the victory state, etc.
     *
     * @param mode one of the STATE_* constants
     * @/see setState(int, CharSequence)
     */
    public void setState(int mode) {
        synchronized (mSurfaceHolder) {
            Log.v(LOG_TAG, "setState called: " + mode);
            mMode = mode;
            if (mMode == STATE_READY) doReset();
            if (mMode == STATE_END) mCurrentScore = 0;
        }
    }

    /**
     * Callback invoked when the surface dimensions change.
     */
    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
//            mCanvasWidth = width;
//            mCanvasHeight = height;
        }
    }

    /**
     * Set FAB global data (in pixels) from inputs.
     *
     * @param posX   The centre X coordinate of the FAB.
     * @param posY   The center Y coordinate of the FAB.
     * @param radius The radius of the FAB.
     */
    public void setFabData(int posX, int posY, int radius) {
        mFabData = new int[]{posX + radius, posY + radius, radius};
    }

    /**
     * Sets the colours to be used for the obstacles and background.
     *
     * @param colourSet The array of colour values ranging from light to dark.
     */
    public void setColour(int[] colourSet) {
        synchronized (mSurfaceHolder) {
            mColourSet = colourSet;
            // Set the background colour.
            mColour = colourSet[Utilities.COLOUR_LOCATION_MAIN];
            // Wall colour.
            mWallColour = colourSet[Utilities.COLOUR_LOCATION_LIGHT];
            mLinePaint.setColor(mWallColour);
        }
    }

    /**
     * The direction that ball is to start moving due to screen rotation.
     *
     * @param direction The X direction.
     */
    public void setDirection(int direction) {
        mDirection = direction;
    }

    public int getCurrentScore() {
        return mCurrentScore;
    }

    /**
     * The method for drawing each frame of the SurfaceView-GameLoop. Draws background and every
     * wall pair, along with their shadows.
     *
     * @param canvas The canvas to draw to.
     */
    private void doDraw(Canvas canvas) {
//        Log.d(LOG_TAG, "drawing frame");
        if (canvas == null) return;
        if (tempBmp.isRecycled() || tempBmp.getWidth() != canvas.getWidth() || tempBmp.getHeight() != canvas.getHeight()) {
            tempBmp.recycle();
            tempBmp = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBmp);
        }
        tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (mMode == STATE_STARTING) {
            // TODO: Change this to have the walls animating in from the sides
            // Animate wall pairs in.
            long now = System.currentTimeMillis();
            if (mLastTime > now) return;
            double elapsed = (now - mLastTime) / 500.0;

            // Animate walls in, start by changing their colour.
            alphaFadeState += elapsed * 150;
            if ((int) Math.round(alphaFadeState) >= 255) {
                setState(STATE_RUNNING);
                alphaFadeState = 255;
                mLinePaint.setAlpha(255);
                return;
            }
            mLastTime = now;
        }
        mLinePaint.setAlpha((int) Math.round(alphaFadeState));

        // Draw background.
        tempCanvas.drawColor(mColour);

        // Sort by elevation (lowest walls drawn first).
        Collections.sort(mWallPairs, getComparator(ELEVATION_SORT));

        // Draw walls.
        for (WallPair wallPair : mWallPairs) {
            mScratchRect.set(wallPair.getRect(WallPair.LEFT_WALL));
            if (!mScratchRect.isEmpty()) {
                tempCanvas.drawRect(mScratchRect, mLinePaint);
//                drawRectShadow(
//                        canvas,
//                        true,
//                        wallPair.getLeftWallDimensions(),
//                        wallPair.getElevation());
            }
            mScratchRect.set(wallPair.getRect(WallPair.RIGHT_WALL));
            if (!mScratchRect.isEmpty()) {
                tempCanvas.drawRect(mScratchRect, mLinePaint);
//                drawRectShadow(
//                        canvas,
//                        false,
//                        wallPair.getRightWallDimensions(),
//                        wallPair.getElevation());
            }
        }
        if (DEBUG_MODE) {
            mScratchRect.set(FAB_X - FAB_RADIUS, FAB_Y - FAB_RADIUS, FAB_X + FAB_RADIUS, FAB_Y + FAB_RADIUS);
            tempCanvas.drawRect(mScratchRect, mLinePaint);
        }
        canvas.drawBitmap(tempBmp, 0, 0, null);
    }

    /**
     * Calculates the obstacles' states (mX, mY) based on direction of player movement and
     * acceleration. Does not invalidate(). Called at the start of draw(). Detects collisions and
     * sets the UI to the next state.
     */
    private void updatePhysics() {
        long now = System.currentTimeMillis();
        // Do nothing if mLastTime is in the future. This allows the game-start to delay the
        // start of the physics by 100ms or so.
        if (mLastTime > now) return;
        double elapsed = (now - mLastTime) / 500.0;

        /* X axis change for walls due to screen rotation acceleration. */
        double ddx = PHYS_X_ACCEL_SEC * elapsed * mDirection;
        double dxOld = mDX;
        // Calculate speed for the end of the period
        mDX += ddx;
        // If the X speed is greater than terminal speed, then set to terminal speed.
        if (mDX > PHYS_X_MAX_SPEED) {
            mDX = PHYS_X_MAX_SPEED;
        } else if (mDX < -PHYS_X_MAX_SPEED) {
            mDX = -PHYS_X_MAX_SPEED;
        }

        // Update X and Y differences.
        mX = (int) (elapsed * (mDX + dxOld) / 2);
        mY = (int) (elapsed * SCROLLING_Y_SPEED);

        // Update timer.
        mLastTime = now;

        // Update wall positions based on X and Y updates.
        for (WallPair wallPair : mWallPairs) {
            wallPair.updatePosition(mX, mY);
        }

        // ---- COLLISION DETECTION ----
        // First check if any wall pairs are in line with the FAB.
        for (WallPair wallPair : mWallPairs) {
            if ((wallPair.getBottom() > FAB_Y - FAB_RADIUS)     // Inverse y-axis
                    && wallPair.getTop() < FAB_Y + FAB_RADIUS) {
                // Then check if either wall's inner edge is crossing the FAB
                if ((wallPair.getLeftEdge() > FAB_X - FAB_RADIUS) ||
                        wallPair.getRightEdge() < FAB_X + FAB_RADIUS) {
                    // For now, assume a hit
                    if (DEBUG_MODE) {
                        Log.d(LOG_TAG, wallPair.toString());
                        Log.d(LOG_TAG, "FAB top = " + (FAB_Y - FAB_RADIUS));
                        Log.d(LOG_TAG, "FAB left/right = " + (FAB_X - FAB_RADIUS) + "/" + (FAB_X + FAB_RADIUS));
                    }
                    // TODO: Make more accurate, after testing.
                    Intent intent = new Intent(Utilities.INTENT_FILTER);
                    intent.putExtra(Utilities.STATE_KEY, STATE_END);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                }
            }
        }

        // Sort wall pairs, so that the bottom pair is the first item.
        Collections.sort(mWallPairs, descending(getComparator(TOP_SORT)));

        // Remove wall pairs from list if they've passed through the bottom of the screen.
        while (mWallPairs.get(0).getTop() > screenHeight) {
            mWallPairs.remove(0);
            // Add to score:
            mCurrentScore += 1;
        }
        // Add new wall pairs to list if the top pair has just entered the top of the screen.
        WallPair wallPair = mWallPairs.get(mWallPairs.size() - 1);
        if (wallPair.getTop() >= -50) {
            mWallPairs.add(new WallPair(
                    wallPair.getDirection(),
                    wallPair.getLeftEdge(),
                    wallPair.getTop()));
        }
    }

    /**
     * Draws realistic shadow around imported rectangle coordinates.
     *
     * @param canvas The canvas drawn to.
     * @param dim    The edge coordinates of the rectangle (x, y, x + width, y + height).
     */
    public void drawRectShadow(Canvas canvas, boolean isLeft, int[] dim, int elevation) {
        int shadowLength = BASE_SHADOW_LENGTH * (elevation + 4);
        int shadowLengthMinor = shadowLength / 2;

        if (isLeft) {
            // Top edge
            linearGradient.setColors(new int[]{START_COLOR_MINOR, END_COLOR});
            linearGradient.setBounds(dim[0], dim[1] - shadowLengthMinor, dim[2], dim[1]);
            linearGradient.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
            linearGradient.draw(canvas);

            // Bottom edge
            linearGradient.setColors(new int[]{START_COLOR, END_COLOR});
            linearGradient.setBounds(dim[0], dim[3], dim[2], dim[3] + shadowLength);
            linearGradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            linearGradient.draw(canvas);

            // Right edge
            linearGradient.setBounds(dim[2], dim[1] + shadowLength, dim[2] + shadowLength, dim[3]);
            linearGradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            linearGradient.draw(canvas);

            radialGradient.setGradientRadius(shadowLength);

            // Bottom right corner
            radialGradient.setBounds(dim[2], dim[3], dim[2] + shadowLength, dim[3] + shadowLength);
            radialGradient.setGradientCenter(0, 0);
            radialGradient.draw(canvas);

            // Top right corner
            radialGradient.setBounds(dim[2], dim[1] - shadowLengthMinor, dim[2] + shadowLength, dim[1] + shadowLength);
            radialGradient.setGradientCenter(0, 1);
            radialGradient.draw(canvas);
        } else {
            // Bottom edge
            linearGradient.setBounds(dim[0], dim[3], dim[2], dim[3] + shadowLength);
            linearGradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            linearGradient.draw(canvas);

            radialGradient.setGradientRadius(shadowLength);

            // Bottom left corner
            radialGradient.setBounds(dim[0] - shadowLengthMinor, dim[3], dim[0], dim[3] + shadowLength);
            radialGradient.setGradientCenter(1, 0);
            radialGradient.draw(canvas);
        }
//        // Bottom edge
//        linearGradient.setBounds(
//                dim[0] + shadowLength,
//                dim[3],
//                dim[2],
//                dim[3] + shadowLength);
//        linearGradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
//        linearGradient.draw(canvas);
//
//        // Right edge
//        linearGradient.setBounds(
//                dim[2],
//                dim[1] + shadowLength,
//                dim[2] + shadowLength,
//                dim[3]);
//        linearGradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
//        linearGradient.draw(canvas);
//
//        radialGradient.setGradientRadius(shadowLength);
//
//        // Bottom left corner
//        radialGradient.setBounds(
//                dim[0],
//                dim[3],
//                dim[0] + shadowLength,
//                dim[3] + shadowLength);
//        radialGradient.setGradientCenter(1, 0);
//        radialGradient.draw(canvas);
//
//        // Bottom right corner
//        radialGradient.setBounds(
//                dim[2],
//                dim[3],
//                dim[2] + shadowLength,
//                dim[3] + shadowLength);
//        radialGradient.setGradientCenter(0, 0);
//        radialGradient.draw(canvas);
//
//        // Top right corner
//        radialGradient.setBounds(
//                dim[2],
//                dim[1],
//                dim[2] + shadowLength,
//                dim[1] + shadowLength);
//        radialGradient.setGradientCenter(0, 1);
//        radialGradient.draw(canvas);


    }
}

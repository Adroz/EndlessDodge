package com.nikmoores.android.materialmove;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.nikmoores.android.materialmove.WallPairComparator.ELEVATION_SORT;
import static com.nikmoores.android.materialmove.WallPairComparator.TOP_SORT;
import static com.nikmoores.android.materialmove.WallPairComparator.descending;
import static com.nikmoores.android.materialmove.WallPairComparator.getComparator;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameLoop extends Thread {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    /*
     * Game state constants.
     */
    public static final int STATE_END = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_RUNNING = 4;

    /*
     * Physics constants.
     */
    public static final int PHYS_X_ACCEL_SEC = 1000;     // TODO: Will need to be calculated based on screen width.
    public static final int PHYS_X_MAX_SPEED = 400;     // TODO: Will need to be calculated based on screen width.
    public static final int SCROLLING_Y_SPEED = 800;      // TODO: Will need to be calculated based on screen height.

    /*
     * State key constants.
     */
    private static final String KEY_SCORE = "mCurrentScore";
    private static final String KEY_COLOUR_SET = "mColourSet";

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
    private int mCanvasHeight = 1;

    /**
     * Current width of the surface/canvas.
     *
     * @see #setSurfaceSize
     */
    private int mCanvasWidth = 1;

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
     * The game's background colour. Colour changes on each play through.
     */
    private int mColour = 0xFFFFFFFF;

    /**
     * The current game's colour set. Colour changes on each play through.
     */
    private int[] mColourSet;

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


    public GameLoop(SurfaceHolder holder, Context context, Handler handler) {
        mSurfaceHolder = holder;
        mContext = context;
        mHandler = handler;

        // Initialise variables.
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mScratchRect = new RectF(0, 0, 0, 0);
        doReset();
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
                        if (mRun) doDraw(c);
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
            }
        }
        return bundle;
    }

    /**
     * Resets the game variables (obstacles, score, direction, speed, etc).
     */
    public void doReset() {
        synchronized (mSurfaceHolder) {
            mCurrentScore = 0;
            mX = 0;
            mY = 0;
            mDX = 0;
            mWallPairs.clear();
            mWallPairs.add(new WallPair(mContext));
        }
    }

    /**
     * Starts the game, setting parameters for the current difficulty.
     */
    public void doStart() {
        synchronized (mSurfaceHolder) {
            mLastTime = System.currentTimeMillis() + 100;
            setState(STATE_RUNNING);
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
            setState(mode, null);
        }
    }

    /**
     * Sets the game mode. That is, whether we are running, paused, in the
     * failure state, in the victory state, etc.
     *
     * @param mode    one of the STATE_* constants
     * @param message string to add to screen or null
     */
    public void setState(int mode, CharSequence message) {
        synchronized (mSurfaceHolder) {
            mMode = mode;
            Log.v(LOG_TAG, "setState called: " + mode);
            if (mMode == STATE_RUNNING) {
                // TODO: For testing, delete me.
                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.INVISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            } else {
//                mRotating = 0;
//                mEngineFiring = false;
//                Resources res = mContext.getResources();
                CharSequence str = "";
                if (mMode == STATE_READY) {
                    doReset();
                    str = "READY";
                } else if (mMode == STATE_PAUSE)
                    str = "PAUSE";
                else if (mMode == STATE_END)
                    str = "END " + "- score is: " + mCurrentScore;
                if (message != null) {
                    str = message + "\n" + str;
                }
                if (mMode == STATE_END) mCurrentScore = 0;

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", str.toString());
                b.putInt("viz", View.VISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
    }

    /**
     * Callback invoked when the surface dimensions change.
     */
    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
        }
    }

    public void setFabData(int posX, int posY, int radius) {
        mFabData = new int[]{posX, posY, radius};
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
            mLinePaint.setColor(colourSet[7]);
        }
    }

    public void setDirection(int xSpeed) {
        mDirection = xSpeed;

        // TODO: For testing, delete me.
        String str = (mDirection > 0) ? "LEFT" : "RIGHT";
        Message msg = mHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", str);
        b.putInt("viz", View.VISIBLE);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private void doDraw(Canvas canvas) {
        // Draw background.
        canvas.drawColor(mColour);

        // Sort by elevation (lowest walls drawn first).
        Collections.sort(mWallPairs, getComparator(ELEVATION_SORT));

        // Draw walls.
        for (WallPair wallPair : mWallPairs) {
            mLinePaint.setColor(mColourSet[wallPair.getElevation()]);
            canvas.drawRect(wallPair.getRect(0), mLinePaint);
            canvas.drawRect(wallPair.getRect(1), mLinePaint);
        }
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

        // TODO: Implement collision detection.
        // ---- COLLISION DETECTION ----
        // First check if any wall pairs are in line with the FAB.
//        if (mWallPairs.get(0).getBottom() > mFabData[1]) {
//            // Then check if either wall's inner edge is crossing the FAB
//            if ((mWallPairs.get(0).getLeftEdge() > mFabData[0] - mFabData[2]) ||
//                    mWallPairs.get(0).getRightEdge() < mFabData[0] + mFabData[2]) {
//                // For now, assume a hit.
//                // TODO: Make more accurate, after testing.
//                Intent intent = new Intent(Utilities.INTENT_FILTER);
//                intent.putExtra(Utilities.STATE_KEY, STATE_END);
//                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
//            }
//        }

        // Sort wall pairs, so that the bottom pair is the first item.
        Collections.sort(mWallPairs, descending(getComparator(TOP_SORT)));

        // Remove wall pairs from list if they've passed through the bottom of the screen.
        while (mWallPairs.get(0).getTop() > mCanvasHeight) {  // TODO: Might end up with issues of canvasHeight vs screenHeight...
            mWallPairs.remove(0);
        }
        // Add new wall pairs to list if the top pair has just entered the top of the screen.
        if (mWallPairs.get(mWallPairs.size() - 1).getTop() >= 0) {
            mWallPairs.add(new WallPair(mContext));
        }
    }
}

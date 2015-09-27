package com.nikmoores.android.materialmove;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameLoop extends Thread {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    /*
     * Game state constants
     */
    public static final int STATE_END = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_RUNNING = 4;

    /*
     * State key constants.
     */
    private static final String KEY_SCORE = "mCurrentScore";

    /**
     * Handle to the surface manager object that's interacted with.
     */
    private SurfaceHolder mSurfaceHolder;

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
     * Variable to calculate elapsed time between frames
     */
    private long mLastTime;

    /**
     * The game's background colour. Colour changes on each play through.
     */
    private int mColour = 0xFFFFFFFF;

    /**
     * Current game score.
     */
    private int mCurrentScore;


    private final Object mRunLock = new Object();

//    static final long FPS = 60;
//    private GameView view;
//    private boolean running = false;

    public GameLoop(SurfaceHolder holder, Context context, Handler handler) {
        mSurfaceHolder = holder;
        mHandler = handler;
        mContext = context;

        initStartingValues();
    }

    /**
     * Initialise starting values (score, obstacle placements, etc).
     */
    private void initStartingValues() {
        mCurrentScore = 0;
        bmp = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
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
        }
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
     * Dump game's state to a Bundle. Typically called when the Activity is being suspended.
     *
     * @return Bundle with the game's state
     */
    public Bundle saveState(Bundle bundle) {
        synchronized (mSurfaceHolder) {
            if (bundle != null) {
                bundle.putInt(KEY_SCORE, mCurrentScore);
            }
        }
        return bundle;
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
            if (mMode == STATE_RUNNING) {
                // TODO: For testing, delete me.
                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.INVISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            } else {
                Resources res = mContext.getResources();
                CharSequence str = "";
                if (mMode == STATE_READY)
                    str = "READY";
                else if (mMode == STATE_PAUSE)
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
                Log.v(LOG_TAG, "Game over");
            }
        }
    }

    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
        }
    }

    public void setColour(int colour) {
        synchronized (mSurfaceHolder) {
            // Set the background colour.
            mColour = colour;
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


    private void doDraw(Canvas canvas) {
        // Draw background.
        canvas.drawColor(mColour);
        canvas.drawBitmap(bmp, x, y, null);
    }

    /**
     * Calculates the obstacles' states (x, y) based on direction of player movement and
     * acceleration. Does not invalidate(). Called at the start of draw(). Detects collisions and
     * sets the UI to the next state.
     */
    private void updatePhysics() {
        long now = System.currentTimeMillis();
        // Do nothing if mLastTime is in the future. This allows the game-start to delay the
        // start of the physics by 100ms or so.
        if (mLastTime > now) return;
        // Update Obstacles.
        if ((x > mCanvasWidth - width - xSpeed || x + xSpeed < 0)) {
            xSpeed = -xSpeed;
        }
        x = x + xSpeed;
        y = y + FALLING_SPEED;

        // TODO: Implement collision detection.
        // Check for collision
        if(y> mCanvasHeight/2){
            setState(STATE_END, "Game Over.");

            Intent intent = new Intent(Utilities.INTENT_FILTER);
            intent.putExtra(Utilities.STATE_KEY, mMode);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    }

    // Testing variables
    int x = 200;
    int y = 500;
    int FALLING_SPEED = 1;
    int xSpeed = 15;
    int width = 20;
    Bitmap bmp;
}
